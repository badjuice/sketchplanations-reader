package com.sketchplanations.reader.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sketchplanations.reader.data.Catalogue
import com.sketchplanations.reader.data.Sketch
import com.sketchplanations.reader.state.AppModel
import com.sketchplanations.reader.state.Filter
import com.sketchplanations.reader.state.Sort
import com.sketchplanations.reader.state.SyncState
import com.sketchplanations.reader.ui.components.Chip
import com.sketchplanations.reader.ui.components.EmptyState
import com.sketchplanations.reader.ui.components.Hairline
import com.sketchplanations.reader.ui.components.InkCard
import com.sketchplanations.reader.ui.components.InkButton
import com.sketchplanations.reader.ui.components.LocalTablet
import com.sketchplanations.reader.ui.components.Lucide
import com.sketchplanations.reader.ui.components.SketchRow
import com.sketchplanations.reader.ui.components.T
import com.sketchplanations.reader.ui.theme.EaseOut
import com.sketchplanations.reader.ui.theme.Sk
import com.sketchplanations.reader.ui.theme.mono
import com.sketchplanations.reader.ui.theme.sans
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class ArcGroup(val key: String, val rows: List<Sketch>)

private sealed interface ArchiveItem {
    data class Header(val key: String) : ArchiveItem
    data class Rows(val group: String, val rows: List<Sketch>) : ArchiveItem
}

