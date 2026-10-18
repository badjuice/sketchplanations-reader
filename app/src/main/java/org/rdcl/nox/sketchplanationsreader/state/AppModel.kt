package org.rdcl.nox.sketchplanationsreader.state

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import org.rdcl.nox.sketchplanationsreader.data.AppSettings
import org.rdcl.nox.sketchplanationsreader.data.Catalogue
import org.rdcl.nox.sketchplanationsreader.data.Conn
import org.rdcl.nox.sketchplanationsreader.data.ContentRepository
import org.rdcl.nox.sketchplanationsreader.data.SyncResult
import org.rdcl.nox.sketchplanationsreader.data.Library
import org.rdcl.nox.sketchplanationsreader.data.Sketch
import org.rdcl.nox.sketchplanationsreader.data.SketchLink
import org.rdcl.nox.sketchplanationsreader.data.Store
import org.rdcl.nox.sketchplanationsreader.data.connectivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class Screen(val title: String, val top: Boolean) {
    Onboarding("Welcome", false),
    Today("Sketchplanations", true),
    Detail("", false),
    Archive("Archive", true),
    Search("Search", true),
    Categories("Topics", true),
    Library("Library", true),
    Podcast("The podcast", true),
    PodLibrary("Podcast library", true),
    Prints("Support the author", true),
    About("About this app", true),
    Settings("Settings", true),
    Episode("Episode", false),
    Browse("Browse", false),
    Tag("", false),
    PodcastAbout("About the podcast", false),
    PodcastContact("Contact the show", false),
}

/** A history entry. Sketch → linked sketch → episode all share screen names, so each frame snapshots its subject too. */
data class Frame(val screen: Screen, val detail: Int, val ep: Int, val tag: String, val acc: String)

data class Confirm(val title: String, val body: String, val cta: String, val act: () -> Unit)

enum class SyncState { Idle, Running, Done, Failed }
enum class Sort { Az, Date }
enum class Filter { All, Offline, Favourites }
enum class SearchPhase { Idle, Typing, Searching, Done }

sealed interface Effect {
    data class OpenUrl(val url: String, val inApp: Boolean) : Effect
    data class Share(val text: String) : Effect
    data class Email(val to: String, val subject: String, val body: String) : Effect
}

class AppModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    val repo = ContentRepository(app)

    var loaded by mutableStateOf(false); private set
    var settings by mutableStateOf(AppSettings()); private set
    var library by mutableStateOf(Library()); private set
    var conn by mutableStateOf(Conn.Wifi); private set

    // Navigation
    var screen by mutableStateOf(Screen.Today); private set
    var stack by mutableStateOf(emptyList<Frame>()); private set
    var detail by mutableIntStateOf(589); private set
    var ep by mutableIntStateOf(0); private set
    var tag by mutableStateOf("Thinking"); private set
    var acc by mutableStateOf("what")
    var drawer by mutableStateOf(false)
    /** Bumped when a re-tapped tab asks the current list to scroll home. */
    var scrollTopTick by mutableIntStateOf(0); private set

    // Onboarding + catalogue sync
    var ob by mutableIntStateOf(0)
    var syncState by mutableStateOf(SyncState.Idle); private set
    var syncN by mutableIntStateOf(0); private set

    // Per-view modes, reset on entry
    var sort by mutableStateOf(Sort.Date)
    var asc by mutableStateOf(false)
    var filter by mutableStateOf(Filter.All)
    var catByFreq by mutableStateOf(true)
    var libFavTab by mutableStateOf(true)
    var podFavTab by mutableStateOf(true)
    var deckIdx by mutableIntStateOf(0)
    var browseIdx by mutableIntStateOf(0)

    // Search
    var q by mutableStateOf(""); private set
    var phase by mutableStateOf(SearchPhase.Idle); private set
    var searchMsg by mutableStateOf(Catalogue.searchMessages[0]); private set

    // Downloads — progress lives inside the button that started it
    var dl by mutableStateOf<Int?>(null); private set
    var pct by mutableIntStateOf(0); private set
    var epDl by mutableStateOf<Int?>(null); private set
    var epPct by mutableIntStateOf(0); private set
    var bulkSketches by mutableStateOf<Int?>(null); private set
    var bulkEpisodes by mutableStateOf<Int?>(null); private set
    var syncPct by mutableStateOf<Int?>(null); private set
    /** Sketches the catalogue fetch expects, as the site reports it. */
    var syncTotal by mutableIntStateOf(0); private set
    /** Bumped whenever stored content changes, so screens reload. */
    var contentVersion by mutableIntStateOf(0); private set
    var pulse by mutableStateOf<Pair<String, String>?>(null); private set
    /** "Load image" reveals for this session only — never the download cache. */
    var revealed by mutableStateOf(emptySet<Int>()); private set

    // Player
    var playing by mutableStateOf(false); private set
    var audioEp by mutableIntStateOf(0); private set
    var pos by mutableFloatStateOf(0.32f); private set
    var sleepEndsAt by mutableStateOf<Long?>(null); private set

    // Transient UI
    var zoom by mutableStateOf(false)
    var toast by mutableStateOf<String?>(null); private set
    var confirm by mutableStateOf<Confirm?>(null)
    var egg by mutableStateOf(false)
    private var verTaps = 0

    private val effectChannel = Channel<Effect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    private var toastJob: Job? = null
    private var pulseJob: Job? = null
    private var searchJob: Job? = null
    private var dlJob: Job? = null
    private var epDlJob: Job? = null
    private var playJob: Job? = null
    private var syncJob: Job? = null
    private var bulkJob: Job? = null

    init {
        viewModelScope.launch {
            val (s, l) = store.load()
            settings = s
            library = l
            if (!l.onboarded) screen = Screen.Onboarding
            loaded = true
            launch { snapshotFlow { settings }.drop(1).collect { store.save(it) } }
            launch { snapshotFlow { library }.drop(1).collect { store.save(it) } }
            // The incremental catalogue check also runs on launch when online.
            library = library.copy(indexBuilt = repo.sketchCount() > 0)
            delay(1500)
            if (online && library.indexBuilt) syncWithWebsite(quiet = true)
        }
        viewModelScope.launch {
            var first = true
            connectivity(app).collect { c ->
                val was = conn
                conn = c
                if (first) { first = false; return@collect }
                when {
                    was == Conn.Off && c != Conn.Off ->
                        flash(if (screen == Screen.Detail || screen == Screen.Browse) "Back online — tap to load this sketch" else "Back online")
                    c == Conn.Off -> flash("Offline. Downloaded sketches still open.")
                    else -> flash(if (c == Conn.Wifi) "On Wi-Fi" else "On mobile data")
                }
            }
        }
    }

    // ---- helpers -------------------------------------------------------------------------

    val online get() = conn != Conn.Off
    val sketch: Sketch get() = Catalogue.sketch(detail) ?: Catalogue.sketches[0]
    val episode get() = Catalogue.episodes[ep]

    fun updateSettings(f: AppSettings.() -> AppSettings) { settings = settings.f() }
    private fun updateLibrary(f: Library.() -> Library) { library = library.f() }

    fun flash(m: String) {
        toastJob?.cancel()
        toast = m
        toastJob = viewModelScope.launch { delay(1900); toast = null }
    }

    /** Confirmation belongs in the control that was pressed, not in a bar over it. */
    private fun pulse(k: String, m: String) {
        pulseJob?.cancel()
        pulse = k to m
        pulseJob = viewModelScope.launch { delay(1700); pulse = null }
    }

    fun pulseFor(k: String) = pulse?.takeIf { it.first == k }?.second

    fun isOffline(n: Int) = n in library.offline
    fun isFavourite(n: Int) = n in library.favourites
    fun inGallery(n: Int) = n in library.gallery

    /** Offline and not downloaded: nothing on the device to show. */
    fun needsNet(n: Int) = !isOffline(n) && !online
    /** On mobile data with Images on Wi-Fi only: the image waits behind Load image. */
    fun imageHeld(n: Int) = !isOffline(n) && conn == Conn.Mobile && settings.imgWifiOnly && n !in revealed

    // ---- navigation ----------------------------------------------------------------------

    private fun frame() = Frame(screen, detail, ep, tag, acc)

    /** Every view resets to its default on entry — no screen remembers a filter set three taps ago. */
    private fun applyDefaults(s: Screen) {
        when (s) {
            Screen.Categories -> catByFreq = true
            Screen.Archive -> { filter = Filter.All; sort = Sort.Date; asc = false }
            Screen.Library -> libFavTab = true
            Screen.PodLibrary -> podFavTab = true
            Screen.Search -> { searchJob?.cancel(); q = ""; phase = SearchPhase.Idle }
            Screen.About -> acc = "what"
            else -> {}
        }
    }

    private fun moveTo(s: Screen, push: Boolean) {
        searchJob?.cancel()
        val leaving = screen
        stack = when {
            screen == Screen.Onboarding -> emptyList()
            push -> stack + frame()
            else -> emptyList()
        }
        screen = s
        drawer = false
        zoom = false
        if (leaving == Screen.Episode && s != Screen.Episode && playing && settings.stopOnLeave) pause()
    }

    /** Top-level destinations swap (the bar keeps its hamburger); anything deeper pushes. */
    fun go(s: Screen) {
        moveTo(s, push = !s.top)
        applyDefaults(s)
    }

    fun goDeep(s: Screen, extra: () -> Unit = {}) {
        moveTo(s, push = true)
        applyDefaults(s)
        extra()
    }

    fun open(n: Int) {
        moveTo(Screen.Detail, push = true)
        detail = n
    }

    fun openEp(i: Int) {
        moveTo(Screen.Episode, push = true)
        ep = i.coerceIn(0, Catalogue.episodes.lastIndex)
    }

    fun openTag(t: String) {
        moveTo(Screen.Tag, push = true)
        tag = t
    }

    fun openAboutHosts() = goDeep(Screen.About) { acc = "hosts" }

    fun canGoBack() = stack.isNotEmpty() || (screen != Screen.Today && screen != Screen.Onboarding)

    /** Pops one level, to exactly the previous item — not just the previous screen type. */
    fun back() {
        zoom = false
        val e = stack.lastOrNull()
        if (e == null) { go(Screen.Today); return }
        val leaving = screen
        stack = stack.dropLast(1)
        screen = e.screen; detail = e.detail; ep = e.ep; tag = e.tag; acc = e.acc
        if (leaving == Screen.Episode && screen != Screen.Episode && playing && settings.stopOnLeave) pause()
    }

    /** Re-tapping the active tab pops it to its root, or scrolls to the top if already there. */
    fun tab(s: Screen) {
        if (screen == s && stack.isEmpty()) scrollTopTick++ else go(s)
    }

    fun shuffle() {
        val s = Catalogue.sketches.random()
        open(s.n)
        flash("A random one: ${s.title}")
    }

    /** Inline links to other sketches and episodes open in the app; everything else leaves it. */
    fun follow(link: SketchLink?, label: String) {
        when {
            link == null -> flash("Could not open $label")
            link.sketchRef != null -> {
                val n = link.sketchRef!!
                if (Catalogue.sketch(n) != null) open(n) else flash("Could not open $label")
            }
            link.episodeRef != null -> openEp(Catalogue.episodeIndex(link.episodeRef!!).coerceAtLeast(0))
            else -> openUrl(link.href)
        }
    }

    fun openUrl(url: String) {
        effectChannel.trySend(Effect.OpenUrl(url, inApp = !settings.extBrowser))
    }

    fun ask(c: Confirm) { confirm = c }

    // ---- onboarding / catalogue sync -----------------------------------------------------

    fun nextOnboarding() {
        if (ob < 3) {
            if (ob == 2) startSync()
            ob++
            return
        }
        when (syncState) {
            SyncState.Running -> {}
            SyncState.Failed, SyncState.Idle -> startSync()
            SyncState.Done -> finishOnboarding()
        }
    }

    private fun finishOnboarding() {
        updateLibrary { copy(onboarded = true) }
        screen = Screen.Today
        stack = emptyList()
    }

    fun askSkipIndex() = ask(Confirm(
        "Skip building the index?",
        "Archive, Topics and A–Z stay empty until it runs. Today, Browse and Search still work while you are online, and Archive will offer to build it again.",
        "Skip it",
    ) {
        syncJob?.cancel()
        syncState = SyncState.Failed; syncN = 0
        finishOnboarding()
        flash("Index skipped — Archive will offer it again")
    })

    /** First-run catalogue fetch: every sketch with its words, about ten requests. */
    fun startSync() {
        syncJob?.cancel()
        if (!online) { syncState = SyncState.Failed; syncN = 0; return }
        syncState = SyncState.Running; syncN = 0
        syncJob = viewModelScope.launch {
            when (repo.sync { done, total -> syncN = done; syncTotal = total }) {
                is SyncResult.Done -> {
                    syncState = SyncState.Done
                    syncN = repo.sketchCount(); syncTotal = syncN
                    updateLibrary { copy(indexBuilt = true, lastSync = System.currentTimeMillis()) }
                    contentVersion++
                }
                SyncResult.Offline -> { syncState = SyncState.Failed; syncN = 0 }
                is SyncResult.Failed -> { syncState = SyncState.Failed; syncN = 0 }
            }
        }
    }

    /**
     * Settings › Storage › Sync with website, and the check on launch: fetches only what's new or
     * edited since last time. [quiet] skips the messages when nothing changed.
     */
    fun syncWithWebsite(quiet: Boolean = false) {
        if (!online) { if (!quiet) flash("Offline — connect to check for new sketches"); return }
        if (syncPct != null || syncState == SyncState.Running) return
        syncPct = 0
        viewModelScope.launch {
            val result = repo.sync { done, total -> if (total > 0) syncPct = (done * 100 / total).coerceIn(0, 99) }
            syncPct = null
            when (result) {
                is SyncResult.Done -> {
                    updateLibrary { copy(indexBuilt = true, lastSync = System.currentTimeMillis()) }
                    if (syncState != SyncState.Running) syncState = SyncState.Done
                    contentVersion++
                    if (!quiet) pulse("sync", if (result.changed == 0) "Up to date" else "${result.changed} updated")
                    else if (result.changed > 0) flash(if (result.changed == 1) "1 sketch new or updated" else "${result.changed} sketches new or updated")
                }
                SyncResult.Offline -> if (!quiet) flash("Offline — connect to check for new sketches")
                is SyncResult.Failed -> if (!quiet) flash("Couldn't reach the site — try again later")
            }
        }
    }

    // ---- search --------------------------------------------------------------------------

    /**
     * Debounced: the field updates instantly, but the loading message only appears once
     * typing has actually stopped — so it never flickers per keystroke.
     */
    fun runSearch(text: String) {
        searchJob?.cancel()
        q = text
        if (text.isBlank()) { phase = SearchPhase.Idle; return }
        phase = SearchPhase.Typing
        searchJob = viewModelScope.launch {
            delay(340)
            searchMsg = Catalogue.searchMessages.random()
            phase = SearchPhase.Searching
            delay(760)
            phase = SearchPhase.Done
            if (settings.showRecents) {
                val t = q.trim()
                updateLibrary { copy(recents = (listOf(t) + recents.filter { !it.equals(t, ignoreCase = true) }).take(6)) }
            }
        }
    }

    fun clearRecents() { updateLibrary { copy(recents = emptyList()) }; flash("Recent searches cleared") }

    // ---- sketch actions ------------------------------------------------------------------

    private fun downloadBlocked(): Boolean {
        if (!online) { flash("Offline — connect to download"); return true }
        if (conn == Conn.Mobile && settings.wifiOnly) { flash("Wi-Fi only is on — Settings › Offline"); return true }
        return false
    }

    fun toggleDownload(n: Int) {
        if (isOffline(n)) {
            updateLibrary { copy(offline = offline - n) }
            flash("Removed from offline")
            return
        }
        if (dl == n || downloadBlocked()) return
        dlJob?.cancel()
        dl = n; pct = 0
        val from = System.currentTimeMillis()
        dlJob = viewModelScope.launch {
            while (true) {
                val p = ((System.currentTimeMillis() - from) / 1400f * 100).roundToInt().coerceAtMost(100)
                pct = p
                if (p >= 100) break
                delay(60)
            }
            dl = null
            updateLibrary { if (n in offline) this else copy(offline = offline + n) }
            pulse("dl", "Downloaded")
        }
    }

    fun toggleFavourite(n: Int) {
        val was = isFavourite(n)
        updateLibrary { copy(favourites = if (was) favourites - n else favourites + n) }
        pulse("fav", if (was) "Removed" else "Favourited")
    }

    fun toggleGallery(n: Int) {
        val was = inGallery(n)
        updateLibrary { copy(gallery = if (was) gallery - n else gallery + n) }
        pulse("gal", if (was) "Removed" else "Saved to gallery")
    }

    fun share(s: Sketch) {
        effectChannel.trySend(Effect.Share(Catalogue.credit(s)))
        pulse("share", "Credit attached")
    }

    fun retryFetch(n: Int) {
        if (online) toggleDownload(n) else flash("Still offline — connect to download this one")
    }

    fun reveal(n: Int) {
        revealed = revealed + n
        flash("Loaded over mobile data")
    }

    fun removeFavourite(n: Int) { updateLibrary { copy(favourites = favourites - n) }; flash("Removed from favourites") }
    fun removeOffline(n: Int) { updateLibrary { copy(offline = offline - n) }; flash("Offline copy deleted") }

    // ---- podcast -------------------------------------------------------------------------

    fun isEpOffline(num: Int) = num in library.epOffline
    fun isEpFav(num: Int) = num in library.epFav

    fun toggleEpFav(num: Int) {
        val was = isEpFav(num)
        updateLibrary { copy(epFav = if (was) epFav - num else epFav + num) }
        flash(if (was) "Removed from favourites" else "Added to favourites")
    }

    fun toggleEpDownload(num: Int) {
        if (isEpOffline(num)) {
            updateLibrary { copy(epOffline = epOffline - num) }
            flash("Episode removed")
            return
        }
        if (epDl == num || downloadBlocked()) return
        epDlJob?.cancel()
        epDl = num; epPct = 0
        val from = System.currentTimeMillis()
        epDlJob = viewModelScope.launch {
            while (true) {
                val p = ((System.currentTimeMillis() - from) / 1800f * 100).roundToInt().coerceAtMost(100)
                epPct = p
                if (p >= 100) break
                delay(60)
            }
            epDl = null
            updateLibrary { if (num in epOffline) this else copy(epOffline = epOffline + num) }
            pulse("epdl", "Downloaded")
        }
    }

    fun removeEpFav(num: Int) { updateLibrary { copy(epFav = epFav - num) }; flash("Removed from favourites") }
    fun removeEpOffline(num: Int) { updateLibrary { copy(epOffline = epOffline - num) }; flash("Episode deleted") }

    /** Position shown for an episode: live for the one in the player, the start for any other. */
    fun posFor(i: Int) = if (i == audioEp) pos else 0f

    fun togglePlay(i: Int = ep) {
        val e = Catalogue.episodes[i]
        if (playing && i == audioEp) { pause(); return }
        if (!isEpOffline(e.num) && !online) { flash("Offline — download this episode to listen"); return }
        if (i != audioEp) { audioEp = i; pos = 0f }
        play()
    }

    fun playLatest() {
        openEp(0)
        if (!(playing && audioEp == 0)) togglePlay(0)
    }

    private fun play() {
        playing = true
        if (settings.sleep > 0 && sleepEndsAt == null) sleepEndsAt = System.currentTimeMillis() + settings.sleep * 60_000L
        playJob?.cancel()
        playJob = viewModelScope.launch {
            var last = System.currentTimeMillis()
            while (isActive) {
                delay(250)
                val now = System.currentTimeMillis()
                val e = Catalogue.episodes[audioEp]
                pos = (pos + (now - last) / 1000f * settings.speed / e.seconds).coerceAtMost(1f)
                last = now
                sleepEndsAt?.let { if (now >= it) { pause(); flash("Sleep timer — paused"); return@launch } }
                if (pos >= 1f) { finished(); return@launch }
            }
        }
    }

    fun pause() {
        playing = false
        playJob?.cancel()
        sleepEndsAt = null
    }

    private fun finished() {
        val e = Catalogue.episodes[audioEp]
        pause()
        if (settings.autoDelete && isEpOffline(e.num)) updateLibrary { copy(epOffline = epOffline - e.num) }
        val next = audioEp + 1
        if (settings.continuous && next <= Catalogue.episodes.lastIndex) {
            audioEp = next; pos = 0f
            if (isEpOffline(Catalogue.episodes[next].num) || online) play()
        } else {
            pos = 0f
        }
    }

    fun seek(i: Int, fraction: Float) {
        if (i != audioEp) { audioEp = i; if (playing) pause() }
        pos = fraction.coerceIn(0f, 1f)
    }

    fun skip(i: Int, seconds: Int) {
        val e = Catalogue.episodes[i]
        seek(i, posFor(i) + seconds.toFloat() / e.seconds)
    }

    fun cycleSpeed() {
        val o = listOf(1f, 1.25f, 1.5f, 2f)
        updateSettings { copy(speed = o[(o.indexOf(speed) + 1) % o.size]) }
    }

    fun setSleep(m: Int) {
        updateSettings { copy(sleep = m) }
        sleepEndsAt = if (m > 0 && playing) System.currentTimeMillis() + m * 60_000L else null
    }

    // ---- settings › storage --------------------------------------------------------------

    val mbSketch get() = if (settings.hd) 0.35 else 0.15
    val mbEp = 31.5
    val mbBase = 4.1
    val sketchMB get() = library.offline.size * mbSketch
    val podMB get() = library.epOffline.size * mbEp
    val libMB get() = mbBase + sketchMB + podMB

    fun askBulk(sketches: Boolean) {
        if (!online) return flash("Offline — connect to download")
        ask(if (sketches) Confirm(
            "Download every sketch?",
            "About ${(Catalogue.TOTAL_POSTS * mbSketch).roundToInt()} MB for all ${Catalogue.TOTAL_POSTS} posts. Runs on Wi-Fi only and pauses if you leave.",
            "Download all",
        ) { bulk(true) } else Confirm(
            "Download every episode?",
            "About 1.8 GB for ${Catalogue.TOTAL_EPISODES} episodes — roughly twelve times the whole sketch archive. Wi-Fi only, and it will take a while.",
            "Download anyway",
        ) { bulk(false) })
    }

    private fun bulk(sketches: Boolean) {
        if (conn != Conn.Wifi) return flash("Waiting for Wi-Fi — bulk downloads never use mobile data")
        if (bulkJob?.isActive == true) return flash("A download is already running")
        bulkJob = viewModelScope.launch {
            val from = System.currentTimeMillis()
            val span = if (sketches) 3000f else 5000f
            while (true) {
                val p = ((System.currentTimeMillis() - from) / span * 100).roundToInt().coerceAtMost(100)
                if (sketches) bulkSketches = p else bulkEpisodes = p
                if (p >= 100) break
                if (conn != Conn.Wifi) { bulkSketches = null; bulkEpisodes = null; flash("Paused — off Wi-Fi"); return@launch }
                delay(60)
            }
            if (sketches) {
                bulkSketches = null
                updateLibrary { copy(offline = (offline + Catalogue.sketches.map { it.n }).distinct()) }
                pulse("bulkS", "Downloaded")
            } else {
                bulkEpisodes = null
                updateLibrary { copy(epOffline = (epOffline + Catalogue.episodes.map { it.num }).distinct()) }
                pulse("bulkE", "Downloaded")
            }
        }
    }

    private fun plural(n: Int, one: String, many: String) = "$n ${if (n == 1) one else many}"
    private fun mb(v: Double) = String.format(java.util.Locale.UK, "%.1f MB", v)

    fun askDeleteSketches() = ask(Confirm(
        "Delete downloaded sketches?",
        "This removes ${plural(library.offline.size, "sketch", "sketches")} — ${mb(sketchMB)}. Your favourites stay.",
        "Delete sketches",
    ) { updateLibrary { copy(offline = emptyList()) }; flash("Downloaded sketches deleted") })

    fun askDeleteEpisodes() = ask(Confirm(
        "Delete downloaded episodes?",
        "This removes ${plural(library.epOffline.size, "episode", "episodes")} — ${mb(podMB)}. Your favourites stay.",
        "Delete episodes",
    ) { updateLibrary { copy(epOffline = emptyList()) }; flash("Downloaded episodes deleted") })

    fun askDeleteAll() = ask(Confirm(
        "Delete all downloads?",
        "This removes ${plural(library.offline.size, "sketch", "sketches")} and ${plural(library.epOffline.size, "episode", "episodes")} — ${mb(sketchMB + podMB)}. Your favourites stay.",
        "Delete everything",
    ) { updateLibrary { copy(offline = emptyList(), epOffline = emptyList()) }; flash("Downloads cleared") })

    fun tapVersion() {
        verTaps++
        if (verTaps >= 10) { verTaps = 0; egg = true; return }
        if (verTaps >= 6) flash("${10 - verTaps} more…")
    }

    // ---- outbound ------------------------------------------------------------------------

    fun sendContact(name: String, email: String, message: String) {
        if (message.isBlank()) return flash("Write a message first")
        effectChannel.trySend(Effect.Email(
            "hello@sketchplanations.com", "For the podcast" + if (name.isNotBlank()) " — from $name" else "",
            message + if (email.isNotBlank()) "\n\nReply to: $email" else "",
        ))
    }

    fun subscribe(email: String) {
        val e = email.trim()
        openUrl("https://sketchplanations.substack.com/subscribe" + if (e.isNotEmpty()) "?email=" + java.net.URLEncoder.encode(e, "UTF-8") else "")
    }
}
