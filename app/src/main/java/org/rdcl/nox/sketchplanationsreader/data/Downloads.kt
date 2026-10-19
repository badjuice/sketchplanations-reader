package org.rdcl.nox.sketchplanationsreader.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.rdcl.nox.sketchplanations.explode.Images
import org.rdcl.nox.sketchplanations.explode.Rendition
import org.rdcl.nox.sketchplanations.explode.SketchImage
import org.rdcl.nox.sketchplanationsreader.BuildConfig
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext

/**
 * Files the user chose to keep: sketch drawings and podcast episodes, in the app's private storage.
 * Everything that shows a sketch asks [image] first, so a downloaded sketch shows with no network.
 */
class Downloads(context: Context, private val http: OkHttpClient) {
    private val imageDir = File(context.filesDir, "images").apply { mkdirs() }
    private val episodeDir = File(context.filesDir, "episodes").apply { mkdirs() }

    /** Every byte fetched since the process started; the download notification turns it into a speed. */
    val bytesRead = AtomicLong()

    /** Image files on disk by key, observed by the UI so a finished download shows at once. */
    private val images = mutableStateMapOf<String, File>().apply {
        imageDir.listFiles()?.filter { it.isFile && !it.name.endsWith(".part") }?.forEach { put(it.nameWithoutExtension, it) }
    }

    private fun hash(s: String) = MessageDigest.getInstance("SHA-1").digest(s.toByteArray())
        .joinToString("") { "%02x".format(it) }.take(16)

    private fun key(image: SketchImage, r: Rendition) =
        hash(image.original) + "-" + if (image.isAnimated) "anim" else r.name.lowercase()

    /**
     * What to load for [image] at [rendition]: a stored file when there is one (the exact size,
     * else the reading or HD copy, which scale down fine), otherwise the network URL.
     */
    fun image(image: SketchImage, rendition: Rendition): String {
        val f = images[key(image, rendition)] ?: images[key(image, Rendition.Read)] ?: images[key(image, Rendition.Hd)]
        return f?.let { Uri.fromFile(it).toString() } ?: Images.url(image, rendition)
    }

    fun hasImage(image: SketchImage, rendition: Rendition) = images.containsKey(key(image, rendition))

    /** Fetches the renditions not yet stored. [progress] gets 0..1 across all of them. */
    suspend fun saveImages(image: SketchImage, renditions: List<Rendition>, progress: (Float) -> Unit = {}) {
        val todo = renditions.distinctBy { key(image, it) }.filterNot { hasImage(image, it) }
        todo.forEachIndexed { i, r ->
            val k = key(image, r)
            val ext = if (image.isAnimated) "gif" else "img"
            val f = fetch(Images.url(image, r), File(imageDir, "$k.$ext")) { p -> progress((i + p) / todo.size) }
            images[k] = f
        }
        progress(1f)
    }

    fun deleteImages(image: SketchImage) {
        val prefix = hash(image.original) + "-"
        images.keys.filter { it.startsWith(prefix) }.forEach { k -> images.remove(k)?.delete() }
    }

    fun imageBytes(): Long = imageDir.listFiles()?.sumOf { it.length() } ?: 0L

    // ---- episodes -------------------------------------------------------------------------

    fun episodeFile(guid: String) = File(episodeDir, hash(guid) + ".mp3")
    fun hasEpisode(guid: String) = episodeFile(guid).exists()

    /** Resolves the feed's tracking redirect now and streams the audio to disk. */
    suspend fun saveEpisode(guid: String, audioUrl: String, progress: (Float) -> Unit) {
        fetch(audioUrl, episodeFile(guid), progress)
    }

    fun deleteEpisode(guid: String) { episodeFile(guid).delete() }

    fun episodeBytes(): Long = episodeDir.listFiles()?.sumOf { it.length() } ?: 0L

    fun deleteAllImages() { images.clear(); imageDir.listFiles()?.forEach { it.delete() } }
    fun deleteAllEpisodes() { episodeDir.listFiles()?.forEach { it.delete() } }

    /** Downloads to a `.part` file and renames it into place, so a cut-off download never looks finished. */
    private suspend fun fetch(url: String, dest: File, progress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url)
            .header("User-Agent", "SketchplanationsReader/${BuildConfig.VERSION_NAME} (+https://github.com/badjuice/sketchplanations-reader)")
            .build()
        val part = File(dest.path + ".part")
        try {
            http.newCall(req).execute().use { res ->
                if (!res.isSuccessful) throw IOException("HTTP ${res.code}")
                val body = res.body ?: throw IOException("Empty response")
                val total = body.contentLength().takeIf { it > 0 }
                var done = 0L
                body.byteStream().use { input ->
                    part.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            bytesRead.addAndGet(n.toLong())
                            if (total != null) progress((done.toFloat() / total).coerceAtMost(1f))
                        }
                    }
                }
            }
            if (!part.renameTo(dest)) { dest.delete(); if (!part.renameTo(dest)) throw IOException("Couldn't store the file") }
            dest
        } catch (e: Throwable) {
            part.delete()
            throw e
        }
    }
}

/** A small download straight into memory, e.g. an image for the gallery. */
suspend fun OkHttpClient.bytes(url: String): ByteArray = withContext(Dispatchers.IO) {
    val req = Request.Builder().url(url)
        .header("User-Agent", "SketchplanationsReader/${BuildConfig.VERSION_NAME} (+https://github.com/badjuice/sketchplanations-reader)")
        .build()
    newCall(req).execute().use { res ->
        if (!res.isSuccessful) throw IOException("HTTP ${res.code}")
        res.body?.bytes() ?: throw IOException("Empty response")
    }
}
