package org.rdcl.nox.sketchplanationsreader.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.rdcl.nox.sketchplanationsreader.data.db.SketchSummary as Sketch
import org.rdcl.nox.sketchplanationsreader.ui.date
import org.rdcl.nox.sketchplanationsreader.ui.image
import org.rdcl.nox.sketchplanationsreader.ui.num
import org.rdcl.nox.sketchplanationsreader.ui.code
import org.rdcl.nox.sketchplanationsreader.ui.lengthLabel
import androidx.compose.runtime.produceState
import org.rdcl.nox.sketchplanationsreader.state.AppModel
import org.rdcl.nox.sketchplanationsreader.state.SearchPhase
import org.rdcl.nox.sketchplanationsreader.state.Screen
import org.rdcl.nox.sketchplanationsreader.state.Sort
import org.rdcl.nox.sketchplanationsreader.ui.components.Chip
import org.rdcl.nox.sketchplanationsreader.ui.components.Columns
import org.rdcl.nox.sketchplanationsreader.ui.components.DeleteButton
import org.rdcl.nox.sketchplanationsreader.ui.components.EmptyState
import org.rdcl.nox.sketchplanationsreader.ui.components.Eyebrow
import org.rdcl.nox.sketchplanationsreader.ui.components.Hairline
import org.rdcl.nox.sketchplanationsreader.ui.components.Icon
import org.rdcl.nox.sketchplanationsreader.ui.components.InkButton
import org.rdcl.nox.sketchplanationsreader.ui.components.InkRule
import org.rdcl.nox.sketchplanationsreader.ui.components.Lucide
import org.rdcl.nox.sketchplanationsreader.ui.components.Pill
import org.rdcl.nox.sketchplanationsreader.ui.components.PrimaryButton
import org.rdcl.nox.sketchplanationsreader.ui.components.SketchRow
import org.rdcl.nox.sketchplanationsreader.ui.components.T
import org.rdcl.nox.sketchplanationsreader.ui.components.TextLink
import org.rdcl.nox.sketchplanationsreader.ui.components.hardShadow
import org.rdcl.nox.sketchplanationsreader.ui.components.noRippleClick
import org.rdcl.nox.sketchplanationsreader.ui.theme.Radius
import org.rdcl.nox.sketchplanationsreader.ui.theme.Shadow
import org.rdcl.nox.sketchplanationsreader.ui.theme.Sk
import org.rdcl.nox.sketchplanationsreader.ui.theme.hand
import org.rdcl.nox.sketchplanationsreader.ui.theme.heading
import org.rdcl.nox.sketchplanationsreader.ui.theme.mono
import org.rdcl.nox.sketchplanationsreader.ui.theme.sans

@Composable
private fun ResultRow(m: AppModel, s: Sketch, pill: Boolean = false) {
    SketchRow(
        s.title, "${s.num} · ${s.date}", { m.open(s.uid) }, image = s.image,
        offline = m.isOffline(s.uid), gallery = m.inGallery(s.uid), hd = m.settings.hd, showPill = pill, read = m.isRead(s.uid),
        titleSize = if (m.settings.bigText) 18 else 16,
    )
}

