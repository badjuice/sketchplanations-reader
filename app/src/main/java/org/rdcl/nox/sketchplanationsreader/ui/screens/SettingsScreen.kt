package org.rdcl.nox.sketchplanationsreader.ui.screens

import android.os.Environment
import android.os.StatFs
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import org.rdcl.nox.sketchplanationsreader.work.Bulk
import org.rdcl.nox.sketchplanationsreader.work.BulkDownloadService
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.rdcl.nox.sketchplanationsreader.BuildConfig
import org.rdcl.nox.sketchplanationsreader.data.AppSettings
import org.rdcl.nox.sketchplanationsreader.state.AppModel
import org.rdcl.nox.sketchplanationsreader.state.Options
import org.rdcl.nox.sketchplanationsreader.ui.components.Icon
import org.rdcl.nox.sketchplanationsreader.ui.components.InkButton
import org.rdcl.nox.sketchplanationsreader.ui.components.InkCard
import org.rdcl.nox.sketchplanationsreader.ui.components.LocalTablet
import org.rdcl.nox.sketchplanationsreader.ui.components.Lucide
import org.rdcl.nox.sketchplanationsreader.ui.components.Pill
import org.rdcl.nox.sketchplanationsreader.ui.components.T
import org.rdcl.nox.sketchplanationsreader.ui.components.noRippleClick
import org.rdcl.nox.sketchplanationsreader.ui.theme.FontPack
import org.rdcl.nox.sketchplanationsreader.ui.theme.Sk
import org.rdcl.nox.sketchplanationsreader.ui.theme.hand
import org.rdcl.nox.sketchplanationsreader.ui.theme.mono
import org.rdcl.nox.sketchplanationsreader.ui.theme.sans
import java.util.Locale
import kotlin.math.roundToInt

private class SettingsGroup(val title: String, val icon: Lucide, val rows: List<Row_>, val custom: (@Composable () -> Unit)? = null, val weight: Int = rows.size, val key: String? = null)

private fun mb(v: Double) = if (v < 1024) String.format(Locale.UK, "%.1f MB", v) else String.format(Locale.UK, "%.2f GB", v / 1024)
private fun gb(bytes: Long) = String.format(Locale.UK, "%.1f GB", bytes / 1e9)

