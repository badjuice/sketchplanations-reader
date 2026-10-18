package com.sketchplanations.reader.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sketchplanations.reader.BuildConfig
import com.sketchplanations.reader.data.AppSettings
import com.sketchplanations.reader.data.Catalogue
import com.sketchplanations.reader.data.HomeLayout
import com.sketchplanations.reader.data.LinkStyle
import com.sketchplanations.reader.state.AppModel
import com.sketchplanations.reader.ui.components.Chip
import com.sketchplanations.reader.ui.components.Icon
import com.sketchplanations.reader.ui.components.InkButton
import com.sketchplanations.reader.ui.components.InkCard
import com.sketchplanations.reader.ui.components.LocalTablet
import com.sketchplanations.reader.ui.components.Lucide
import com.sketchplanations.reader.ui.components.Pill
import com.sketchplanations.reader.ui.components.T
import com.sketchplanations.reader.ui.components.Toggle
import com.sketchplanations.reader.ui.components.noRippleClick
import com.sketchplanations.reader.ui.theme.FontPack
import com.sketchplanations.reader.ui.theme.Sk
import com.sketchplanations.reader.ui.theme.hand
import com.sketchplanations.reader.ui.theme.mono
import com.sketchplanations.reader.ui.theme.sans
import java.util.Locale
import kotlin.math.roundToInt

private sealed interface Row_ {
    val label: String
    val hint: String?
}

private class ToggleRow(override val label: String, override val hint: String?, val on: Boolean, val flip: () -> Unit) : Row_
private class ValueRow(override val label: String, override val hint: String?, val value: String, val onTap: (() -> Unit)? = null) : Row_
private class ActionRow(override val label: String, override val hint: String?, val cta: String, val act: () -> Unit) : Row_
private class SegRow<T>(override val label: String, override val hint: String?, val opts: List<Pair<T, String>>, val cur: T, val pick: (T) -> Unit) : Row_

private class SettingsGroup(val title: String, val icon: Lucide, val rows: List<Row_>, val custom: (@Composable () -> Unit)? = null, val weight: Int = rows.size)

private fun mb(v: Double) = if (v < 1024) String.format(Locale.UK, "%.1f MB", v) else String.format(Locale.UK, "%.2f GB", v / 1024)
private fun gb(bytes: Long) = String.format(Locale.UK, "%.1f GB", bytes / 1e9)

