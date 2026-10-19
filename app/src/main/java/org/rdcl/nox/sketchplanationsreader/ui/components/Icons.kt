package org.rdcl.nox.sketchplanationsreader.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Lucide glyphs, 24-unit grid, drawn as 2px round-capped strokes (or fills for the
 * transport controls). Circles and rects are written out as paths.
 */
enum class Lucide(val filled: Boolean, vararg val d: String) {
    Back(false, "M19 12H5", "m12 19-7-7 7-7"),
    Menu(false, "M4 6h16", "M4 12h16", "M4 18h16"),
    More(true, "M14 5a2 2 0 1 1-4 0 2 2 0 0 1 4 0z", "M14 12a2 2 0 1 1-4 0 2 2 0 0 1 4 0z", "M14 19a2 2 0 1 1-4 0 2 2 0 0 1 4 0z"),
    Search(false, "M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z", "m21 21-4.3-4.3"),
    Shuffle(false, "M16 3h5v5", "M4 20 21 3", "M21 16v5h-5", "m15 15 6 6", "M4 4l5 5"),
    ArrowRight(false, "M5 12h14", "m12 5 7 7-7 7"),
    ArrowUp(false, "M12 19V5", "m5 12 7-7 7 7"),
    ArrowDown(false, "M12 5v14", "m19 12-7 7-7-7"),
    Chevron(false, "m9 18 6-6-6-6"),
    Download(false, "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "m7 10 5 5 5-5", "M12 15V3"),
    WifiOff(false, "M2 2l20 20", "M8.5 16.4a5 5 0 0 1 7 0", "M5 12.9a10 10 0 0 1 3.2-2.1", "M15.8 10.8A10 10 0 0 1 19 12.9",
        "M2 8.8a16 16 0 0 1 5-3", "M17 5.8a16 16 0 0 1 5 3", "M12 20h.01"),
    WifiOffSmall(false, "M2 2l20 20", "M8.5 16.4a5 5 0 0 1 7 0", "M5 12.9a10 10 0 0 1 3.2-2.1", "M12 20h.01"),
    External(false, "M15 3h6v6", "M10 14 21 3", "M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"),
    Close(false, "M18 6 6 18", "m6 6 12 12"),
    Calendar(false, "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z", "M16 2v4", "M8 2v4", "M3 10h18"),
    Trash(false, "M3 6h18", "M8 6V4h8v2", "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"),
    Play(true, "M6 3v18l14-9z"),
    Pause(true, "M6 4h4v16H6z", "M14 4h4v16h-4z"),
    Star(false, "m12 17.3-6.2 3.7 1.7-7L2 9.2l7.1-.6L12 2l2.9 6.6 7.1.6-5.5 4.8 1.7 7z"),
    StarFilled(true, "m12 17.3-6.2 3.7 1.7-7L2 9.2l7.1-.6L12 2l2.9 6.6 7.1.6-5.5 4.8 1.7 7z"),
    Share(false, "M21 5a3 3 0 1 1-6 0 3 3 0 0 1 6 0z", "M9 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0z", "M21 19a3 3 0 1 1-6 0 3 3 0 0 1 6 0z",
        "m8.6 13.5 6.8 4", "m15.4 6.5-6.8 4"),
    Check(false, "M20 6 9 17l-5-5"),
    Image(false, "M4 4h16v16H4z", "m4 16 4-4 3 3 4-4 5 5"),
    Clock(false, "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z", "M12 7v5l3 2"),
    Today(false, "m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z", "M9 22V12h6v10"),
    Archive(false, "M8 6h13", "M8 12h13", "M8 18h13", "M3 6h.01", "M3 12h.01", "M3 18h.01"),
    Library(false, "m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"),
    Chart(false, "M3 3v18h18", "M7 16v-5", "M12 16V8", "M17 16v-3"),
    Mic(false, "M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3z", "M19 10v2a7 7 0 0 1-14 0v-2", "M12 19v3"),
    Info(false, "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z", "M12 16v-4", "M12 8h.01"),
    Settings(false, "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z",
        "M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-2.9 1.2v.2a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.6 1.7 1.7 0 0 0-1.9.4l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0-1.2-2.9H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.6-1.1 1.7 1.7 0 0 0-.4-1.9l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.9.3H10a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.9-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.9V10a1.7 1.7 0 0 0 1.5 1H23a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"),
    Sun(false, "M12 17a5 5 0 1 0 0-10 5 5 0 0 0 0 10z", "M12 1v2", "M12 21v2", "M4.2 4.2l1.4 1.4", "M18.4 18.4l1.4 1.4",
        "M1 12h2", "M21 12h2", "M4.2 19.8l1.4-1.4", "M18.4 5.6l1.4-1.4"),
    Moon(false, "M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"),
    Link(false, "M10 13a5 5 0 0 0 7.5.5l3-3a5 5 0 0 0-7-7l-1.7 1.7", "M14 11a5 5 0 0 0-7.5-.5l-3 3a5 5 0 0 0 7 7l1.7-1.7"),
    Bell(false, "M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9", "M13.7 21a2 2 0 0 1-3.4 0"),
    Disk(false, "M22 12A10 10 0 1 1 12 2a10 10 0 0 1 10 10z", "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z"),
    Browse(false, "M4 4h16v13H4z", "M8 21h8", "m8 13 3-3 2 2 3-4"),
    /** The site's two-section search spinner (filled, not Lucide). */
    Spinner(true, "M0 11c.511-6.158 5.685-11 12-11s11.489 4.842 12 11h-2.009c-.506-5.046-4.793-9-9.991-9s-9.485 3.954-9.991 9h-2.009zm21.991 2c-.506 5.046-4.793 9-9.991 9s-9.485-3.954-9.991-9h-2.009c.511 6.158 5.685 11 12 11s11.489-4.842 12-11h-2.009z"),
}

private val cache = HashMap<Pair<Lucide, Float>, ImageVector>()

private fun vectorOf(icon: Lucide, stroke: Float): ImageVector = cache.getOrPut(icon to stroke) {
    val b = ImageVector.Builder(icon.name, 24.dp, 24.dp, 24f, 24f)
    icon.d.forEach { d ->
        val nodes = PathParser().parsePathString(d).toNodes()
        if (icon.filled) {
            b.addPath(nodes, fill = SolidColor(Color.Black))
        } else {
            b.addPath(
                nodes, stroke = SolidColor(Color.Black), strokeLineWidth = stroke,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            )
        }
    }
    b.build()
}

@Composable
fun Icon(icon: Lucide, color: Color, size: Dp = 22.dp, stroke: Float = 2f, modifier: Modifier = Modifier) {
    val v = remember(icon, stroke) { vectorOf(icon, stroke) }
    Image(v, contentDescription = null, modifier = modifier.size(size), colorFilter = ColorFilter.tint(color))
}
