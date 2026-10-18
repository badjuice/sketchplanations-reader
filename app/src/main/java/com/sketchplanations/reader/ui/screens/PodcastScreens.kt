package com.sketchplanations.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sketchplanations.reader.data.Catalogue
import com.sketchplanations.reader.data.Episode
import com.sketchplanations.reader.state.AppModel
import com.sketchplanations.reader.state.Screen
import com.sketchplanations.reader.ui.clock
import com.sketchplanations.reader.ui.components.ArtBlock
import com.sketchplanations.reader.ui.components.CardPill
import com.sketchplanations.reader.ui.components.Chip
import com.sketchplanations.reader.ui.components.Columns
import com.sketchplanations.reader.ui.components.DeleteButton
import com.sketchplanations.reader.ui.components.EmptyState
import com.sketchplanations.reader.ui.components.Hairline
import com.sketchplanations.reader.ui.components.Highlighter
import com.sketchplanations.reader.ui.components.Icon
import com.sketchplanations.reader.ui.components.InkButton
import com.sketchplanations.reader.ui.components.InkCard
import com.sketchplanations.reader.ui.components.InkRule
import com.sketchplanations.reader.ui.components.LocalTablet
import com.sketchplanations.reader.ui.components.Lucide
import com.sketchplanations.reader.ui.components.Pill
import com.sketchplanations.reader.ui.components.PressRow
import com.sketchplanations.reader.ui.components.PrimaryButton
import com.sketchplanations.reader.ui.components.T
import com.sketchplanations.reader.ui.components.hardShadow
import com.sketchplanations.reader.ui.components.noRippleClick
import com.sketchplanations.reader.ui.components.readMeasure
import com.sketchplanations.reader.ui.components.tapOrHold
import com.sketchplanations.reader.ui.theme.Art
import com.sketchplanations.reader.ui.theme.Shadow
import com.sketchplanations.reader.ui.theme.Sk
import com.sketchplanations.reader.ui.theme.hand
import com.sketchplanations.reader.ui.theme.heading
import com.sketchplanations.reader.ui.theme.mono
import com.sketchplanations.reader.ui.theme.sans

@Composable
private fun EpisodeSquare(num: Int, size: Dp = 54.dp) {
    ArtBlock(Modifier.size(size)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { T(num.toString(), mono(14, 700), color = Art.caption) }
    }
}

// ---- Podcast home -------------------------------------------------------------------------

@Composable
fun PodcastScreen(m: AppModel) {
    val latest = Catalogue.episodes[0]
    ScreenScroll(m) {
        Column(Modifier.fillMaxWidth().background(Sk.colors.surfaceSunken).padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 24.dp)) {
            Highlighter("latest episode 🎧")
            Spacer(Modifier.height(14.dp))
            Row(Modifier.padding(bottom = 18.dp)) {
                ArtBlock(Modifier.size(96.dp), radius = 12.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    T(latest.title, heading(23, 1.2f))
                    Spacer(Modifier.height(6.dp))
                    T("EP ${latest.num} · ${latest.len} · ${latest.date}", mono(13), color = Sk.colors.textTertiary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Play", { m.playLatest() }, Modifier.weight(1f), icon = Lucide.Play)
                OutlinePill("Library", Lucide.Library) { m.go(Screen.PodLibrary) }
            }
        }
        InkRule()
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            T("All episodes", sans(21, 800, 1.3f))
            T(Catalogue.episodes.size.toString(), mono(13), color = Sk.colors.textTertiary)
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            Columns(Catalogue.episodes.withIndex().toList()) { (i, e) ->
                Column {
                    PressRow({ m.openEp(i) }, Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(horizontal = 6.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            EpisodeSquare(e.num)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                T(e.title, sans(16, 700, 1.3f))
                                Spacer(Modifier.height(4.dp))
                                T("${e.len} · ${e.date}", mono(12), color = Sk.colors.textTertiary)
                            }
                            Box(
                                Modifier.size(38.dp).hardShadow(Shadow.sm, Sk.colors.shadowInk, CircleShape).clip(CircleShape)
                                    .background(Sk.colors.surfaceApp).border(2.dp, Sk.colors.borderDefault, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Lucide.Play, Sk.colors.accentAlt, 13.dp) }
                        }
                    }
                    Hairline()
                }
            }
        }
        InkRule()
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 26.dp)) {
            CardPill("About the podcast", { m.goDeep(Screen.PodcastAbout) })
        }
    }
}

@Composable
private fun OutlinePill(text: String, icon: Lucide, onClick: () -> Unit) {
    Chip(text, onClick, height = 44.dp, hPad = 20.dp, style = sans(16, 700), leading = icon)
}

