package org.rdcl.nox.sketchplanationsreader.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert

/**
 * One row per sketch. List screens read the light columns; the reading view parses [raw] (the
 * source document) with SketchplanationsExplode, so parser fixes reach stored data without a refetch.
 */
@Entity(tableName = "sketch", indices = [Index("uid", unique = true), Index("publishedAt")])
data class SketchRow(
    @PrimaryKey val id: String,
    val uid: String,
    val title: String,
    /** Leading articles dropped, for A–Z: The Diderot Effect files under D. */
    val sortKey: String,
    val publishedAt: Long,
    val lastPublishedAt: Long,
    /** Position in publication order, #1 first. The site stores no number of its own. */
    val ordinal: Int,
    val imageUrl: String,
    val imageAlt: String?,
    val imageWidth: Int?,
    val imageHeight: Int?,
    /** A short line for cards: the standfirst, or the opening of the first paragraph. */
    val lead: String,
    /** Lower-cased title, tags, alt text and body, for offline search. */
    val searchText: String,
    val podcastUrl: String?,
    val raw: String,
)

/** The light columns, for lists. */
data class SketchSummary(
    val id: String,
    val uid: String,
    val title: String,
    val sortKey: String,
    val publishedAt: Long,
    val ordinal: Int,
    val imageUrl: String,
    val imageAlt: String?,
    val lead: String,
)

@Entity(tableName = "topic", indices = [Index("uid", unique = true)])
data class TopicRow(@PrimaryKey val id: String, val uid: String, val name: String)

@Entity(tableName = "sketch_topic", primaryKeys = ["sketchId", "topicId"], indices = [Index("topicId")])
data class SketchTopic(val sketchId: String, val topicId: String)

data class TopicCount(val id: String, val uid: String, val name: String, val count: Int)

@Entity(tableName = "meta")
data class Meta(@PrimaryKey val key: String, val value: String)

private const val SUMMARY = "id, uid, title, sortKey, publishedAt, ordinal, imageUrl, imageAlt, lead"

@Dao
interface ContentDao {
    @Query("SELECT $SUMMARY FROM sketch ORDER BY publishedAt DESC")
    suspend fun summaries(): List<SketchSummary>

    @Query("SELECT raw FROM sketch WHERE uid = :uid")
    suspend fun raw(uid: String): String?

    @Query("SELECT COUNT(*) FROM sketch")
    suspend fun count(): Int

    @Query("SELECT MAX(lastPublishedAt) FROM sketch")
    suspend fun lastEdit(): Long?

    @Query("SELECT id, publishedAt FROM sketch")
    suspend fun order(): List<Order>

    @Query("UPDATE sketch SET ordinal = :ordinal WHERE id = :id")
    suspend fun setOrdinal(id: String, ordinal: Int)

    @Query("SELECT uid FROM sketch WHERE searchText LIKE '%' || :needle || '%' ESCAPE '\\' ORDER BY publishedAt DESC")
    suspend fun search(needle: String): List<String>

    @Query(
        "SELECT t.id, t.uid, t.name, COUNT(st.sketchId) AS count FROM topic t " +
            "LEFT JOIN sketch_topic st ON st.topicId = t.id GROUP BY t.id ORDER BY count DESC, t.name",
    )
    suspend fun topicCounts(): List<TopicCount>

    @Query("SELECT s.uid FROM sketch s JOIN sketch_topic st ON st.sketchId = s.id JOIN topic t ON t.id = st.topicId WHERE t.uid = :topicUid ORDER BY s.publishedAt DESC")
    suspend fun uidsForTopic(topicUid: String): List<String>

    @Query("SELECT t.uid FROM topic t JOIN sketch_topic st ON st.topicId = t.id WHERE st.sketchId = :sketchId")
    suspend fun topicUidsFor(sketchId: String): List<String>

    @Upsert suspend fun upsertSketches(rows: List<SketchRow>)
    @Upsert suspend fun upsertTopics(rows: List<TopicRow>)
    @Upsert suspend fun upsertSketchTopics(rows: List<SketchTopic>)

    @Query("DELETE FROM sketch_topic WHERE sketchId IN (:ids)")
    suspend fun clearTopicsOf(ids: List<String>)

    @Query("SELECT id FROM sketch")
    suspend fun ids(): List<String>

    @Query("DELETE FROM sketch WHERE id IN (:ids)")
    suspend fun deleteSketches(ids: List<String>)

    @Query("DELETE FROM sketch_topic WHERE sketchId NOT IN (SELECT id FROM sketch)")
    suspend fun deleteOrphanLinks()

    @Query("SELECT value FROM meta WHERE `key` = :key")
    suspend fun meta(key: String): String?

    @Upsert suspend fun setMeta(m: Meta)

    /** Writes sketches and their topic links together, replacing any links they had. */
    @Transaction
    suspend fun store(rows: List<SketchRow>, links: List<SketchTopic>) {
        if (rows.isEmpty()) return
        upsertSketches(rows)
        rows.map { it.id }.chunked(500).forEach { clearTopicsOf(it) }
        upsertSketchTopics(links)
    }

    /** Removes sketches the site no longer publishes. Chunked to stay under SQLite's variable limit. */
    @Transaction
    suspend fun keepOnly(published: Set<String>) {
        ids().filter { it !in published }.chunked(500).forEach { deleteSketches(it) }
        deleteOrphanLinks()
    }

    @Transaction
    suspend fun renumber() {
        order().sortedWith(compareBy<Order> { it.publishedAt }.thenBy { it.id })
            .forEachIndexed { i, o -> setOrdinal(o.id, i + 1) }
    }
}

data class Order(val id: String, val publishedAt: Long)

@Database(entities = [SketchRow::class, TopicRow::class, SketchTopic::class, Meta::class], version = 1, exportSchema = false)
abstract class ContentDb : RoomDatabase() {
    abstract fun dao(): ContentDao

    companion object {
        fun open(context: Context): ContentDb =
            Room.databaseBuilder(context, ContentDb::class.java, "content.db")
                // The database is a cache of the site: if its shape ever changes, rebuild it from a sync.
                .fallbackToDestructiveMigration()
                .build()
    }
}