// ---- Search ---------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(m: AppModel) {
    val focus = remember { FocusRequester() }
    val fm = LocalFocusManager.current
    LaunchedEffect(Unit) { if (m.q.isEmpty()) runCatching { focus.requestFocus() } }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(Sk.colors.surfaceApp)) {
            val shape = Pill
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth().height(44.dp)
                    .hardShadow(Shadow.sm, Sk.colors.shadowInk, shape).clip(shape).background(Sk.colors.surfaceSunken)
                    .border(2.dp, Sk.colors.borderDefault, shape).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Lucide.Search, Sk.colors.textTertiary, 18.dp)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (m.q.isEmpty()) T("Search sketches, topics, words", sans(16), color = Sk.colors.textTertiary, maxLines = 1)
                    BasicTextField(
                        m.q, { m.runSearch(it) },
                        Modifier.fillMaxWidth().focusRequester(focus),
                        textStyle = sans(16).copy(color = Sk.colors.textPrimary),
                        singleLine = true,
                        cursorBrush = SolidColor(Sk.colors.accentAlt),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { fm.clearFocus() }),
                    )
                }
                if (m.q.isNotEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.size(28.dp).noRippleClick({ m.runSearch("") }), contentAlignment = Alignment.Center) { Icon(Lucide.Close, Sk.colors.textTertiary, 18.dp) }
                }
            }
            Hairline()
        }

        ScreenScroll(m, Modifier.weight(1f)) {
            when (m.phase) {
                SearchPhase.Idle -> Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 26.dp)) {
                    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        T("More ways to explore", sans(14), color = Sk.colors.textTertiary)
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TextLink("Topics", { m.openTopics() }, sans(16, 700))
                            T("·", sans(16, 700), color = Sk.colors.textTertiary)
                            TextLink("Archive", { m.go(Screen.Archive) }, sans(16, 700))
                            T("·", sans(16, 700), color = Sk.colors.textTertiary)
                            TextLink("A–Z", { m.go(Screen.Archive); m.sort = Sort.Az }, sans(16, 700))
                        }
                    }
                    if (m.settings.showRecents && m.library.recents.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            T("Recent searches", sans(16, 700), color = Sk.colors.textSecondary)
                            TextLink("Clear", { m.clearRecents() }, sans(14, 700))
                        }
                        FlowRow(Modifier.padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            m.library.recents.forEach { r ->
                                Chip(r, { m.runSearch(r) }, height = null, hPad = 14.dp, vPad = 8.dp, style = sans(15, 600))
                            }
                        }
                    }
                    T("Or search by", sans(16, 700), Modifier.padding(bottom = 10.dp), Sk.colors.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BigCard("Topic", Lucide.Chart, Modifier.weight(1f)) { m.openTopics() }
                        BigCard("Date or number", Lucide.Calendar, Modifier.weight(1f)) { m.go(Screen.Archive); m.sort = Sort.Date }
                    }
                }
                SearchPhase.Typing -> {}
                SearchPhase.Searching -> Row(Modifier.padding(horizontal = 16.dp, vertical = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spinner()
                    Spacer(Modifier.width(14.dp))
                    T(m.searchMsg, hand(20, 1.35f), color = Sk.colors.textSecondary)
                }
                SearchPhase.Done -> {
                    val results = m.results.mapNotNull { m.byUid[it] }
                    if (results.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            T("Sketches", sans(21, 800, 1.3f))
                            T("${results.size} ${if (results.size == 1) "result" else "results"}", sans(14), color = Sk.colors.textTertiary)
                        }
                        Hairline()
                        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 26.dp)) {
                            Columns(results) { ResultRow(m, it) }
                        }
                    } else {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 40.dp)) {
                            T("No sketches found", heading(26))
                            Spacer(Modifier.height(8.dp))
                            T("Nothing matches “${m.q}” yet.", sans(16, 400, 1.5f), color = Sk.colors.textSecondary)
                            Spacer(Modifier.height(22.dp))
                            PrimaryButton("Try a random sketch?", { m.shuffle() }, Modifier.fillMaxWidth(), icon = Lucide.Shuffle)
                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.noRippleClick({ m.openUrl("https://forms.gle/c6LymLW5vLx9FUeW6") }).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Lucide.External, Sk.colors.textLink, 15.dp, stroke = 2.2f)
                                Spacer(Modifier.width(8.dp))
                                T("Tell the author what's missing", sans(16, 700), color = Sk.colors.textLink)
                            }
                            Spacer(Modifier.height(2.dp))
                            T("Opens Jono's suggestion form on the web. This app only links to it.", sans(13, 400, 1.5f), Modifier.widthIn(max = 300.dp), Sk.colors.textTertiary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BigCard(label: String, icon: Lucide, modifier: Modifier, onClick: () -> Unit) {
    InkButton(onClick, modifier, bg = Sk.colors.surfaceCard, fg = Sk.colors.textPrimary, shape = RoundedCornerShape(Radius.card), shadow = Shadow.md, height = null, padding = 18.dp, arrangement = Arrangement.Start) { fg ->
        Column(Modifier.padding(vertical = 18.dp)) {
            Icon(icon, Sk.colors.accentAlt, 22.dp)
            Spacer(Modifier.height(10.dp))
            T(label, sans(16, 700), color = fg)
        }
    }
}

/** The site's two-arc spinner. */
@Composable
fun Spinner() {
    val t = rememberInfiniteTransition(label = "spin")
    val r by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart), label = "r")
    Box(Modifier.size(26.dp).graphicsLayer { rotationZ = r }) { Icon(Lucide.Spinner, Sk.colors.textPrimary, 26.dp) }
}