@Composable
fun SettingsScreen(m: AppModel) {
    val s = m.settings
    val dark = Sk.colors.isDark
    val ctx = LocalContext.current
    val tablet = LocalTablet.current
    fun set(f: AppSettings.() -> AppSettings) = m.updateSettings(f)
    val groups = listOf(
        SettingsGroup("Appearance", Lucide.Sun, listOf(
            ToggleRow("Dark theme", "Sketches keep their cream paper", dark) { set { copy(dark = !dark) } },
            ToggleRow("Reduce motion", "Screens switch instantly; presses stop springing", s.reduceMotion) { set { copy(reduceMotion = !reduceMotion) } },
            SegRow("Typeface", "Applies to the whole app, including the sketch captions",
                FontPack.entries.map { it to it.label }, s.font) { v -> set { copy(font = v) } },
        )),
        // Each screen keeps its own settings under the sliders in its top bar; these are the same.
        SettingsGroup("Screens", Lucide.Today, Options.entries.map { o ->
            ActionRow(o.title, optionRows(m, o).joinToString(", ") { it.label.lowercase() }.replaceFirstChar { it.uppercase() }, "Open") { m.options = o }
        }, weight = 10),
        SettingsGroup("Offline", Lucide.Download, listOf(
            ToggleRow("Keep new sketches offline", "Each new sketch arrives ready to read", s.autoDl) { set { copy(autoDl = !autoDl) } },
            ToggleRow("Wi-Fi only", "Never use mobile data for offline copies", s.wifiOnly) { set { copy(wifiOnly = !wifiOnly) } },
            ToggleRow("High-resolution images", "Bigger files, better zoom", s.hd) { set { copy(hd = !hd) } },
            ToggleRow("Images on Wi-Fi only", "On mobile data a sketch waits behind a Load image button", s.imgWifiOnly) { set { copy(imgWifiOnly = !imgWifiOnly) } },
        )),
        SettingsGroup("Storage", Lucide.Disk, emptyList(), custom = @Composable { Storage(m); Spacer(Modifier.height(18.dp)); Destructive(m) }, weight = 13, key = "storage"),
        SettingsGroup("App", Lucide.Info, listOf(
            ValueRow("Version", "Sketchplanations reader", BuildConfig.VERSION_NAME) { m.tapVersion() },
            ValueRow("Build", "Unofficial · sketches © Jono Hey", BuildConfig.VERSION_CODE.toString()),
        )),
    )

    ScreenScroll(m) {
        if (!tablet) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                groups.forEach { GroupCard(m, it) }
            }
        } else {
            // Masonry: two columns, each card kept whole, greedily balanced by height.
            val cols = listOf(mutableListOf<SettingsGroup>(), mutableListOf())
            val h = intArrayOf(0, 0)
            groups.forEach { g -> val c = if (h[0] <= h[1]) 0 else 1; cols[c] += g; h[c] += g.weight + 2 }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                cols.forEach { col ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) { col.forEach { GroupCard(m, it) } }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupCard(m: AppModel, g: SettingsGroup) {
    // The download notification opens Settings scrolled to Storage.
    val into = remember { BringIntoViewRequester() }
    LaunchedEffect(m.settingsFocus) {
        if (g.key != null && m.settingsFocus == g.key) { delay(200); into.bringIntoView(); m.settingsFocus = null }
    }
    InkCard(Modifier.fillMaxWidth().bringIntoViewRequester(into)) {
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
        if (lib.offline.isEmpty() && lib.epOffline.isEmpty()) T("nothing kept offline yet ✎", hand(18), Modifier.padding(top = 10.dp), Sk.colors.textTertiary)

        Spacer(Modifier.height(16.dp))
        val last = if (lib.lastSync == 0L) "never" else android.text.format.DateUtils.getRelativeTimeSpanString(lib.lastSync).toString()
        StorageAction("Sync with website", "Checks for new sketches. Also runs on launch when online · last $last", m.syncPct, m.pulseFor("sync") ?: "Sync now") { m.syncWithWebsite() }
        BulkAction(m, Bulk.Kind.Sketches, "Keep every sketch offline", "${m.sketches.size} sketches · about ${(m.sketches.size * m.mbSketch).roundToInt()} MB at this quality", m.pulseFor("bulkS") ?: "Start")
        BulkAction(m, Bulk.Kind.Episodes, "Keep every episode offline", "${m.episodes.size} episodes · about ${String.format(Locale.UK, "%.1f", m.episodes.sumOf(m::epMB) / 1000)} GB", m.pulseFor("bulkE") ?: "Start")
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

/** A bulk download's row: Start, then Pause / Resume with live progress, speed and Cancel. */
@Composable
private fun BulkAction(m: AppModel, kind: Bulk.Kind, label: String, hint: String, cta: String) {
    val p = m.bulk?.takeIf { it.kind == kind }
    val shown by animateFloatAsState(p?.fraction ?: 0f, tween(Sk.motion.base * 2), label = "bulk")
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                T(label, sans(15, 600, 1.35f))
                Spacer(Modifier.height(2.dp))
                val status = when {
                    p == null -> hint
                    p.paused -> "Paused at ${p.done} of ${p.total}"
                    p.waiting -> "Waiting for Wi-Fi · ${p.done} of ${p.total}"
                    else -> "${p.done} of ${p.total}" + if (p.bytesPerSec > 0) " · ${BulkDownloadService.speed(p.bytesPerSec)}" else ""
                }
                T(status, sans(13, 400, 1.45f).copy(fontFeatureSettings = "tnum"), color = Sk.colors.textTertiary)
            }
            Spacer(Modifier.width(12.dp))
            val (text, act) = when {
                p == null -> cta to { m.askBulk(kind == Bulk.Kind.Sketches) }
                p.paused -> "Resume" to { m.resumeBulk() }
                else -> "Pause" to { m.pauseBulk() }
            }
            InkButton(act, Modifier.widthIn(min = 96.dp), bg = Sk.colors.surfaceCard, fg = Sk.colors.textPrimary, height = 36.dp, padding = 14.dp) { fg ->
                T(text, sans(13, 700), color = fg, maxLines = 1)
            }
        }
        if (p != null) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(10.dp).clip(Pill).border(2.dp, Sk.colors.ink, Pill).background(Sk.colors.surfaceSunken)) {
                    Box(Modifier.fillMaxWidth(shown).fillMaxHeight().background(if (p.paused) Sk.colors.rule else Sk.colors.penGreen))
                }
                Spacer(Modifier.width(12.dp))
                T("Cancel", sans(13, 700), Modifier.noRippleClick({ m.cancelBulk() }).padding(4.dp), Sk.colors.accentAlt)
            }
        }
    }
}

/** Three separate destructive buttons, each confirming the exact cost. Disabled when there is nothing to delete. */
@Composable
private fun Destructive(m: AppModel) {
    val lib = m.library
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DangerButton("Delete offline sketches", mb(m.sketchMB), lib.offline.isNotEmpty(), false) { m.askDeleteSketches() }
        DangerButton("Delete offline episodes", mb(m.podMB), lib.epOffline.isNotEmpty(), false) { m.askDeleteEpisodes() }
        DangerButton("Delete everything offline", mb(m.sketchMB + m.podMB), lib.offline.isNotEmpty() || lib.epOffline.isNotEmpty(), true) { m.askDeleteAll() }
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