// ---- Episode ------------------------------------------------------------------------------

@Composable
fun EpisodeScreen(m: AppModel) {
    val e = m.episode
    val i = m.ep
    val tablet = LocalTablet.current
    val off = m.isEpOffline(e.num)
    val fav = m.isEpFav(e.num)
    val pos = m.posFor(i)
    val playingThis = m.playing && m.audioEp == i
    val cur = e.seconds * pos
    ScreenScroll(m, align = Alignment.CenterHorizontally) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 18.dp)) {
                InkCard(Modifier.size(108.dp), bg = Art.paper, shadow = Shadow.md, radius = 12.dp) {
                    Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.BottomStart) { T("art 1:1 ✎", hand(15), color = Art.caption) }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    T("EP ${e.num} · ${e.date}", mono(12), color = Sk.colors.textTertiary)
                    Spacer(Modifier.height(6.dp))
                    T(e.title, heading(25, 1.2f))
                    Spacer(Modifier.height(6.dp))
                    T(e.len, sans(14), color = Sk.colors.textSecondary)
                }
            }

            InkCard(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp).fillMaxWidth(), shadow = Shadow.md) {
                Column(Modifier.padding(16.dp)) {
                    Waveform(pos) { m.seek(i, it) }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        T(clock(cur.toInt()), mono(13), color = Sk.colors.textTertiary)
                        T("-" + clock((e.seconds - cur).toInt()), mono(13), color = Sk.colors.textTertiary)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SkipButton("${m.settings.skipBack}s", "Back ${m.settings.skipBack} seconds") { m.skip(i, -m.settings.skipBack) }
                        InkButton({ m.togglePlay(i) }, Modifier.weight(1f)) { fg ->
                            Icon(if (playingThis) Lucide.Pause else Lucide.Play, fg, 16.dp)
                            Spacer(Modifier.width(8.dp))
                            T(if (playingThis) "Pause" else "Play", sans(16, 700), color = fg)
                        }
                        SkipButton("${m.settings.skipFwd}s", "Forward ${m.settings.skipFwd} seconds") { m.skip(i, m.settings.skipFwd) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Hairline()
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Chip(speedLabel(m.settings.speed), { m.cycleSpeed() }, height = 34.dp, hPad = 14.dp, style = mono(13, 700))
                        if (m.settings.sleep > 0) {
                            val left = m.sleepEndsAt?.let { ((it - System.currentTimeMillis()) / 60_000L + 1).toInt() } ?: m.settings.sleep
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Lucide.Clock, Sk.colors.accentAlt, 14.dp)
                                Spacer(Modifier.width(6.dp))
                                T("Sleep in ${if (left >= 60) "1h" else "${left}m"}", sans(13, 600), color = Sk.colors.accentAlt)
                            }
                        }
                    }
                }
            }

            Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton(if (fav) "Favourited" else "Favourite", if (fav) Lucide.StarFilled else Lucide.Star, fav, Modifier.weight(1f)) { m.toggleEpFav(e.num) }
                val running = m.epDl == e.num
                ActionButton(
                    m.pulseFor("epdl") ?: if (off) "Downloaded" else "Download", if (off) Lucide.Check else Lucide.Download, off,
                    Modifier.weight(1f), progress = if (running) m.epPct / 100f else null,
                ) { m.toggleEpDownload(e.num) }
            }

            if (!off && !m.online) {
                Row(
                    Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Sk.colors.warningWash).border(2.dp, Sk.colors.borderDefault, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Lucide.WifiOffSmall, Sk.colors.penOrangeInk, 18.dp)
                    Spacer(Modifier.width(10.dp))
                    T("Offline — download this episode to listen", sans(14, 600, 1.4f))
                }
            }

            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp)) {
                T("Chapters", sans(16, 700), Modifier.padding(bottom = 10.dp), Sk.colors.textSecondary)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    e.chapters.forEachIndexed { ci, c ->
                        val next = e.chapters.getOrNull(ci + 1)?.at ?: e.seconds
                        val on = cur >= c.at && cur < next
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (on) Sk.colors.accentAltWash else Color.Transparent)
                                .noRippleClick({ m.seek(i, c.at.toFloat() / e.seconds) }).padding(horizontal = 12.dp, vertical = 9.dp),
                        ) {
                            T(clock(c.at), mono(13), Modifier.width(56.dp), Sk.colors.textTertiary)
                            T(c.label, sans(15, if (on) 700 else 600, 1.35f), Modifier.weight(1f), if (on) Sk.colors.accentAlt else Sk.colors.textPrimary)
                        }
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().readMeasure(tablet).padding(horizontal = 16.dp)) {
            T("Show notes", sans(16, 700), Modifier.padding(bottom = 10.dp), Sk.colors.textSecondary)
            e.notes.forEach {
                T(it, sans(16, 400, 1.6f))
                Spacer(Modifier.height(14.dp))
            }
            val match = Catalogue.sketches.firstOrNull { it.title == e.title }
            if (match != null) {
                Spacer(Modifier.height(8.dp))
                InkButton({ m.open(match.n) }, Modifier.fillMaxWidth(), bg = Sk.colors.surfaceCard, fg = Sk.colors.textPrimary,
                    shape = RoundedCornerShape(18.dp), shadow = Shadow.md, height = null, padding = 12.dp, arrangement = Arrangement.Start) { fg ->
                    Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ArtBlock(Modifier.size(64.dp, 52.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            T("the sketch ✎", hand(16), color = Sk.colors.textTertiary)
                            Spacer(Modifier.height(2.dp))
                            T(e.title, sans(16, 700, 1.3f), color = fg)
                        }
                        Icon(Lucide.Chevron, Sk.colors.textTertiary, 20.dp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

fun speedLabel(s: Float) = (if (s == s.toInt().toFloat()) s.toInt().toString() else s.toString()) + "×"

@Composable
private fun SkipButton(text: String, label: String, onClick: () -> Unit) {
    var held by remember { mutableStateOf(false) }
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(if (held) Sk.colors.accentAltWash else Color.Transparent)
            .tapOrHold(label, onPress = { held = it }, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { T(text, mono(12, 700), color = Sk.colors.textSecondary) }
}

/** 46-bar waveform. Click or drag to seek. */
@Composable
private fun Waveform(pos: Float, onSeek: (Float) -> Unit) {
    var w by remember { mutableStateOf(1) }
    Row(
        Modifier.fillMaxWidth().height(36.dp).onSizeChanged { w = it.width }
            .pointerInput(Unit) { detectTapGestures { onSeek(it.x / w) } }
            .pointerInput(Unit) { detectHorizontalDragGestures { c, _ -> onSeek(c.position.x / w) } },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until 46) {
            val h = 8 + ((i * 37) % 26)
            Box(Modifier.weight(1f).height(h.dp).clip(Pill).background(if (i / 46f < pos) Sk.colors.accentAlt else Sk.colors.rule))
        }
    }
}

// ---- Podcast library ----------------------------------------------------------------------

@Composable
fun PodLibraryScreen(m: AppModel) {
    val lib = m.library
    val ids = if (m.podFavTab) lib.epFav else lib.epOffline
    val rows: List<Episode> = ids.mapNotNull { n -> Catalogue.episodes.firstOrNull { it.num == n } }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp)) {
            T(String.format(java.util.Locale.UK, "%.1f MB of episodes on this device ✎", m.podMB), hand(18), color = Sk.colors.textSecondary)
        }
        InkRule()
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Favourites ${lib.epFav.size}", { m.podFavTab = true }, on = m.podFavTab)
            Chip("Downloaded ${lib.epOffline.size}", { m.podFavTab = false }, on = !m.podFavTab)
        }
        Hairline()
        ScreenScroll(m, Modifier.weight(1f)) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 26.dp)) {
                Columns(rows) { e ->
                    Column {
                        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            PressRow({ m.openEp(Catalogue.episodeIndex(e.num)) }, Modifier.weight(1f)) {
                                Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    EpisodeSquare(e.num)
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        T(e.title, sans(16, 700, 1.3f))
                                        Spacer(Modifier.height(4.dp))
                                        T("${e.len} · ${if (m.podFavTab) e.date else "31.5 MB"}", mono(12), color = Sk.colors.textTertiary)
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            DeleteButton(if (m.podFavTab) "Remove from favourites" else "Delete episode") {
                                if (m.podFavTab) m.removeEpFav(e.num) else m.removeEpOffline(e.num)
                            }
                        }
                        Hairline()
                    }
                }
                if (rows.isEmpty()) {
                    EmptyState(
                        "Nothing here yet",
                        if (m.podFavTab) "No favourite episodes yet. Tap Favourite on an episode to keep it here."
                        else "Nothing downloaded. Tap Download on an episode to listen without signal.",
                        "Browse episodes", { m.go(Screen.Podcast) },
                    )
                }
            }
        }
    }
}

