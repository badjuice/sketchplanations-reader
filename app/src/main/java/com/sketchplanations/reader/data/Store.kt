package com.sketchplanations.reader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sketchplanations.reader.ui.theme.FontPack
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

enum class LinkStyle { Badge, Underline }
enum class HomeLayout { Deck, Feed }

/** Every persisted preference. Defaults match the design prototype. */
data class AppSettings(
    val dark: Boolean? = null,
    val bigText: Boolean = false,
    val reduceMotion: Boolean = false,
    val homeLayout: HomeLayout = HomeLayout.Deck,
    val extBrowser: Boolean = false,
    val linkStyle: LinkStyle = LinkStyle.Badge,
    val railLetters: Boolean = true,
    val railYears: Boolean = true,
    val railAll: Boolean = false,
    val showRecents: Boolean = true,
    val autoDl: Boolean = true,
    val wifiOnly: Boolean = true,
    val hd: Boolean = true,
    val imgWifiOnly: Boolean = true,
    val speed: Float = 1f,
    val skipBack: Int = 15,
    val skipFwd: Int = 30,
    val sleep: Int = 0,
    val continuous: Boolean = false,
    val autoDelete: Boolean = true,
    val streamHq: Boolean = false,
    val bgPlay: Boolean = true,
    val notifPlayer: Boolean = true,
    val pauseOnCall: Boolean = true,
    val stopOnLeave: Boolean = false,
    val notifySketch: Boolean = true,
    val notifyEp: Boolean = false,
    val font: FontPack = FontPack.Original,
    /** A SAF tree URI chosen in Settings › Offline, or null for the app's own storage. */
    val downloadFolder: String? = null,
)

