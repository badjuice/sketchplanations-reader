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
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.rdcl.nox.sketchplanations.explode.Episode
import org.rdcl.nox.sketchplanations.explode.Link
import org.rdcl.nox.sketchplanations.explode.LinkTarget
import org.rdcl.nox.sketchplanationsreader.data.AppSettings
import org.rdcl.nox.sketchplanationsreader.data.Conn
import org.rdcl.nox.sketchplanationsreader.data.Library
import org.rdcl.nox.sketchplanationsreader.data.SearchMessages
import org.rdcl.nox.sketchplanationsreader.data.Store
import org.rdcl.nox.sketchplanationsreader.data.SyncResult
import org.rdcl.nox.sketchplanationsreader.data.connectivity
import org.rdcl.nox.sketchplanationsreader.data.db.SketchSummary
import org.rdcl.nox.sketchplanationsreader.data.db.TopicCount
import java.net.URI
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import org.rdcl.nox.sketchplanations.explode.Images
import org.rdcl.nox.sketchplanations.explode.Rendition
import org.rdcl.nox.sketchplanationsreader.ReaderApp
import org.rdcl.nox.sketchplanationsreader.work.Bulk
import org.rdcl.nox.sketchplanationsreader.data.Gallery
import org.rdcl.nox.sketchplanationsreader.data.bytes
import org.rdcl.nox.sketchplanationsreader.ui.image
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import org.rdcl.nox.sketchplanationsreader.playback.AudioPlayer
import kotlin.math.roundToInt

enum class Screen(val title: String, val top: Boolean) {
    Onboarding("Welcome", false),
    Today("Sketchplanations", true),
    Detail("", false),
    Archive("Archive", true),
    Search("Search", true),
    Library("Library", true),
    Podcast("Podcast", true),
    Prints("Support the author", true),
    About("About this app", true),
    Settings("Settings", true),
    Episode("Episode", false),
    Tag("", false),
    PodcastAbout("About the podcast", false),
    PodcastContact("Contact the show", false),
}

/** A history entry. Sketch → linked sketch → episode all share screen names, so each frame snapshots its subject too. */
data class Frame(val screen: Screen, val detail: String, val ep: Int, val tag: String, val acc: String, val zoomUrl: String?)

/** An on/off row inside a confirmation, e.g. Wi-Fi only before a bulk download. */
class ConfirmToggle(val label: String, val hint: String, val on: () -> Boolean, val flip: () -> Unit)

data class Confirm(val title: String, val body: String, val cta: String, val toggle: ConfirmToggle? = null, val danger: Boolean = true, val act: () -> Unit)

enum class SyncState { Idle, Running, Done, Failed }
enum class Sort { Az, Date }
enum class SearchPhase { Idle, Typing, Searching, Done }

sealed interface Effect {
    data class OpenUrl(val url: String, val inApp: Boolean) : Effect
    data class Share(val text: String) : Effect
    data class Email(val to: String, val subject: String, val body: String) : Effect
    /** Android 8–9 only: storage permission for saving into Pictures. */
    data object RequestStorage : Effect
    /** Android 13+: permission to show the download progress notification. */
    data object RequestNotifications : Effect
}

class AppModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    val repo = (app as ReaderApp).repo

    var loaded by mutableStateOf(false); private set
    var settings by mutableStateOf(AppSettings()); private set
    var library by mutableStateOf(Library()); private set
    var conn by mutableStateOf(Conn.Wifi); private set

    // Content, read from the device. Everything below works offline.
    /** Every sketch, newest first. */
    var sketches by mutableStateOf(emptyList<SketchSummary>()); private set
    var byUid by mutableStateOf(emptyMap<String, SketchSummary>()); private set
    /** Topics with sketch counts, most used first. */
    var topics by mutableStateOf(emptyList<TopicCount>()); private set
    /** Podcast episodes, newest first. */
    var episodes by mutableStateOf(emptyList<Episode>()); private set
    /** Bumped whenever stored content changes, so screens reload. */
    var contentVersion by mutableIntStateOf(0); private set

    // Navigation
    var screen by mutableStateOf(Screen.Today); private set
    var stack by mutableStateOf(emptyList<Frame>()); private set
    /** The open sketch's uid. */
    var detail by mutableStateOf(""); private set
    /** Index into [episodes]. */
    var ep by mutableIntStateOf(0); private set
    /** Topic uid. */
    var tag by mutableStateOf(""); private set
    var acc by mutableStateOf("what")
    /** The top bar's ⋮ menu is open. */
    var menu by mutableStateOf(false)
    /** Bumped when a re-tapped tab asks the current list to scroll home. */
    var scrollTopTick by mutableIntStateOf(0); private set

    // Onboarding + catalogue sync
    var ob by mutableIntStateOf(0)
    var syncState by mutableStateOf(SyncState.Idle); private set
    var syncN by mutableIntStateOf(0); private set
    /** Sketches the catalogue fetch expects, as the site reports it. */
    var syncTotal by mutableIntStateOf(0); private set

    // Per-view modes, reset on entry
    var sort by mutableStateOf(Sort.Date)
    var asc by mutableStateOf(false)
    /** Archive shows the topic index instead of the sketch list. */
    var archiveTopics by mutableStateOf(false)
    var catByFreq by mutableStateOf(true)
    var libFavTab by mutableStateOf(true)
    var deckIdx by mutableIntStateOf(0)

    // Search
    var q by mutableStateOf(""); private set
    var phase by mutableStateOf(SearchPhase.Idle); private set
    var searchMsg by mutableStateOf(SearchMessages.all[0]); private set
    var results by mutableStateOf(emptyList<String>()); private set

    // Downloads. Progress lives inside the button that started it.
    var dl by mutableStateOf<String?>(null); private set
    var pct by mutableIntStateOf(0); private set
    var epDl by mutableStateOf<String?>(null); private set
    var epPct by mutableIntStateOf(0); private set
    /** The running or paused "keep everything offline" job, from [Bulk]. */
    var bulk by mutableStateOf<Bulk.Progress?>(null); private set
    /** Settings scrolls to this group once, e.g. "storage" from the download notification. */
    var settingsFocus by mutableStateOf<String?>(null)
    var syncPct by mutableStateOf<Int?>(null); private set
    var pulse by mutableStateOf<Pair<String, String>?>(null); private set
    /** "Load image" reveals for this session only; it never touches the download cache. */
    var revealed by mutableStateOf(emptySet<String>()); private set

    // Player
    var playing by mutableStateOf(false); private set
    var audioEp by mutableIntStateOf(0); private set
    var pos by mutableFloatStateOf(0f); private set
    var sleepEndsAt by mutableStateOf<Long?>(null); private set
    /** Length of the loaded audio as decoded; ad insertion can make it differ from the feed. */
    var actualSeconds by mutableIntStateOf(0); private set
    private val audio = AudioPlayer(app)
    private val http = OkHttpClient.Builder().callTimeout(10, java.util.concurrent.TimeUnit.MINUTES).build()
    /** Stored drawings and episodes. Screens ask it what to load for an image. */
    val downloads = (app as ReaderApp).downloads
    private var lastChapter: String? = null

    // Transient UI
    /** The image open in the zoom viewer, or null. */
    var zoomUrl by mutableStateOf<String?>(null)
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

    init {
        viewModelScope.launch {
            val (s, l) = store.load()
            settings = s
            library = l
            reloadContent()
            refreshSizes()
            library = library.copy(indexBuilt = sketches.isNotEmpty())
            if (!l.onboarded) screen = Screen.Onboarding
            loaded = true
            launch { snapshotFlow { settings }.drop(1).collect { store.save(it) } }
            launch { snapshotFlow { library }.drop(1).collect { store.save(it) } }
            // The incremental catalogue check also runs on launch when online.
            delay(1500)
            if (online && library.indexBuilt) syncWithWebsite(quiet = true)
        }
        viewModelScope.launch { Bulk.progress.collect { bulk = it } }
        viewModelScope.launch {
            var n = 0
            Bulk.saved.collect { (kind, id) ->
                updateLibrary {
                    if (kind == Bulk.Kind.Sketches) { if (id in offline) this else copy(offline = offline + id) }
                    else { if (id in epOffline) this else copy(epOffline = epOffline + id) }
                }
                if (++n % 20 == 0 || kind == Bulk.Kind.Episodes) refreshSizes()
            }
        }
        viewModelScope.launch {
            Bulk.finished.collect { p ->
                refreshSizes()
                pulse(if (p.kind == Bulk.Kind.Sketches) "bulkS" else "bulkE", if (p.failed == 0) "Done" else "${p.failed} failed")
            }
        }
        viewModelScope.launch { snapshotFlow { settings.speed }.drop(1).collect { audio.player?.setPlaybackSpeed(it) } }
        viewModelScope.launch { snapshotFlow { settings.pauseOnCall }.collect { audio.setPauseForCalls(it) } }
        // Leaving the app pauses audio unless it may carry on in the background, which needs the
        // shade notification Android requires for background playback.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                if (playing && !settings.bgPlay) pause()
            }
        })
        viewModelScope.launch {
            var first = true
            connectivity(app).collect { c ->
                val was = conn
                conn = c
                if (first) { first = false; return@collect }
                when {
                    was == Conn.Off && c != Conn.Off ->
                        flash(if (screen == Screen.Detail) "Back online. Tap to load this sketch" else "Back online")
                    c == Conn.Off -> flash("Offline. Everything stored on the phone still opens.")
                    else -> flash(if (c == Conn.Wifi) "On Wi-Fi" else "On mobile data")
                }
            }
        }
    }

    /**
     * Sketches and topics come first, since every opening screen needs them. The podcast feed is
     * parsed afterwards so it never holds up the first frame.
     */
    override fun onCleared() {
        audio.release()
    }

    private suspend fun reloadContent() {
        val all = repo.summaries()
        sketches = all
        byUid = all.associateBy { it.uid }
        // The site writes topic names in lower case; the app shows them capitalised.
        topics = repo.topics().map { t -> t.copy(name = t.name.replaceFirstChar { it.titlecase() }) }
        if (detail.isEmpty()) detail = all.firstOrNull()?.uid.orEmpty()
        contentVersion++
        viewModelScope.launch {
            episodes = repo.podcast()?.episodes.orEmpty()
            reconcileOffline()
            ep = ep.coerceIn(0, (episodes.size - 1).coerceAtLeast(0))
            audioEp = audioEp.coerceIn(0, (episodes.size - 1).coerceAtLeast(0))
        }
    }

    /**
     * The download service stores files even while no screen is open to note them, so anything
     * on disk counts as offline. Deleting always removes the files too, so the two can't disagree.
     */
    private fun reconcileOffline() {
        viewModelScope.launch(Dispatchers.IO) {
            val s = sketches.filter { it.uid !in library.offline && downloads.hasImage(it.image, Rendition.Read) }.map { it.uid }
            val e = episodes.filter { it.guid !in library.epOffline && downloads.hasEpisode(it.guid) }.map { it.guid }
            if (s.isNotEmpty() || e.isNotEmpty()) {
                launch(Dispatchers.Main) { updateLibrary { copy(offline = offline + s, epOffline = epOffline + e) } }
            }
        }
    }

    // ---- helpers -------------------------------------------------------------------------

    val online get() = conn != Conn.Off
    val sketch: SketchSummary? get() = byUid[detail]
    val episode: Episode? get() = episodes.getOrNull(ep)
    fun topicName(uid: String) = topics.firstOrNull { it.uid == uid }?.name ?: uid.replace('-', ' ')

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

    fun isOffline(uid: String) = uid in library.offline
    fun isFavourite(uid: String) = uid in library.favourites
    fun inGallery(uid: String) = uid in library.gallery

    /** Offline and not downloaded: nothing on the device to show. */
    fun needsNet(uid: String) = !isOffline(uid) && !online
    /** On mobile data with Images on Wi-Fi only: the image waits behind Load image. */
    fun imageHeld(uid: String) = !isOffline(uid) && conn == Conn.Mobile && settings.imgWifiOnly && uid !in revealed

    // ---- navigation ----------------------------------------------------------------------

    private fun frame() = Frame(screen, detail, ep, tag, acc, zoomUrl)

    /** Every view resets to its default on entry. No screen remembers a filter set three taps ago. */
    private fun applyDefaults(s: Screen) {
        when (s) {
            Screen.Archive -> { archiveTopics = false; catByFreq = true; sort = Sort.Date; asc = false }
            Screen.Library -> libFavTab = true
            Screen.Search -> { searchJob?.cancel(); q = ""; phase = SearchPhase.Idle; results = emptyList() }
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
        menu = false
        zoomUrl = null
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

    fun open(uid: String) {
        if (uid !in byUid) { openUrl("https://sketchplanations.com/$uid"); return }
        moveTo(Screen.Detail, push = true)
        detail = uid
    }

    fun openEp(i: Int) {
        if (episodes.isEmpty()) return
        moveTo(Screen.Episode, push = true)
        ep = i.coerceIn(0, episodes.lastIndex)
    }

    /** Archive, opened on its topic index. */
    fun openTopics() {
        go(Screen.Archive)
        archiveTopics = true
    }

    /** Swiping between sketches changes the one shown without adding to the back stack. */
    fun showSketch(uid: String) {
        if (uid in byUid) detail = uid
    }

    /** Moves the open sketch [delta] places through the list: +1 is older, -1 is newer. */
    fun stepSketch(delta: Int) {
        val i = sketches.indexOfFirst { it.uid == detail }
        sketches.getOrNull(i + delta)?.let { detail = it.uid }
    }

    fun openTag(uid: String) {
        moveTo(Screen.Tag, push = true)
        tag = uid
    }

    /** From the download notification: Settings, scrolled to Storage. */
    fun openStorage() {
        if (screen == Screen.Onboarding) return
        go(Screen.Settings)
        settingsFocus = "storage"
    }

    fun openAboutHosts() = goDeep(Screen.About) { acc = "hosts" }

    fun canGoBack() = stack.isNotEmpty() || (screen != Screen.Today && screen != Screen.Onboarding)

    /** Pops one level, back to exactly the previous item rather than the previous screen type. */
    fun back() {
        zoomUrl = null
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
        val s = sketches.randomOrNull() ?: return flash("Nothing stored yet. Sync first")
        open(s.uid)
        flash("A random one: ${s.title}")
    }

    /** Links to other sketches, topics and episodes open in the app; everything else leaves it. */
    fun follow(link: Link) {
        when (val t = link.target) {
            is LinkTarget.Sketch -> open(t.uid)
            is LinkTarget.Topic -> if (topics.any { it.uid == t.uid }) openTag(t.uid) else openUrl("https://sketchplanations.com/categories/${t.uid}")
            is LinkTarget.Episode -> episodeFor(t.url)?.let(::openEp) ?: openUrl(t.url)
            is LinkTarget.ImageVersion -> zoomUrl = t.url
            is LinkTarget.External -> openUrl(t.url)
        }
    }

    /** Podcast-site URLs carry the episode's title as a slug; failing that, the episode about this sketch. */
    private fun episodeFor(url: String): Int? {
        val slug = runCatching { URI(url).path.trim('/') }.getOrNull().orEmpty()
        fun slugOf(t: String) = t.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return episodes.indexOfFirst { slug.isNotEmpty() && slugOf(it.title) == slug }.takeIf { it >= 0 }
            ?: episodes.indexOfFirst { it.sketchUid == detail }.takeIf { it >= 0 }
    }

    fun openUrl(url: String) {
        effectChannel.trySend(Effect.OpenUrl(url, inApp = !settings.extBrowser))
    }

    fun ask(c: Confirm) { confirm = c }

    // ---- onboarding / catalogue sync -----------------------------------------------------

    fun nextOnboarding() {
        if (ob < 3) {
            if (ob == 2 && sketches.isEmpty()) startSync()
            if (ob == 2 && sketches.isNotEmpty()) syncState = SyncState.Done
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
        "Today, Archive, Topics and Search stay empty until it runs. Archive will offer to build it again whenever you're online.",
        "Skip it",
    ) {
        syncJob?.cancel()
        syncState = SyncState.Failed; syncN = 0
        finishOnboarding()
        flash("Index skipped. Archive will offer it again")
    })

    /** First-run catalogue fetch: every sketch with its words, about ten requests. */
    fun startSync() {
        syncJob?.cancel()
        if (!online) { syncState = SyncState.Failed; syncN = 0; return }
        syncState = SyncState.Running; syncN = 0
        syncJob = viewModelScope.launch {
            when (repo.sync { done, total -> syncN = done; syncTotal = total }) {
                is SyncResult.Done -> {
                    reloadContent()
                    syncState = SyncState.Done
                    syncN = sketches.size; syncTotal = syncN
                    updateLibrary { copy(indexBuilt = true, lastSync = System.currentTimeMillis()) }
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
        if (!online) { if (!quiet) flash("Offline. Connect to check for new sketches"); return }
        if (syncPct != null || syncState == SyncState.Running) return
        syncPct = 0
        viewModelScope.launch {
            val result = repo.sync { done, total -> if (total > 0) syncPct = (done * 100 / total).coerceIn(0, 99) }
            syncPct = null
            when (result) {
                is SyncResult.Done -> {
                    val before = sketches.map { it.uid }.toSet()
                    reloadContent()
                    autoDownload(sketches.filter { it.uid !in before })
                    updateLibrary { copy(indexBuilt = true, lastSync = System.currentTimeMillis()) }
                    if (syncState != SyncState.Running) syncState = SyncState.Done
                    if (!quiet) pulse("sync", if (result.changed == 0) "Up to date" else "${result.changed} updated")
                    else if (result.changed > 0) flash(if (result.changed == 1) "1 sketch new or updated" else "${result.changed} sketches new or updated")
                }
                SyncResult.Offline -> if (!quiet) flash("Offline. Connect to check for new sketches")
                is SyncResult.Failed -> if (!quiet) flash("Couldn't reach the site. Try again later")
            }
        }
    }

    /** "Auto-download new sketches": fetch new arrivals after a sync, respecting Wi-Fi only. */
    private fun autoDownload(fresh: List<SketchSummary>) {
        if (!settings.autoDl || fresh.isEmpty() || fresh.size > 20) return
        if (conn != Conn.Wifi && settings.wifiOnly) return
        viewModelScope.launch {
            fresh.forEach { s ->
                runCatching { downloads.saveImages(s.image, renditions()) }
                    .onSuccess { updateLibrary { if (s.uid in offline) this else copy(offline = offline + s.uid) } }
            }
            refreshSizes()
        }
    }

    // ---- search --------------------------------------------------------------------------

    /**
     * Debounced: the field updates instantly, but the loading message only appears once
     * typing has actually stopped, so it never flickers per keystroke. Searches the words
     * stored on the device, so it works offline.
     */
    fun runSearch(text: String) {
        searchJob?.cancel()
        q = text
        if (text.isBlank()) { phase = SearchPhase.Idle; results = emptyList(); return }
        phase = SearchPhase.Typing
        searchJob = viewModelScope.launch {
            delay(340)
            searchMsg = SearchMessages.all.random()
            phase = SearchPhase.Searching
            val found = searchLocally(text)
            delay(500)
            results = found
            phase = SearchPhase.Done
            if (settings.showRecents) {
                val t = q.trim()
                updateLibrary { copy(recents = (listOf(t) + recents.filter { !it.equals(t, ignoreCase = true) }).take(6)) }
            }
        }
    }

    /** "589" or "#589" jumps to that number; anything else is a text search. */
    private suspend fun searchLocally(text: String): List<String> {
        Regex("^#?(\\d+)$").find(text.trim())?.let { m ->
            val n = m.groupValues[1].toInt()
            return sketches.filter { it.ordinal == n }.map { it.uid }
        }
        return repo.search(text)
    }

    fun clearRecents() { updateLibrary { copy(recents = emptyList()) }; flash("Recent searches cleared") }

    // ---- sketch actions ------------------------------------------------------------------

    private fun downloadBlocked(): Boolean {
        if (!online) { flash("Offline. Connect to keep it offline"); return true }
        if (conn == Conn.Mobile && settings.wifiOnly) { flash("Wi-Fi only is on. Settings › Offline"); return true }
        return false
    }

    /** The drawing at reading size, plus HD when that setting is on. The words are always stored. */
    private fun renditions() = if (settings.hd) listOf(Rendition.Read, Rendition.Hd) else listOf(Rendition.Read)

    fun toggleDownload(uid: String) {
        val s = byUid[uid] ?: return
        if (isOffline(uid)) {
            downloads.deleteImages(s.image)
            updateLibrary { copy(offline = offline - uid) }
            refreshSizes()
            flash("Removed from offline")
            return
        }
        if (dl == uid || downloadBlocked()) return
        dlJob?.cancel()
        dl = uid; pct = 0
        dlJob = viewModelScope.launch {
            try {
                downloads.saveImages(s.image, renditions()) { pct = (it * 100).roundToInt() }
                updateLibrary { if (uid in offline) this else copy(offline = offline + uid) }
                pulse("dl", "Kept offline")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                flash("Couldn't keep it offline. Check the connection and try again")
            } finally {
                dl = null
                refreshSizes()
            }
        }
    }

    fun toggleFavourite(uid: String) {
        val was = isFavourite(uid)
        updateLibrary { copy(favourites = if (was) favourites - uid else favourites + uid) }
        pulse("fav", if (was) "Removed" else "Favourited")
    }

    /** Alternate versions saved this session, by URL; the sketch's own drawing is tracked in the library. */
    var savedUrls by mutableStateOf(emptySet<String>()); private set
    private var afterStoragePermission: (() -> Unit)? = null

    private fun isOwnImage(s: SketchSummary, url: String) =
        url == downloads.image(s.image, Rendition.Hd) || url == downloads.image(s.image, Rendition.Read) || url == s.image.original

    fun imageSaved(uid: String, url: String) = byUid[uid]?.let { if (isOwnImage(it, url)) inGallery(uid) else url in savedUrls } ?: false

    /**
     * Copies the picture open in the viewer into Pictures › Sketchplanations, with the credit in its
     * name and text fields. For the sketch's own drawing that's a 2400 px PNG; versions save as shown.
     */
    fun saveToGallery(uid: String, url: String) {
        val s = byUid[uid] ?: return
        if (imageSaved(uid, url)) return flash("Already saved. Look in Pictures › Sketchplanations")
        val own = isOwnImage(s, url)
        viewModelScope.launch {
            pulse("gal", "Saving…")
            val img = s.image
            val (bytes, mime) = try {
                when {
                    url.startsWith("file:") && !own -> java.io.File(Uri.parse(url).path!!).readBytes() to "image/webp"
                    !own -> http.bytes(url) to "image/png"
                    img.isAnimated -> http.bytes(img.original) to "image/gif"
                    // Ask for PNG outright: with auto=format the image service would pick JPEG or WebP.
                    else -> http.bytes(img.original + "?fm=png&w=2400&fit=max") to "image/png"
                }
            } catch (e: Exception) {
                // Offline: fall back to a downloaded copy if there is one.
                val local = (if (own) downloads.image(img, Rendition.Hd) else url).takeIf { it.startsWith("file:") }
                    ?.let { java.io.File(Uri.parse(it).path!!).readBytes() }
                    ?: return@launch run { pulse = null; flash("Offline. Connect to save this image") }
                local to (if (img.isAnimated) "image/gif" else "image/webp")
            }
            val title = if (own) s.title else "${s.title}, another version"
            val credit = Gallery.Credit(title, "https://sketchplanations.com/${s.uid}")
            when (val res = Gallery.save(getApplication(), bytes, title, mime, credit)) {
                is Gallery.Result.Saved -> {
                    if (own) updateLibrary { if (uid in gallery) this else copy(gallery = gallery + uid) } else savedUrls = savedUrls + url
                    pulse("gal", "Saved")
                    flash("Saved to ${res.where}")
                }
                Gallery.Result.NeedsPermission -> {
                    pulse = null
                    afterStoragePermission = { saveToGallery(uid, url) }
                    effectChannel.trySend(Effect.RequestStorage)
                }
                is Gallery.Result.Failed -> { pulse = null; flash("Couldn't save the image") }
            }
        }
    }

    /** Android 8–9: the storage prompt's answer. A yes finishes the save that asked for it. */
    fun onStoragePermission(granted: Boolean) {
        val next = afterStoragePermission
        afterStoragePermission = null
        if (granted) next?.invoke() else flash("Saving to Pictures needs storage access")
    }

    /** Shares carry the credit in the wording the licence page asks for, with the sketch's address. */
    fun share(s: SketchSummary) {
        effectChannel.trySend(Effect.Share("${s.title}\nImage: Jono Hey, Sketchplanations\nhttps://sketchplanations.com/${s.uid}"))
        pulse("share", "Credit attached")
    }

    fun retryFetch(uid: String) {
        if (online) toggleDownload(uid) else flash("Still offline. Connect to load this one")
    }

    fun reveal(uid: String) {
        revealed = revealed + uid
        flash("Loaded over mobile data")
    }

    fun removeFavourite(uid: String) { updateLibrary { copy(favourites = favourites - uid) }; flash("Removed from favourites") }
    fun removeOffline(uid: String) {
        byUid[uid]?.let { downloads.deleteImages(it.image) }
        updateLibrary { copy(offline = offline - uid) }
        refreshSizes()
        flash("Offline copy deleted")
    }

    // ---- podcast -------------------------------------------------------------------------

    fun isEpOffline(guid: String) = guid in library.epOffline
    fun isEpFav(guid: String) = guid in library.epFav

    fun toggleEpFav(guid: String) {
        val was = isEpFav(guid)
        updateLibrary { copy(epFav = if (was) epFav - guid else epFav + guid) }
        flash(if (was) "Removed from favourites" else "Added to favourites")
    }

    fun toggleEpDownload(guid: String) {
        val e = episodes.firstOrNull { it.guid == guid } ?: return
        if (isEpOffline(guid)) {
            downloads.deleteEpisode(guid)
            updateLibrary { copy(epOffline = epOffline - guid) }
            refreshSizes()
            flash("Episode removed")
            return
        }
        val url = e.audioUrl ?: return flash("This episode has no audio in the feed")
        if (epDl == guid || downloadBlocked()) return
        epDlJob?.cancel()
        epDl = guid; epPct = 0
        epDlJob = viewModelScope.launch {
            try {
                downloads.saveEpisode(guid, url) { epPct = (it * 100).roundToInt() }
                updateLibrary { if (guid in epOffline) this else copy(epOffline = epOffline + guid) }
                pulse("epdl", "Kept offline")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                flash("Couldn't keep it offline. Check the connection and try again")
            } finally {
                epDl = null
                refreshSizes()
            }
        }
    }

    fun removeEpFav(guid: String) { updateLibrary { copy(epFav = epFav - guid) }; flash("Removed from favourites") }
    fun removeEpOffline(guid: String) {
        downloads.deleteEpisode(guid)
        updateLibrary { copy(epOffline = epOffline - guid) }
        refreshSizes()
        flash("Episode deleted")
    }

    /** Position shown for an episode: live for the one in the player, the start for any other. */
    fun posFor(i: Int) = if (i == audioEp) pos else 0f

    fun togglePlay(i: Int = ep) {
        val e = episodes.getOrNull(i) ?: return
        if (playing && i == audioEp) { pause(); return }
        if (!isEpOffline(e.guid) && !online) { flash("Offline. Keep this episode offline to listen without a connection"); return }
        if (i != audioEp) { audioEp = i; pos = 0f; actualSeconds = 0 }
        play()
    }

    fun playLatest() {
        if (episodes.isEmpty()) return flash("No episodes stored yet. Sync first")
        openEp(0)
        if (!(playing && audioEp == 0)) togglePlay(0)
    }

    /** The current episode's real length once the player knows it, else the feed's figure. */
    fun seconds(e: Episode) =
        if (actualSeconds > 0 && episodes.getOrNull(audioEp)?.guid == e.guid) actualSeconds else e.durationSeconds ?: 45 * 60

    private fun chapterAt(e: Episode, atSeconds: Int) =
        e.chaptersFor(seconds(e)).lastOrNull { it.atSeconds <= atSeconds }?.title

    private fun play() {
        val e = episodes.getOrNull(audioEp) ?: return
        val uri = if (downloads.hasEpisode(e.guid)) Uri.fromFile(downloads.episodeFile(e.guid))
        else Uri.parse(e.audioUrl ?: return flash("This episode has no audio in the feed"))
        playing = true
        if (settings.sleep > 0 && sleepEndsAt == null) sleepEndsAt = System.currentTimeMillis() + settings.sleep * 60_000L
        viewModelScope.launch {
            audio.setListener(playerEvents)
            val p = runCatching { audio.connect(settings.bgPlay) }.getOrElse {
                playing = false
                return@launch flash("Couldn't start the player")
            }
            if (p.currentMediaItem?.mediaId != e.guid) {
                val startMs = (pos * seconds(e) * 1000).toLong()
                lastChapter = chapterAt(e, (startMs / 1000).toInt())
                p.setMediaItem(AudioPlayer.item(e, uri, lastChapter), startMs)
                p.prepare()
            }
            p.setPlaybackSpeed(settings.speed)
            p.play()
            tick()
        }
    }

    /** Mirrors the real player into the UI four times a second while audio is loaded. */
    private fun tick() {
        playJob?.cancel()
        playJob = viewModelScope.launch {
            while (isActive) {
                val p = audio.player ?: break
                val e = episodes.getOrNull(audioEp) ?: break
                if (p.duration != C.TIME_UNSET && p.duration > 0) actualSeconds = (p.duration / 1000).toInt()
                pos = (p.currentPosition / 1000f / seconds(e)).coerceIn(0f, 1f)
                // Keep the lock screen's subtitle on the chapter being heard.
                val chapter = chapterAt(e, (p.currentPosition / 1000).toInt())
                val loaded = p.currentMediaItem?.localConfiguration?.uri
                if (chapter != lastChapter && p.currentMediaItem?.mediaId == e.guid && loaded != null) {
                    lastChapter = chapter
                    p.replaceMediaItem(p.currentMediaItemIndex, AudioPlayer.item(e, loaded, chapter))
                }
                sleepEndsAt?.let { if (System.currentTimeMillis() >= it) { pause(); flash("Paused by the sleep timer"); return@launch } }
                if (!p.playWhenReady) break
                delay(250)
            }
        }
    }

    private val playerEvents = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            playing = playWhenReady
            if (playWhenReady) tick()
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) finished()
        }

        override fun onPlayerError(error: PlaybackException) {
            playing = false
            flash(if (!online) "Offline. Keep this episode offline to listen without a connection" else "Couldn't play this episode. Try again")
        }
    }

    fun pause() {
        playing = false
        audio.player?.pause()
        playJob?.cancel()
        sleepEndsAt = null
    }

    private fun finished() {
        val e = episodes.getOrNull(audioEp)
        pause()
        if (e != null && settings.autoDelete && isEpOffline(e.guid)) {
            audio.player?.clearMediaItems()
            downloads.deleteEpisode(e.guid)
            updateLibrary { copy(epOffline = epOffline - e.guid) }
            refreshSizes()
        }
        // Newest first, so the next episode in order is the one before it in the list.
        val next = audioEp - 1
        if (settings.continuous && next >= 0) {
            audioEp = next; pos = 0f; actualSeconds = 0
            if (isEpOffline(episodes[next].guid) || online) play()
        } else {
            pos = 0f
        }
    }

    fun seek(i: Int, fraction: Float) {
        val f = fraction.coerceIn(0f, 1f)
        if (i != audioEp) { if (playing) pause(); audioEp = i; actualSeconds = 0 }
        pos = f
        val p = audio.player ?: return
        val e = episodes.getOrNull(i) ?: return
        if (p.currentMediaItem?.mediaId == e.guid) p.seekTo((f * seconds(e) * 1000).toLong())
    }

    fun skip(i: Int, secs: Int) {
        val e = episodes.getOrNull(i) ?: return
        seek(i, posFor(i) + secs.toFloat() / seconds(e))
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

    /** Estimate per sketch for planning a bulk download, measured on the first scrape: ~82 KB at reading size, +205 KB in HD. */
    val mbSketch get() = if (settings.hd) 0.29 else 0.09
    fun epMB(e: Episode) = (e.audioBytes ?: 31_500_000L) / 1e6

    // Measured on disk, refreshed whenever downloads change.
    var mbBase by mutableStateOf(0.0); private set
    var sketchMB by mutableStateOf(0.0); private set
    var podMB by mutableStateOf(0.0); private set
    val libMB get() = mbBase + sketchMB + podMB

    private fun refreshSizes() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val db = app.getDatabasePath("content.db").let { f -> listOf(f, java.io.File(f.path + "-wal")).sumOf { it.length() } }
            mbBase = (db + java.io.File(app.filesDir, "podcast.xml").length()) / 1e6
            sketchMB = downloads.imageBytes() / 1e6
            podMB = downloads.episodeBytes() / 1e6
        }
    }

    fun askBulk(all: Boolean) {
        if (!online) return flash("Offline. Connect first")
        if (bulk?.paused == true) return resumeBulk()
        val allEpisodesMB = episodes.sumOf(::epMB)
        val wifi = ConfirmToggle("Wi-Fi only", "Off lets it use mobile data too. Same as Settings › Offline", { settings.wifiOnly }) {
            updateSettings { copy(wifiOnly = !wifiOnly) }
        }
        ask(if (all) Confirm(
            "Keep every sketch offline?",
            "About ${(sketches.size * mbSketch).roundToInt()} MB for all ${sketches.size} sketches. It runs in the background, with progress and Pause in the notification.",
            "Keep all offline", wifi, danger = false,
        ) { bulk(Bulk.Kind.Sketches) } else Confirm(
            "Keep every episode offline?",
            "About ${String.format(java.util.Locale.UK, "%.1f", allEpisodesMB / 1000)} GB for ${episodes.size} episodes. It runs in the background, with progress and Pause in the notification.",
            "Keep all offline", wifi, danger = false,
        ) { bulk(Bulk.Kind.Episodes) })
    }

    private fun bulk(kind: Bulk.Kind) {
        if (bulk?.let { !it.paused } == true) return flash("Already keeping things offline. Pause or cancel it first")
        effectChannel.trySend(Effect.RequestNotifications)
        Bulk.start(getApplication(), kind, settings.hd, settings.wifiOnly)
    }

    fun pauseBulk() = Bulk.pause(getApplication())
    fun resumeBulk() = Bulk.resume(getApplication())
    fun cancelBulk() = Bulk.cancel(getApplication())

    private fun plural(n: Int, one: String, many: String) = "$n ${if (n == 1) one else many}"
    private fun mb(v: Double) = String.format(java.util.Locale.UK, "%.1f MB", v)

    fun askDeleteSketches() = ask(Confirm(
        "Delete offline sketches?",
        "This removes ${plural(library.offline.size, "sketch", "sketches")} (${mb(sketchMB)}). Your favourites stay.",
        "Delete sketches",
    ) { downloads.deleteAllImages(); updateLibrary { copy(offline = emptyList()) }; refreshSizes(); flash("Offline sketches deleted") })

    fun askDeleteEpisodes() = ask(Confirm(
        "Delete offline episodes?",
        "This removes ${plural(library.epOffline.size, "episode", "episodes")} (${mb(podMB)}). Your favourites stay.",
        "Delete episodes",
    ) { audio.player?.clearMediaItems(); downloads.deleteAllEpisodes(); updateLibrary { copy(epOffline = emptyList()) }; refreshSizes(); flash("Offline episodes deleted") })

    fun askDeleteAll() = ask(Confirm(
        "Delete everything offline?",
        "This removes ${plural(library.offline.size, "sketch", "sketches")} and ${plural(library.epOffline.size, "episode", "episodes")}, ${mb(sketchMB + podMB)} in all. Your favourites stay.",
        "Delete everything",
    ) {
        audio.player?.clearMediaItems()
        downloads.deleteAllImages(); downloads.deleteAllEpisodes()
        updateLibrary { copy(offline = emptyList(), epOffline = emptyList()) }
        refreshSizes()
        flash("Offline copies deleted")
    })

    fun tapVersion() {
        verTaps++
        if (verTaps >= 10) { verTaps = 0; egg = true; return }
        if (verTaps >= 6) flash("${10 - verTaps} more…")
    }

    // ---- outbound ------------------------------------------------------------------------

    fun sendContact(name: String, email: String, message: String) {
        if (message.isBlank()) return flash("Write a message first")
        effectChannel.trySend(Effect.Email(
            "hello@sketchplanations.com", "For the podcast" + if (name.isNotBlank()) " from $name" else "",
            message + if (email.isNotBlank()) "\n\nReply to: $email" else "",
        ))
    }

    fun subscribe(email: String) {
        val e = email.trim()
        openUrl("https://sketchplanations.substack.com/subscribe" + if (e.isNotEmpty()) "?email=" + java.net.URLEncoder.encode(e, "UTF-8") else "")
    }
}