@Composable
fun SettingsScreen(m: AppModel) {
    val s = m.settings
    val dark = Sk.colors.isDark
    val ctx = LocalContext.current
    val tablet = LocalTablet.current
    fun set(f: AppSettings.() -> AppSettings) = m.updateSettings(f)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            set { copy(downloadFolder = uri.toString()) }
        }
    }
    val folder = s.downloadFolder?.let { Uri.parse(it).lastPathSegment?.replace("primary:", "/storage/emulated/0/") } ?: "Inside the app"

    val groups = listOf(
        SettingsGroup("Appearance", Lucide.Sun, listOf(
            ToggleRow("Dark theme", "Sketches keep their cream paper", dark) { set { copy(dark = !dark) } },
            ToggleRow("Larger reading text", "Lifts sketch prose and list titles; menus stay put", s.bigText) { set { copy(bigText = !bigText) } },
            ToggleRow("Reduce motion", "Drawer appears instantly; presses stop springing", s.reduceMotion) { set { copy(reduceMotion = !reduceMotion) } },
            SegRow("Today's layout", "A swipe deck one card at a time, or a scrolling feed", listOf(HomeLayout.Deck to "Swipe deck", HomeLayout.Feed to "Feed"), s.homeLayout) { v -> set { copy(homeLayout = v) } },
        )),
        SettingsGroup("Links", Lucide.Link, listOf(
            SegRow("Open external links in", "Sketch notes link out to books, papers and other sites", listOf(false to "In-app", true to "Browser"), s.extBrowser) { v -> set { copy(extBrowser = v) } },
            SegRow("Links in the text", "How a link looks inside a sketch’s words", listOf(LinkStyle.Badge to "Badges", LinkStyle.Underline to "Underlined"), s.linkStyle) { v -> set { copy(linkStyle = v) } },
        )),
        SettingsGroup("Archive", Lucide.Archive, listOf(
            ToggleRow("Letter jump rail", "The A–Z scrubber when sorting alphabetically", s.railLetters) { set { copy(railLetters = !railLetters) } },
            ToggleRow("Year jump rail", "The scrubber when sorting by date", s.railYears) { set { copy(railYears = !railYears) } },
            ToggleRow("Show every letter", "Keeps the rail complete — empty ones sit greyed out", s.railAll) { set { copy(railAll = !railAll) } },
        )),
        SettingsGroup("Search", Lucide.Search, listOf(
            ToggleRow("Save recent searches", "Kept on this device only", s.showRecents) { set { copy(showRecents = !showRecents) } },
        )),
        SettingsGroup("Offline", Lucide.Download, listOf(
            ToggleRow("Auto-download new sketches", "Each new sketch arrives ready to read", s.autoDl) { set { copy(autoDl = !autoDl) } },
            ToggleRow("Wi-Fi only", "Never use mobile data for downloads", s.wifiOnly) { set { copy(wifiOnly = !wifiOnly) } },
            ToggleRow("High-resolution images", "Bigger files, better zoom", s.hd) { set { copy(hd = !hd) } },
            ToggleRow("Images on Wi-Fi only", "On mobile data a sketch waits behind a Load image button", s.imgWifiOnly) { set { copy(imgWifiOnly = !imgWifiOnly) } },
            ActionRow("Download folder", folder, "Change") { runCatching { picker.launch(null) }.onFailure { m.flash("No folder picker on this device") } },
        )),
        SettingsGroup("Podcast", Lucide.Mic, listOf(
            SegRow("Playback speed", "Applies to every episode", listOf(1f to "1×", 1.25f to "1.25×", 1.5f to "1.5×", 2f to "2×"), s.speed) { v -> set { copy(speed = v) } },
            SegRow("Skip back", "The left button on the player", listOf(10 to "10s", 15 to "15s", 30 to "30s"), s.skipBack) { v -> set { copy(skipBack = v) } },
            SegRow("Skip forward", "The right button on the player", listOf(15 to "15s", 30 to "30s", 45 to "45s"), s.skipFwd) { v -> set { copy(skipFwd = v) } },
            SegRow("Sleep timer", "Fades out and pauses", listOf(0 to "Off", 15 to "15m", 30 to "30m", 60 to "1h"), s.sleep) { v -> m.setSleep(v) },
            ToggleRow("Play the next episode", "Rolls straight on when one finishes", s.continuous) { set { copy(continuous = !continuous) } },
            ToggleRow("Delete after listening", "Frees the space once you reach the end", s.autoDelete) { set { copy(autoDelete = !autoDelete) } },
            ToggleRow("High-quality streaming", "Uses more data when not downloaded", s.streamHq) { set { copy(streamHq = !streamHq) } },
            ToggleRow("Keep playing in the background", "Carries on with the screen locked or the app closed", s.bgPlay) { set { copy(bgPlay = !bgPlay) } },
            ToggleRow("Lock screen and shade controls", "Play, skip and chapter title outside the app", s.notifPlayer) { set { copy(notifPlayer = !notifPlayer) } },
            ToggleRow("Pause for calls", "Stops on an incoming call and resumes after", s.pauseOnCall) { set { copy(pauseOnCall = !pauseOnCall) } },
            ToggleRow("Stop when leaving the episode", "Off by default — audio follows you around the app", s.stopOnLeave) { set { copy(stopOnLeave = !stopOnLeave) } },
        ), weight = 16),
        SettingsGroup("Notifications", Lucide.Bell, listOf(
            ToggleRow("New sketch each week", null, s.notifySketch) { set { copy(notifySketch = !notifySketch) } },
            ToggleRow("New podcast episode", null, s.notifyEp) { set { copy(notifyEp = !notifyEp) } },
        )),
        SettingsGroup("Storage", Lucide.Disk, emptyList(), custom = @Composable { Storage(m) }, weight = 9),
        SettingsGroup("Typeface", Lucide.Info, listOf(
            SegRow("Typeface", "Applies to the whole app — including the sketch captions",
                FontPack.entries.map { it to it.label }, s.font) { v -> set { copy(font = v) } },
        )),
        SettingsGroup("Delete downloads", Lucide.Trash, emptyList(), custom = @Composable { Destructive(m) }, weight = 4),
        SettingsGroup("App", Lucide.Info, listOf(
            ValueRow("Version", "Sketchplanations reader", BuildConfig.VERSION_NAME) { m.tapVersion() },
            ValueRow("Build", "Unofficial · sketches © Jono Hey", BuildConfig.VERSION_CODE.toString()),
        )),
    )

    ScreenScroll(m) {
        if (!tablet) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                groups.forEach { GroupCard(it) }
            }
        } else {
            // Masonry: two columns, each card kept whole, greedily balanced by height.
            val cols = listOf(mutableListOf<SettingsGroup>(), mutableListOf())
            val h = intArrayOf(0, 0)
            groups.forEach { g -> val c = if (h[0] <= h[1]) 0 else 1; cols[c] += g; h[c] += g.weight + 2 }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                cols.forEach { col ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) { col.forEach { GroupCard(it) } }
                }
            }
        }
    }
}

