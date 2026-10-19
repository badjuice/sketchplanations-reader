package org.rdcl.nox.sketchplanationsreader.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.rdcl.nox.sketchplanationsreader.data.AppSettings
import org.rdcl.nox.sketchplanationsreader.data.HomeLayout
import org.rdcl.nox.sketchplanationsreader.data.LinkStyle
import org.rdcl.nox.sketchplanationsreader.state.AppModel
import org.rdcl.nox.sketchplanationsreader.state.Options
import org.rdcl.nox.sketchplanationsreader.ui.components.Chip
import org.rdcl.nox.sketchplanationsreader.ui.components.Hairline
import org.rdcl.nox.sketchplanationsreader.ui.components.Icon
import org.rdcl.nox.sketchplanationsreader.ui.components.InkButton
import org.rdcl.nox.sketchplanationsreader.ui.components.InkCard
import org.rdcl.nox.sketchplanationsreader.ui.components.Lucide
import org.rdcl.nox.sketchplanationsreader.ui.components.T
import org.rdcl.nox.sketchplanationsreader.ui.components.Toggle
import org.rdcl.nox.sketchplanationsreader.ui.components.noRippleClick
import org.rdcl.nox.sketchplanationsreader.ui.theme.Sk
import org.rdcl.nox.sketchplanationsreader.ui.theme.mono
import org.rdcl.nox.sketchplanationsreader.ui.theme.sans

internal sealed interface Row_ {
    val label: String
    val hint: String?
}

internal class ToggleRow(override val label: String, override val hint: String?, val on: Boolean, val flip: () -> Unit) : Row_
internal class ValueRow(override val label: String, override val hint: String?, val value: String, val onTap: (() -> Unit)? = null) : Row_
internal class ActionRow(override val label: String, override val hint: String?, val cta: String, val act: () -> Unit) : Row_
internal class SegRow<T>(override val label: String, override val hint: String?, val opts: List<Pair<T, String>>, val cur: T, val pick: (T) -> Unit) : Row_

val Options.title get() = when (this) {
    Options.Today -> "Today"
    Options.Reading -> "Reading"
    Options.Archive -> "Archive"
    Options.Search -> "Search"
    Options.Podcast -> "Podcast"
}

val Options.icon get() = when (this) {
    Options.Today -> Lucide.Today
    Options.Reading -> Lucide.Link
    Options.Archive -> Lucide.Archive
    Options.Search -> Lucide.Search
    Options.Podcast -> Lucide.Mic
}

/** The settings that belong to one screen. Settings lists the same rows under the screen's name. */
internal fun optionRows(m: AppModel, o: Options): List<Row_> {
    val s = m.settings
    fun set(f: AppSettings.() -> AppSettings) = m.updateSettings(f)
    return when (o) {
        Options.Today -> listOf(
            SegRow("Layout", "A swipe deck one card at a time, or a scrolling feed", listOf(HomeLayout.Deck to "Swipe deck", HomeLayout.Feed to "Feed"), s.homeLayout) { v -> set { copy(homeLayout = v) } },
        )
        Options.Reading -> listOf(
            ToggleRow("Larger reading text", "Lifts sketch prose and list titles; menus stay put", s.bigText) { set { copy(bigText = !bigText) } },
            SegRow("Links in the text", "How a link looks inside a sketch’s words", listOf(LinkStyle.Badge to "Badges", LinkStyle.Underline to "Underlined"), s.linkStyle) { v -> set { copy(linkStyle = v) } },
            SegRow("Open external links in", "Sketch notes link out to books, papers and other sites", listOf(false to "In-app", true to "Browser"), s.extBrowser) { v -> set { copy(extBrowser = v) } },
        )
        Options.Archive -> listOf(
            SegRow("Thumbnails", "Whole keeps the Sketchplanations mark in view", listOf(false to "Cropped", true to "Whole sketch"), s.thumbWhole) { v -> set { copy(thumbWhole = v) } },
            ToggleRow("Letter jump rail", "The A–Z scrubber when sorting alphabetically", s.railLetters) { set { copy(railLetters = !railLetters) } },
            ToggleRow("Year jump rail", "The scrubber when sorting by date", s.railYears) { set { copy(railYears = !railYears) } },
            ToggleRow("Show every letter", "Keeps the rail complete, with empty letters greyed out", s.railAll) { set { copy(railAll = !railAll) } },
        )
        Options.Search -> listOf(
            ToggleRow("Save recent searches", "Kept on this device only", s.showRecents) { set { copy(showRecents = !showRecents) } },
        )
        Options.Podcast -> listOf(
            SegRow("Playback speed", "Applies to every episode", listOf(1f to "1×", 1.25f to "1.25×", 1.5f to "1.5×", 2f to "2×"), s.speed) { v -> set { copy(speed = v) } },
            SegRow("Skip back", "The left button on the player", listOf(10 to "10s", 15 to "15s", 30 to "30s"), s.skipBack) { v -> set { copy(skipBack = v) } },
            SegRow("Skip forward", "The right button on the player", listOf(15 to "15s", 30 to "30s", 45 to "45s"), s.skipFwd) { v -> set { copy(skipFwd = v) } },
            SegRow("Sleep timer", "Fades out and pauses", listOf(0 to "Off", 15 to "15m", 30 to "30m", 60 to "1h"), s.sleep) { v -> m.setSleep(v) },
            ToggleRow("Play the next episode", "Rolls straight on when one finishes", s.continuous) { set { copy(continuous = !continuous) } },
            ToggleRow("Delete after listening", "Frees the space once you reach the end", s.autoDelete) { set { copy(autoDelete = !autoDelete) } },
            ToggleRow("Play in the background", "Keeps playing with the screen off and shows controls on the lock screen", s.bgPlay) { set { copy(bgPlay = !bgPlay) } },
            ToggleRow("Pause for calls", "Stops on an incoming call and resumes after", s.pauseOnCall) { set { copy(pauseOnCall = !pauseOnCall) } },
            ToggleRow("Stop when leaving the episode", "Off by default, so audio follows you around the app", s.stopOnLeave) { set { copy(stopOnLeave = !stopOnLeave) } },
        )
    }
}