@Composable
fun ArchiveScreen(m: AppModel) {
    val tablet = LocalTablet.current
    val lib = m.library
    val base = when (m.filter) {
        Filter.Offline -> Catalogue.sketches.filter { it.n in lib.offline }
        Filter.Favourites -> Catalogue.sketches.filter { it.n in lib.favourites }
        Filter.All -> Catalogue.sketches
    }
    val flip = if (m.asc) -1 else 1
    val groups: List<ArcGroup> = if (m.sort == Sort.Az) {
        base.sortedWith { a, b -> a.sortKey.compareTo(b.sortKey, ignoreCase = true) * flip }
            .groupBy { it.sortKey.first().uppercaseChar().toString() }.map { ArcGroup(it.key, it.value) }
    } else {
        base.sortedWith { a, b -> b.raw.compareTo(a.raw) * flip }.groupBy { it.year }.map { ArcGroup(it.key, it.value) }
    }

    // The rail rekeys to the active sort, direction and filter.
    val present = groups.map { it.key }.toSet()
    val allKeys = if (m.sort == Sort.Az) ('A'..'Z').map { it.toString() }
    else {
        val years = Catalogue.sketches.map { it.raw.year }
        (years.max() downTo years.min()).map { it.toString() }
    }.let { if (m.asc) it.reversed() else it }
    val railOn = if (m.sort == Sort.Az) m.settings.railLetters else m.settings.railYears
    val rail = if (!railOn) emptyList() else (if (m.settings.railAll) allKeys else allKeys.filter { it in present })

    val items = buildList {
        groups.forEach { g ->
            add(ArchiveItem.Header(g.key))
            if (tablet) g.rows.chunked(2).forEach { add(ArchiveItem.Rows(g.key, it)) } else g.rows.forEach { add(ArchiveItem.Rows(g.key, listOf(it))) }
        }
    }
    val headerIndex = items.withIndex().filter { it.value is ArchiveItem.Header }.associate { (it.value as ArchiveItem.Header).key to it.index }

    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(m.sort, m.asc, m.filter) { state.scrollToItem(0) }
    val tick = remember { m.scrollTopTick }
    LaunchedEffect(m.scrollTopTick) { if (m.scrollTopTick != tick) state.animateScrollToItem(0) }

    Column(Modifier.fillMaxSize()) {
        if (!lib.indexBuilt) { IndexBanner(m); return@Column }

        val azOn = m.sort == Sort.Az
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("A–Z", { m.sort = Sort.Az }, on = azOn)
                Chip("Date", { m.sort = Sort.Date }, on = !azOn)
                Chip(
                    if (azOn) (if (m.asc) "A → Z" else "Z → A") else (if (m.asc) "Oldest first" else "Newest first"),
                    { m.asc = !m.asc }, hPad = 14.dp, leading = if (m.asc) Lucide.ArrowUp else Lucide.ArrowDown,
                )
            }
            T("${base.size} ${if (base.size == 1) "sketch" else "sketches"}", mono(13), color = Sk.colors.textTertiary)
        }
        Hairline()
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Filter.All to "All", Filter.Offline to "Downloaded", Filter.Favourites to "Favourites").forEach { (f, label) ->
                Chip(label, { m.filter = f }, on = m.filter == f, height = 34.dp)
            }
        }
        Hairline()

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (groups.isEmpty()) {
                EmptyState(
                    "Nothing here yet",
                    if (m.filter == Filter.Offline) "Nothing downloaded yet. Open a sketch and tap Download post to keep it here."
                    else "Nothing favourited yet. Tap Favourite on a sketch and it will show up here.",
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            } else {
                LazyColumn(
                    state = state,
                    contentPadding = PaddingValues(start = 16.dp, end = if (rail.isNotEmpty()) 36.dp else 16.dp, bottom = 26.dp),
                ) {
                    items(items, key = { if (it is ArchiveItem.Header) "h-${it.key}" else "r-${(it as ArchiveItem.Rows).rows.first().n}" }) { item ->
                        when (item) {
                            is ArchiveItem.Header -> Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                T(item.key, sans(24, 800, 1f))
                                Spacer(Modifier.width(12.dp))
                                Box(Modifier.weight(1f).height(1.dp).background(Sk.colors.rule))
                            }
                            is ArchiveItem.Rows -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                item.rows.forEach { s ->
                                    Box(Modifier.weight(1f)) { ArchiveRow(m, s) }
                                }
                                if (tablet && item.rows.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // When a rail is hidden the element must not render: an empty strip swallows taps on the row edge.
            if (rail.isNotEmpty()) {
                var active by remember { mutableIntStateOf(-1) }
                JumpRail(
                    rail, present, active,
                    onActive = { active = it },
                    onJump = { i, instant ->
                        val idx = headerIndex[rail[i]] ?: return@JumpRail
                        scope.launch { if (instant) state.scrollToItem(idx) else state.animateScrollToItem(idx) }
                    },
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
                if (active >= 0) {
                    // Readable past a thumb: the held key, centred on screen.
                    T(
                        rail[active], sans(46, 800, 1f).copy(textAlign = TextAlign.Center),
                        Modifier.align(Alignment.Center).widthIn(min = 104.dp).clip(RoundedCornerShape(24.dp)).background(Sk.colors.ink)
                            .border(2.dp, Sk.colors.paperTint, RoundedCornerShape(24.dp)).padding(horizontal = 24.dp, vertical = 20.dp),
                        Sk.colors.paperTint,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchiveRow(m: AppModel, s: Sketch) {
    SketchRow(
        s.title, "${s.num} · ${s.date}", { m.open(s.n) },
        offline = m.isOffline(s.n), gallery = m.inGallery(s.n), hd = m.settings.hd,
        showPill = true, titleSize = if (m.settings.bigText) 18 else 16,
    )
}

/**
 * Fast-scroll jump rail. Draggable with finger or mouse; the held key shifts 14px left and its
 * neighbours 8px and 3px. Jumps are instant while scrubbing, smooth on tap.
 */
@Composable
private fun JumpRail(keys: List<String>, present: Set<String>, active: Int, onActive: (Int) -> Unit, onJump: (Int, Boolean) -> Unit, modifier: Modifier) {
    var height by remember { mutableStateOf(1) }
    val jump by rememberUpdatedState(onJump)
    val act by rememberUpdatedState(onActive)
    val has by rememberUpdatedState(present)
    Box(modifier.fillMaxHeight().width(34.dp), contentAlignment = Alignment.CenterEnd) {
        Column(
            Modifier
                .width(34.dp)
                .padding(end = 2.dp)
                .onSizeChanged { height = it.height }
                .pointerInput(keys) {
                    fun at(y: Float) = (y / (height.toFloat() / keys.size)).toInt().coerceIn(0, keys.lastIndex)
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        var i = at(down.position.y)
                        var moved = false
                        act(i)
                        while (true) {
                            val ev = awaitPointerEvent()
                            val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                            if (!c.pressed) break
                            c.consume()
                            val j = at(c.position.y)
                            if (j != i) {
                                i = j; moved = true
                                act(j)
                                if (keys[j] in has) jump(j, true)
                            }
                        }
                        if (!moved && keys[i] in has) jump(i, false)
                        act(-1)
                    }
                },
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            keys.forEachIndexed { i, k ->
                val dist = if (active < 0) 99 else abs(i - active)
                val shift by animateFloatAsState(when (dist) { 0 -> -14f; 1 -> -8f; 2 -> -3f; else -> 0f }, tween(Sk.motion.fast, easing = EaseOut), label = "rail")
                val has = k in present
                T(
                    k, mono(11, if (dist == 0) 700 else 400).copy(textAlign = TextAlign.Center),
                    Modifier.width(30.dp).graphicsLayer { translationX = shift * density; alpha = if (has) (if (dist == 0) 1f else .9f) else .35f }.padding(vertical = 1.dp),
                    if (dist == 0 && has) Sk.colors.accentAlt else Sk.colors.textTertiary,
                )
            }
        }
    }
}

/** Archive needs the catalogue; if first-run indexing was skipped, it offers to build it here. */
@Composable
private fun IndexBanner(m: AppModel) {
    Column(Modifier.padding(16.dp)) {
        InkCard(Modifier.fillMaxWidth(), bg = Sk.colors.warningWash) {
            Column(Modifier.padding(18.dp)) {
                T("The index isn't built yet", sans(18, 800))
                Spacer(Modifier.height(8.dp))
                T("Archive and Topics need the list of every sketch before they can work. Titles and dates only — the drawings stay on the website until you ask for one.",
                    sans(15, 400, 1.5f), color = Sk.colors.textSecondary)
                Spacer(Modifier.height(14.dp))
                val running = m.syncState == SyncState.Running
                InkButton(
                    { if (!running) m.startSync() },
                    bg = if (running) Sk.colors.surfaceCard else Sk.colors.accentAlt,
                    progress = if (running) m.syncN / Catalogue.TOTAL_POSTS.toFloat() else null,
                ) { fg ->
                    if (running) T("${m.syncN} of ${Catalogue.TOTAL_POSTS}", mono(14, 700), color = Sk.colors.textPrimary)
                    else T(if (m.syncState == SyncState.Failed && !m.online) "No connection — try again" else "Build the index", sans(15, 700), color = fg)
                }
            }
        }
    }
}