@Composable
private fun GroupCard(g: SettingsGroup) {
    InkCard(Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth().background(Sk.colors.surfaceSunken).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) { Icon(g.icon, Sk.colors.accentAlt, 17.dp) }
                Spacer(Modifier.width(10.dp))
                T(g.title, sans(16, 800))
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(Sk.colors.borderDefault))
            g.custom?.invoke()
            if (g.rows.isNotEmpty()) Column(Modifier.padding(vertical = 4.dp)) { g.rows.forEach { SettingRow(it) } }
        }
    }
}

@Composable
private fun SettingRow(r: Row_) {
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp)
                .then(if (r is ValueRow && r.onTap != null) Modifier.noRippleClick(r.onTap) else Modifier)
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

/** One bar with device context on the bar itself: this app · everything else · free. */
@Composable
private fun Storage(m: AppModel) {
    val stat = remember { StatFs(Environment.getDataDirectory().path) }
    val total = stat.totalBytes
    val free = stat.availableBytes
    val libBytes = (m.libMB * 1_000_000).toLong()
    val other = (total - free - libBytes).coerceAtLeast(0)
    val lib = m.library
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            T(mb(m.libMB), sans(20, 800))
            Spacer(Modifier.width(6.dp))
            T("in this app", mono(13), color = Sk.colors.textTertiary)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().height(24.dp).clip(Pill).border(2.dp, Sk.colors.ink, Pill)) {
            Box(Modifier.weight((libBytes.toFloat() / total).coerceAtLeast(.006f)).fillMaxHeight().background(Sk.colors.ink).widthIn(min = 7.dp))
            Box(Modifier.weight((other.toFloat() / total).coerceAtLeast(.001f)).fillMaxHeight().background(Sk.colors.rule), contentAlignment = Alignment.Center) {
                T("${gb(other)} other", sans(10, 600), color = Sk.colors.textSecondary, maxLines = 1)
            }
            Box(Modifier.weight((free.toFloat() / total).coerceAtLeast(.001f)).fillMaxHeight().background(Sk.colors.surfaceSunken), contentAlignment = Alignment.Center) {
                T("${gb(free)} free", sans(10, 600), color = Sk.colors.textTertiary, maxLines = 1)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            T("0", mono(10), color = Sk.colors.textTertiary)
            T("${gb(free)} free of ${gb(total)}", mono(10), color = Sk.colors.textTertiary)
        }
        Spacer(Modifier.height(14.dp))
        T("What this app is holding", sans(12, 600), Modifier.padding(bottom = 6.dp), Sk.colors.textSecondary)
        Row(Modifier.fillMaxWidth().height(14.dp).clip(Pill).border(2.dp, Sk.colors.ink, Pill).background(Sk.colors.surfaceSunken), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(Modifier.weight((m.mbBase / m.libMB).toFloat()).fillMaxHeight().background(Sk.colors.rule))
            if (lib.offline.isNotEmpty()) Box(Modifier.weight((m.sketchMB / m.libMB).toFloat().coerceAtLeast(.01f)).fillMaxHeight().background(Sk.colors.penGreen))
            if (lib.epOffline.isNotEmpty()) Box(Modifier.weight((m.podMB / m.libMB).toFloat().coerceAtLeast(.01f)).fillMaxHeight().background(Sk.colors.penBlue))
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Legend(Sk.colors.rule, "App data ${mb(m.mbBase)}")
            Legend(Sk.colors.penGreen, "Sketches ${mb(m.sketchMB)}")
            Legend(Sk.colors.penBlue, "Episodes ${mb(m.podMB)}")
        }
        if (lib.offline.isEmpty() && lib.epOffline.isEmpty()) T("nothing downloaded yet ✎", hand(18), Modifier.padding(top = 10.dp), Sk.colors.textTertiary)

        Spacer(Modifier.height(16.dp))
        val last = if (lib.lastSync == 0L) "never" else android.text.format.DateUtils.getRelativeTimeSpanString(lib.lastSync).toString()
        StorageAction("Sync with website", "Checks for new sketches. Also runs on launch when online · last $last", m.syncPct, m.pulseFor("sync") ?: "Sync now") { m.syncWithWebsite() }
        StorageAction("Download all sketches", "${Catalogue.TOTAL_POSTS} posts · about ${(Catalogue.TOTAL_POSTS * m.mbSketch).roundToInt()} MB at this quality", m.bulkSketches, m.pulseFor("bulkS") ?: "Download") { m.askBulk(true) }
        StorageAction("Download all episodes", "${Catalogue.TOTAL_EPISODES} episodes · about ${String.format(Locale.UK, "%.1f", Catalogue.TOTAL_EPISODES * m.mbEp / 1024)} GB — large", m.bulkEpisodes, m.pulseFor("bulkE") ?: "Download") { m.askBulk(false) }
    }
}