// ---- Podcast about + follow ---------------------------------------------------------------

private val players = listOf(
    "Spotify" to "https://open.spotify.com", "Apple Podcasts" to "https://podcasts.apple.com",
    "Overcast" to "https://overcast.fm", "RSS feed" to "https://podcast.sketchplanations.com",
)

@Composable
fun PodcastAboutScreen(m: AppModel) {
    val tablet = LocalTablet.current
    ScreenScroll(m, align = Alignment.CenterHorizontally) {
        Column(Modifier.fillMaxWidth().readMeasure(tablet).padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 28.dp)) {
            T("Three friends take one sketch a week and talk it all the way through — where the idea comes from, where it breaks, and where it shows up in ordinary life.", sans(17, 400, 1.5f))
            Spacer(Modifier.height(20.dp))
            InkRule()
            Spacer(Modifier.height(18.dp))
            // Credits live once, in About. This card deep-links there rather than repeating them.
            InkCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    T("Who makes it", sans(15, 700))
                    Spacer(Modifier.height(6.dp))
                    T("Jono Hey, Rob Bell and Tom Pellereau — credited in full, with what each of them brings, under About.", sans(14, 400, 1.5f), color = Sk.colors.textSecondary)
                    Spacer(Modifier.height(12.dp))
                    Chip("The hosts, in About", { m.openAboutHosts() }, height = 38.dp, leading = null)
                }
            }
            T("Listen in full", sans(16, 700), Modifier.padding(top = 24.dp, bottom = 4.dp), Sk.colors.textSecondary)
            T("The whole back catalogue, outside this app. YouTube carries every episode with the sketch on screen.", sans(13, 400, 1.5f), Modifier.padding(bottom = 12.dp), Sk.colors.textTertiary)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CardPill("Watch on YouTube", { m.openUrl("https://www.youtube.com/playlist?list=PLnLqYvj919Xt_XhzOyccZ5WFCTaz_Q0Vq") }, trailing = Lucide.External, size = 15, trailingSize = 14.dp)
                CardPill("Listen on Acast", { m.openUrl("https://shows.acast.com/sketchplanations") }, trailing = Lucide.External, size = 15, trailingSize = 14.dp)
            }
            Spacer(Modifier.height(22.dp))
            InkRule()
            T("Follow the show", sans(16, 700), Modifier.padding(top = 20.dp, bottom = 12.dp), Sk.colors.textSecondary)
            players.chunked(2).forEach { pair ->
                Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { (name, url) -> CardPill(name, { m.openUrl(url) }, Modifier.weight(1f), trailing = Lucide.External, size = 14, trailingSize = 14.dp) }
                }
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Contact the show", { m.goDeep(Screen.PodcastContact) }, Modifier.fillMaxWidth(), trailing = Lucide.ArrowRight)
        }
    }
}