/** The three separate save lists for sketches, their podcast equivalents, and first-run state. */
data class Library(
    val favourites: List<Int> = listOf(571, 510, 402),
    val offline: List<Int> = listOf(589, 578, 487, 350),
    val gallery: List<Int> = listOf(402),
    val epFav: List<Int> = listOf(47),
    val epOffline: List<Int> = listOf(48),
    val recents: List<String> = listOf("Thinking", "bias", "razor", "work"),
    val onboarded: Boolean = false,
    val indexBuilt: Boolean = false,
    val lastSync: Long = 0L,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reader")

/**
 * Persists settings and library lists as two JSON blobs. Small, schema-free and enough for
 * a few hundred ids; the catalogue and cached post bodies belong in Room once the Prismic
 * client lands.
 */
class Store(private val context: Context) {
    private val settingsKey = stringPreferencesKey("settings")
    private val libraryKey = stringPreferencesKey("library")

    suspend fun load(): Pair<AppSettings, Library> {
        val p = context.dataStore.data.first()
        val s = p[settingsKey]?.let { runCatching { decodeSettings(JSONObject(it)) }.getOrNull() } ?: AppSettings()
        val l = p[libraryKey]?.let { runCatching { decodeLibrary(JSONObject(it)) }.getOrNull() } ?: Library()
        return s to l
    }

    suspend fun save(settings: AppSettings) {
        context.dataStore.edit { it[settingsKey] = encode(settings).toString() }
    }

    suspend fun save(library: Library) {
        context.dataStore.edit { it[libraryKey] = encode(library).toString() }
    }

    private fun encode(s: AppSettings) = JSONObject().apply {
        s.dark?.let { put("dark", it) }
        put("bigText", s.bigText); put("reduceMotion", s.reduceMotion); put("homeLayout", s.homeLayout.name)
        put("extBrowser", s.extBrowser); put("linkStyle", s.linkStyle.name)
        put("railLetters", s.railLetters); put("railYears", s.railYears); put("railAll", s.railAll)
        put("showRecents", s.showRecents); put("autoDl", s.autoDl); put("wifiOnly", s.wifiOnly); put("hd", s.hd)
        put("imgWifiOnly", s.imgWifiOnly); put("speed", s.speed.toDouble()); put("skipBack", s.skipBack)
        put("skipFwd", s.skipFwd); put("sleep", s.sleep); put("continuous", s.continuous); put("autoDelete", s.autoDelete)
        put("streamHq", s.streamHq); put("bgPlay", s.bgPlay); put("notifPlayer", s.notifPlayer)
        put("pauseOnCall", s.pauseOnCall); put("stopOnLeave", s.stopOnLeave); put("notifySketch", s.notifySketch)
        put("notifyEp", s.notifyEp); put("font", s.font.name)
        s.downloadFolder?.let { put("downloadFolder", it) }
    }

    private fun decodeSettings(o: JSONObject): AppSettings {
        val d = AppSettings()
        fun b(k: String, def: Boolean) = if (o.has(k)) o.getBoolean(k) else def
        fun i(k: String, def: Int) = if (o.has(k)) o.getInt(k) else def
        return AppSettings(
            dark = if (o.has("dark")) o.getBoolean("dark") else null,
            bigText = b("bigText", d.bigText), reduceMotion = b("reduceMotion", d.reduceMotion),
            homeLayout = runCatching { HomeLayout.valueOf(o.getString("homeLayout")) }.getOrDefault(d.homeLayout),
            extBrowser = b("extBrowser", d.extBrowser),
            linkStyle = runCatching { LinkStyle.valueOf(o.getString("linkStyle")) }.getOrDefault(d.linkStyle),
            railLetters = b("railLetters", d.railLetters), railYears = b("railYears", d.railYears), railAll = b("railAll", d.railAll),
            showRecents = b("showRecents", d.showRecents), autoDl = b("autoDl", d.autoDl), wifiOnly = b("wifiOnly", d.wifiOnly),
            hd = b("hd", d.hd), imgWifiOnly = b("imgWifiOnly", d.imgWifiOnly),
            speed = if (o.has("speed")) o.getDouble("speed").toFloat() else d.speed,
            skipBack = i("skipBack", d.skipBack), skipFwd = i("skipFwd", d.skipFwd), sleep = i("sleep", d.sleep),
            continuous = b("continuous", d.continuous), autoDelete = b("autoDelete", d.autoDelete), streamHq = b("streamHq", d.streamHq),
            bgPlay = b("bgPlay", d.bgPlay), notifPlayer = b("notifPlayer", d.notifPlayer), pauseOnCall = b("pauseOnCall", d.pauseOnCall),
            stopOnLeave = b("stopOnLeave", d.stopOnLeave), notifySketch = b("notifySketch", d.notifySketch), notifyEp = b("notifyEp", d.notifyEp),
            font = runCatching { FontPack.valueOf(o.getString("font")) }.getOrDefault(d.font),
            downloadFolder = if (o.has("downloadFolder")) o.getString("downloadFolder") else null,
        )
    }

    private fun ints(a: List<Int>) = JSONArray().apply { a.forEach { put(it) } }
    private fun JSONObject.ints(k: String, def: List<Int>) =
        optJSONArray(k)?.let { a -> List(a.length()) { a.getInt(it) } } ?: def

    private fun encode(l: Library) = JSONObject().apply {
        put("favourites", ints(l.favourites)); put("offline", ints(l.offline)); put("gallery", ints(l.gallery))
        put("epFav", ints(l.epFav)); put("epOffline", ints(l.epOffline))
        put("recents", JSONArray().apply { l.recents.forEach { put(it) } })
        put("onboarded", l.onboarded); put("indexBuilt", l.indexBuilt); put("lastSync", l.lastSync)
    }

    private fun decodeLibrary(o: JSONObject): Library {
        val d = Library()
        return Library(
            favourites = o.ints("favourites", d.favourites), offline = o.ints("offline", d.offline),
            gallery = o.ints("gallery", d.gallery), epFav = o.ints("epFav", d.epFav), epOffline = o.ints("epOffline", d.epOffline),
            recents = o.optJSONArray("recents")?.let { a -> List(a.length()) { a.getString(it) } } ?: d.recents,
            onboarded = o.optBoolean("onboarded", false), indexBuilt = o.optBoolean("indexBuilt", false),
            lastSync = o.optLong("lastSync", 0L),
        )
    }
}