@Composable
private fun Legend(c: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(c))
        Spacer(Modifier.width(6.dp))
        T(text, sans(12), color = Sk.colors.textSecondary)
    }
}

@Composable
private fun StorageAction(label: String, hint: String, pct: Int?, cta: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            T(label, sans(15, 600, 1.35f))
            Spacer(Modifier.height(2.dp))
            T(hint, sans(13, 400, 1.45f), color = Sk.colors.textTertiary)
        }
        Spacer(Modifier.width(12.dp))
        InkButton(
            { if (pct == null) onClick() }, Modifier.widthIn(min = 96.dp), bg = Sk.colors.surfaceCard, fg = Sk.colors.textPrimary,
            height = 36.dp, padding = 14.dp, progress = pct?.let { it / 100f },
        ) { fg -> T(if (pct != null) "$pct%" else cta, sans(13, 700).copy(fontFeatureSettings = "tnum"), color = fg, maxLines = 1) }
    }
}

/** Three separate destructive buttons, each confirming the exact cost. Disabled when there is nothing to delete. */
@Composable
private fun Destructive(m: AppModel) {
    val lib = m.library
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DangerButton("Delete downloaded sketches", mb(m.sketchMB), lib.offline.isNotEmpty(), false) { m.askDeleteSketches() }
        DangerButton("Delete downloaded episodes", mb(m.podMB), lib.epOffline.isNotEmpty(), false) { m.askDeleteEpisodes() }
        DangerButton("Delete all downloads", mb(m.sketchMB + m.podMB), lib.offline.isNotEmpty() || lib.epOffline.isNotEmpty(), true) { m.askDeleteAll() }
    }
}

@Composable
private fun DangerButton(text: String, size: String, enabled: Boolean, solid: Boolean, onClick: () -> Unit) {
    InkButton(
        onClick, Modifier.fillMaxWidth().alpha(if (enabled) 1f else .4f),
        bg = if (solid) Sk.colors.error else Sk.colors.surfaceCard,
        fg = if (solid) Color(0xFFFFFDF2) else Sk.colors.textPrimary,
        height = null, padding = 18.dp, arrangement = Arrangement.SpaceBetween, enabled = enabled,
    ) { fg ->
        T(text, sans(15, 700), Modifier.weight(1f).padding(vertical = 10.dp), fg)
        T(size, mono(13), color = if (solid) fg.copy(alpha = .75f) else Sk.colors.textTertiary)
    }
}