// ---- Topics -------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopicsContent(m: AppModel) {
    if (m.topics.isEmpty()) { NothingStored(m); return }
    val max = m.topics.maxOf { it.count }.coerceAtLeast(1).toFloat()
    ScreenScroll(m) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 28.dp)) {
            T("Every sketch is filed under a few topics. See how often each comes up, or run through them A–Z.", sans(16, 400, 1.5f), Modifier.padding(bottom = 18.dp), Sk.colors.textSecondary)
            // A switch, never stacked; always defaults to frequency on entry.
            Row(Modifier.padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("By frequency", { m.catByFreq = true }, Modifier.weight(1f), on = m.catByFreq, height = 38.dp)
                Chip("All keywords", { m.catByFreq = false }, Modifier.weight(1f), on = !m.catByFreq, height = 38.dp)
            }
            if (m.catByFreq) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    m.topics.filter { it.count > 0 }.forEach { c ->
                        Column(Modifier.fillMaxWidth().noRippleClick({ m.openTag(c.uid) })) {
                            Row(Modifier.fillMaxWidth().padding(bottom = 5.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                T(c.name, sans(15, 600))
                                T(c.count.toString(), mono(13), color = Sk.colors.textTertiary)
                            }
                            Box(Modifier.fillMaxWidth().height(12.dp).clip(Pill).background(Sk.colors.surfaceSunken).border(1.dp, Sk.colors.borderQuiet, Pill)) {
                                Box(Modifier.fillMaxWidth(c.count / max).fillMaxHeight().background(Sk.colors.penBlue))
                            }
                        }
                    }
                }
            } else {
                m.topics.filter { it.count > 0 }.sortedBy { it.name.lowercase() }.groupBy { it.name.first().uppercaseChar() }.forEach { (k, rows) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        T(k.toString(), sans(17, 800), Modifier.width(20.dp))
                        Spacer(Modifier.width(14.dp))
                        FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            rows.forEach { c -> Chip(c.name, { m.openTag(c.uid) }, height = null, hPad = 12.dp, vPad = 6.dp, count = c.count.toString(), countStyle = mono(12)) }
                        }
                    }
                    Hairline()
                }
            }
        }
    }
}