// ---- Podcast contact ----------------------------------------------------------------------

@Composable
fun PodcastContactScreen(m: AppModel) {
    val tablet = LocalTablet.current
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    ScreenScroll(m, align = Alignment.CenterHorizontally) {
        Column(Modifier.fillMaxWidth().readMeasure(tablet).padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 28.dp)) {
            T("Ideas, corrections, a sketch you want talked about — send it over.", sans(16, 400, 1.5f), Modifier.padding(bottom = 22.dp), Sk.colors.textSecondary)
            Field("Your name", name, { name = it }, "Name")
            Field("Email", email, { email = it }, "you@example.com", KeyboardType.Email)
            Field("Message", message, { message = it }, "Say hello", minHeight = 130.dp, single = false)
            Spacer(Modifier.height(4.dp))
            PrimaryButton("Send message", { m.sendContact(name, email, message) }, Modifier.fillMaxWidth())
            T("Opens your email app with this filled in — nothing is sent from here.", sans(13, 400, 1.5f), Modifier.padding(top = 10.dp), Sk.colors.textTertiary)
        }
    }
}

/** 12px-radius sunken input with the 2px ink edge. */
@Composable
fun Field(
    label: String?, value: String, onChange: (String) -> Unit, placeholder: String,
    type: KeyboardType = KeyboardType.Text, minHeight: Dp = 44.dp, single: Boolean = true,
) {
    Column(Modifier.padding(bottom = if (label != null) 16.dp else 0.dp)) {
        if (label != null) T(label, sans(14, 600), Modifier.padding(bottom = 6.dp), Sk.colors.textSecondary)
        val shape = RoundedCornerShape(12.dp)
        Box(
            Modifier.fillMaxWidth().heightIn(min = minHeight).clip(shape).background(Sk.colors.surfaceSunken)
                .border(2.dp, Sk.colors.borderDefault, shape).padding(horizontal = 16.dp, vertical = if (single) 10.dp else 12.dp),
            contentAlignment = if (single) Alignment.CenterStart else Alignment.TopStart,
        ) {
            if (value.isEmpty()) T(placeholder, sans(16), color = Sk.colors.textTertiary)
            BasicTextField(
                value, onChange, Modifier.fillMaxWidth(),
                textStyle = sans(16, 400, 1.4f).copy(color = Sk.colors.textPrimary),
                singleLine = single, cursorBrush = SolidColor(Sk.colors.accentAlt),
                keyboardOptions = KeyboardOptions(keyboardType = type),
            )
        }
    }
}

