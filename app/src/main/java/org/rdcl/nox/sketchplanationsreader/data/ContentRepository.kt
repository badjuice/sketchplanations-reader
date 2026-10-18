package org.rdcl.nox.sketchplanationsreader.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.rdcl.nox.sketchplanations.explode.Block
import org.rdcl.nox.sketchplanations.explode.OfflineException
import org.rdcl.nox.sketchplanations.explode.Podcast
import org.rdcl.nox.sketchplanations.explode.PodcastFeed
import org.rdcl.nox.sketchplanations.explode.RichText
import org.rdcl.nox.sketchplanations.explode.Sketch
import org.rdcl.nox.sketchplanations.explode.Sketches
import org.rdcl.nox.sketchplanations.explode.SketchplanationsClient
import org.rdcl.nox.sketchplanationsreader.BuildConfig
import org.rdcl.nox.sketchplanationsreader.data.db.ContentDb
import org.rdcl.nox.sketchplanationsreader.data.db.Meta
import org.rdcl.nox.sketchplanationsreader.data.db.SketchRow
import org.rdcl.nox.sketchplanationsreader.data.db.SketchTopic
import org.rdcl.nox.sketchplanationsreader.data.db.TopicRow
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit

sealed interface SyncResult {
    /** [changed] sketches were added or updated. */
    data class Done(val changed: Int, val total: Int) : SyncResult
    data object Offline : SyncResult
    data class Failed(val reason: String) : SyncResult
}

/**
 * The app's content, stored on the device. Everything reads from local storage, so the whole app
 * works offline; [sync] is the only thing that touches the network.
 */
class ContentRepository(context: Context) {
    private val dao = ContentDb.open(context).dao()
    private val feedFile = File(context.filesDir, "podcast.xml")
    private val client = SketchplanationsClient(
        http = OkHttpClient.Builder().callTimeout(60, TimeUnit.SECONDS).build(),
        userAgent = "SketchplanationsReader/${BuildConfig.VERSION_NAME} (+https://github.com/badjuice/sketchplanations-reader)",
    )

    suspend fun sketchCount() = dao.count()
    suspend fun summaries() = dao.summaries()
    suspend fun topics() = dao.topicCounts()
    suspend fun sketchUidsForTopic(uid: String) = dao.uidsForTopic(uid)

    /** The full sketch, parsed from its stored source document. */
    suspend fun sketch(uid: String): Sketch? = dao.raw(uid)?.let { withContext(Dispatchers.Default) { Sketches.parse(it) } }

    /** Offline substring search over titles, tags, alt text and body. Newest first. */
    suspend fun search(query: String): List<String> {
        val needle = query.trim().lowercase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return if (needle.isEmpty()) emptyList() else dao.search(needle)
    }

    suspend fun podcast(): Podcast? = withContext(Dispatchers.IO) {
        if (!feedFile.exists()) null else runCatching { PodcastFeed.parse(feedFile.readBytes()) }.getOrNull()
    }

    suspend fun lastSync(): Long? = dao.meta(LAST_SYNC)?.toLongOrNull()

    /**
     * First run fetches everything (about ten requests). After that only sketches published or
     * edited since the newest edit we hold, plus the tag list and the podcast feed. Once a week it
     * also checks for sketches the site has taken down.
     */
    suspend fun sync(progress: (done: Int, total: Int) -> Unit = { _, _ -> }): SyncResult = try {
        val first = dao.count() == 0
        val topics = client.topics()
        dao.upsertTopics(topics.map { TopicRow(it.id, it.uid, it.name) })
        val names = topics.associate { it.id to it.name }

        val changed = if (first) client.allSketches(progress)
        else client.sketchesChangedSince(Instant.ofEpochMilli((dao.lastEdit() ?: 0L) - OVERLAP_MS))
        dao.store(changed.map { row(it, names) }, changed.flatMap { s -> s.tags.map { SketchTopic(s.id, it.id) } })

        val lastPrune = dao.meta(LAST_PRUNE)?.toLongOrNull() ?: 0L
        if (!first && System.currentTimeMillis() - lastPrune > WEEK_MS) {
            dao.keepOnly(client.allSketchIds())
            dao.setMeta(Meta(LAST_PRUNE, System.currentTimeMillis().toString()))
        } else if (first) {
            dao.setMeta(Meta(LAST_PRUNE, System.currentTimeMillis().toString()))
        }
        if (changed.isNotEmpty()) dao.renumber()

        // The podcast is a bonus: if its feed fails, the sketches still count as synced.
        runCatching {
            val xml = client.podcastFeed()
            PodcastFeed.parse(xml)
            withContext(Dispatchers.IO) {
                val tmp = File(feedFile.path + ".tmp")
                tmp.writeBytes(xml)
                if (!tmp.renameTo(feedFile)) { feedFile.delete(); tmp.renameTo(feedFile) }
            }
        }

        dao.setMeta(Meta(LAST_SYNC, System.currentTimeMillis().toString()))
        SyncResult.Done(changed.size, dao.count())
    } catch (e: OfflineException) {
        SyncResult.Offline
    } catch (e: IOException) {
        SyncResult.Failed(e.message ?: "Couldn't reach the site")
    }

    private fun row(s: Sketch, topicNames: Map<String, String>): SketchRow {
        val text = s.body.joinToString("\n") { plain(it) }
        return SketchRow(
            id = s.id, uid = s.uid, title = s.title,
            sortKey = s.title.replace(Regex("^(The|A|An)\\s+", RegexOption.IGNORE_CASE), ""),
            publishedAt = s.publishedAt.toEpochMilli(), lastPublishedAt = s.lastPublishedAt.toEpochMilli(),
            ordinal = 0,
            imageUrl = s.image.url, imageAlt = s.image.alt, imageWidth = s.image.width, imageHeight = s.image.height,
            lead = s.standfirst ?: lead(s.body),
            searchText = listOf(s.title, s.tags.joinToString(" ") { topicNames[it.id] ?: it.uid }, s.image.alt.orEmpty(), text)
                .joinToString("\n").lowercase(),
            podcastUrl = s.podcastUrl,
            raw = s.raw,
        )
    }

    private fun plain(b: Block): String = when (b) {
        is Block.Paragraph -> b.content.text
        is Block.Heading -> b.content.text
        is Block.Preformatted -> b.content.text
        is Block.ListBlock -> b.items.joinToString("\n", transform = RichText::text)
        is Block.Image -> b.image.alt.orEmpty()
        is Block.Unknown -> b.text
    }

    /** The opening of the first paragraph, cut at a word near 160 characters. */
    private fun lead(body: List<Block>): String {
        val t = body.firstNotNullOfOrNull { (it as? Block.Paragraph)?.content?.text?.trim()?.takeIf(String::isNotEmpty) } ?: return ""
        if (t.length <= 160) return t
        val cut = t.lastIndexOf(' ', 160).takeIf { it > 80 } ?: 160
        return t.substring(0, cut).trimEnd(',', ';', ':', ' ') + "…"
    }

    private companion object {
        const val LAST_SYNC = "lastSync"
        const val LAST_PRUNE = "lastPrune"
        /** Refetch a minute of overlap, so an edit landing during the last sync isn't missed. */
        const val OVERLAP_MS = 60_000L
        const val WEEK_MS = 7 * 24 * 3600_000L
    }
}