/** A screen's own settings, opened from the sliders in the top bar. */
@Composable
fun OptionsSheet(m: AppModel, o: Options) {
    val rise = remember(o) { Animatable(60f) }
    LaunchedEffect(o) { rise.animateTo(0f, spring(dampingRatio = .8f, stiffness = 700f)) }
    Box(Modifier.fillMaxSize().background(Sk.colors.scrim).noRippleClick({ m.options = null }), contentAlignment = Alignment.BottomCenter) {
        InkCard(
            Modifier.padding(12.dp).widthIn(max = 560.dp).fillMaxWidth().heightIn(max = 620.dp)
                .graphicsLayer { translationY = rise.value.dp.toPx(); alpha = 1f - rise.value / 60f }
                .noRippleClick({}),
            bg = Sk.colors.surfaceOverlay,
        ) {
            Column {
                Row(Modifier.fillMaxWidth().background(Sk.colors.surfaceSunken).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) { Icon(o.icon, Sk.colors.accentAlt, 17.dp) }
                    Spacer(Modifier.width(10.dp))
                    T("${o.title} settings", sans(16, 800), Modifier.weight(1f))
                    InkButton({ m.options = null }, bg = Sk.colors.surfaceCard, fg = Sk.colors.textPrimary, height = 36.dp, padding = 14.dp) { fg ->
                        T("Done", sans(13, 700), color = fg)
                    }
                }
                Hairline()
                Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 4.dp)) {
                    optionRows(m, o).forEach { SettingRow(it) }
                }
            }
        }
    }
}

@Composable
internal fun SettingRow(r: Row_) {
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp)
                .then(if (r is ValueRow && r.onTap != null) Modifier.noRippleClick(r.onTap) else Modifier)
                .then(if (r is ToggleRow) Modifier.noRippleClick(r.flip) else Modifier)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                T(r.label, sans(15, 600, 1.35f))
                if (r.hint != null) { Spacer(Modifier.height(2.dp)); T(r.hint!!, sans(13, 400, 1.45f), color = Sk.colors.textTertiary) }
            }
            Spacer(Modifier.width(14.dp))
            when (r) {
                is ToggleRow -> Toggle(r.on, r.flip, r.label)
                is ValueRow -> T(r.value, mono(14), color = Sk.colors.textTertiary)
                is ActionRow -> Chip(r.cta, r.act, height = 34.dp, hPad = 14.dp, style = sans(13, 600))
                else -> {}
            }
        }
        if (r is SegRow<*>) SegOptions(r)
    }
}

@Composable
private fun <T> SegOptions(r: SegRow<T>) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        r.opts.forEach { (v, label) -> Chip(label, { r.pick(v) }, Modifier.weight(1f), on = v == r.cur, hPad = 4.dp) }
    }
}