// ---- Browse -------------------------------------------------------------------------------

/** Sequential reading, modelled on the xkcd Android reader. #1 is the oldest. */
@Composable
fun BrowseScreen(m: AppModel) {
    val tablet = LocalTablet.current
    val list = Catalogue.byDate.reversed()
    val i = m.browseIdx.coerceIn(0, list.lastIndex)
    val cur = list[i]
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).padding(start = 16.dp, end = 16.dp, top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                T("${cur.num} · ${cur.date}", mono(13), color = Sk.colors.textTertiary)
                T("${i + 1} of ${list.size}", mono(13), color = Sk.colors.textTertiary)
            }
            InkCard(Modifier.weight(1f).heightIn(min = 220.dp).fillMaxWidth(), bg = Art.paper, shadow = Shadow.md, shadowColor = Sk.colors.shadowInk) {
                Box(Modifier.fillMaxSize().noRippleClick({ m.open(cur.n) }), contentAlignment = Alignment.BottomStart) {
                    T("sketch ${cur.num} ✎", hand(17), Modifier.padding(14.dp), Art.caption)
                    if (m.needsNet(cur.n)) {
                        Column(Modifier.fillMaxSize().background(Art.paper).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Lucide.WifiOff, Art.caption, 26.dp)
                            Spacer(Modifier.height(10.dp))
                            T("Not downloaded — needs a connection", sans(15, 700), color = Art.caption)
                        }
                    } else if (m.imageHeld(cur.n)) {
                        Column(Modifier.fillMaxSize().background(Art.paper).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            T("held back — you're on mobile data ✎", hand(17), color = Art.caption)
                            Spacer(Modifier.height(12.dp))
                            InkButton({ m.reveal(cur.n) }, height = 40.dp, padding = 18.dp, border = Art.line, shadowColor = Art.line) { fg ->
                                Icon(Lucide.Download, fg, 15.dp, stroke = 2.2f); Spacer(Modifier.width(8.dp)); T("Load image", sans(14, 700), color = fg)
                            }
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().readMeasure(tablet).padding(top = 14.dp, bottom = 4.dp)) {
                T(cur.title, heading(22))
                Spacer(Modifier.height(4.dp))
                T(cur.lead, sans(15, 400, 1.5f), color = Sk.colors.textSecondary, maxLines = 3)
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxWidth().background(Sk.colors.surfaceChrome)) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(Sk.colors.ink))
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                NavSlot("« first", "First sketch", i > 0, Modifier.weight(1f)) { m.browseIdx = 0 }
                NavSlot("‹ prev", "Previous", i > 0, Modifier.weight(1f)) { m.browseIdx = (i - 1).coerceAtLeast(0) }
                InkButton({ m.browseIdx = list.indices.random() }, Modifier.weight(1f), height = 48.dp, padding = 0.dp, label = "Random") { fg ->
                    T("rand", sans(14, 700), color = fg)
                }
                NavSlot("next ›", "Next", i < list.lastIndex, Modifier.weight(1f)) { m.browseIdx = (i + 1).coerceAtMost(list.lastIndex) }
                NavSlot("last »", "Latest", i < list.lastIndex, Modifier.weight(1f)) { m.browseIdx = list.lastIndex }
            }
        }
    }
}

/** Ends disable and drop to 40% opacity rather than disappearing — a disabled slot keeps its box. */
@Composable
private fun NavSlot(text: String, label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    InkButton(
        onClick, modifier.alpha(if (on) 1f else .4f),
        bg = if (on) Sk.colors.surfaceCard else Color.Transparent,
        fg = if (on) Sk.colors.textPrimary else Sk.colors.textTertiary,
        border = if (on) Sk.colors.borderDefault else Sk.colors.borderQuiet,
        shadow = if (on) Shadow.sm else 0.dp, height = 48.dp, padding = 0.dp, enabled = on, label = label,
    ) { fg -> T(text, sans(14, 700), color = fg, maxLines = 1) }
}