// ---- Tag ----------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagScreen(m: AppModel) {
    val rows by produceState(emptyList<Sketch>(), m.tag, m.contentVersion) {
        value = m.repo.sketchUidsForTopic(m.tag).mapNotNull { m.byUid[it] }
    }
    ScreenScroll(m) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 12.dp)) {
            Eyebrow("Topic")
            Spacer(Modifier.height(6.dp))
            T(m.topicName(m.tag), heading(30, 1.2f))
            Spacer(Modifier.height(4.dp))
            T("${rows.size} ${if (rows.size == 1) "sketch" else "sketches"}", mono(13), color = Sk.colors.textTertiary)
        }
        Hairline()
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp)) {
            if (rows.isEmpty()) T("No stored sketch is filed under this topic yet.", sans(16, 400, 1.5f), Modifier.padding(vertical = 24.dp), Sk.colors.textSecondary)
            Columns(rows) { ResultRow(m, it, pill = true) }
        }
        InkRule()
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 28.dp)) {
            T("Other topics", sans(16, 700), Modifier.padding(bottom = 12.dp), Sk.colors.textSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                m.topics.filter { it.uid != m.tag && it.count > 0 }.take(8).forEach { c ->
                    Chip(c.name, { m.openTag(c.uid) }, height = null, hPad = 12.dp, vPad = 5.dp, style = sans(13, 600), count = c.count.toString(), countStyle = mono(11))
                }
            }
        }
    }
}

// ---- Library ------------------------------------------------------------------------------

/**
 * Favourites are bookmarks; Offline is what's stored on the phone. Each tab lists sketches first,
 * then episodes.
 */
@Composable
fun LibraryScreen(m: AppModel) {
    val lib = m.library
    val fav = m.libFavTab
    val sketches = (if (fav) lib.favourites else lib.offline).mapNotNull { m.byUid[it] }
    val episodes = (if (fav) lib.epFav else lib.epOffline).mapNotNull { g -> m.episodes.firstOrNull { it.guid == g } }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Favourites ${lib.favourites.size + lib.epFav.size}", { m.libFavTab = true }, on = fav)
            Chip("Offline ${lib.offline.size + lib.epOffline.size}", { m.libFavTab = false }, on = !fav)
        }
        Hairline()
        ScreenScroll(m, Modifier.weight(1f)) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 26.dp)) {
                if (!fav && (sketches.isNotEmpty() || episodes.isNotEmpty())) {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        T(String.format(java.util.Locale.UK, "%.0f MB on this phone", m.sketchMB + m.podMB), hand(18), color = Sk.colors.textSecondary)
                        TextLink("Storage", { m.go(Screen.Settings) }, sans(14, 700))
                    }
                }
                if (sketches.isNotEmpty()) {
                    Eyebrow("Sketches · ${sketches.size}", Modifier.padding(top = 14.dp, bottom = 2.dp))
                    Columns(sketches) { s ->
                        SketchRow(
                            s.title, "${s.num} · ${s.date}", { m.open(s.uid) }, image = s.image,
                            offline = m.isOffline(s.uid), gallery = m.inGallery(s.uid), hd = m.settings.hd,
                            titleSize = if (m.settings.bigText) 18 else 16, thumbW = 72.dp, thumbH = 58.dp,
                            trailing = {
                                DeleteButton(if (fav) "Remove from favourites" else "Delete offline copy") {
                                    if (fav) m.removeFavourite(s.uid) else m.removeOffline(s.uid)
                                }
                            },
                        )
                    }
                }
                if (episodes.isNotEmpty()) {
                    Eyebrow("Episodes · ${episodes.size}", Modifier.padding(top = 18.dp, bottom = 2.dp))
                    Columns(episodes) { e ->
                        EpisodeRow(
                            e, listOf(e.code, e.lengthLabel).filter { it.isNotEmpty() }.joinToString(" · "),
                            { m.openEp(m.episodes.indexOf(e)) },
                        ) {
                            DeleteButton(if (fav) "Remove from favourites" else "Delete offline copy") {
                                if (fav) m.removeEpFav(e.guid) else m.removeEpOffline(e.guid)
                            }
                        }
                    }
                }
                if (sketches.isEmpty() && episodes.isEmpty()) {
                    EmptyState(
                        "Nothing here yet",
                        if (fav) "Tap Favourite on a sketch or an episode to bookmark it here. Nothing is stored on the phone."
                        else "Tap Keep offline on a sketch or an episode and it opens here with no signal.",
                        "Browse the archive", { m.go(Screen.Archive) },
                    )
                }
            }
        }
    }
}
