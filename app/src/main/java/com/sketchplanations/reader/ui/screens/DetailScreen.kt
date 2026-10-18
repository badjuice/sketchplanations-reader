package com.sketchplanations.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sketchplanations.reader.data.Catalogue
import com.sketchplanations.reader.data.LinkKind
import com.sketchplanations.reader.data.LinkStyle
import com.sketchplanations.reader.data.ProsePart
import com.sketchplanations.reader.data.parseBody
import com.sketchplanations.reader.state.AppModel
import com.sketchplanations.reader.ui.components.ArtBlock
import com.sketchplanations.reader.ui.components.Chip
import com.sketchplanations.reader.ui.components.Eyebrow
import com.sketchplanations.reader.ui.components.Icon
import com.sketchplanations.reader.ui.components.InkButton
import com.sketchplanations.reader.ui.components.InkCard
import com.sketchplanations.reader.ui.components.LocalTablet
import com.sketchplanations.reader.ui.components.Lucide
import com.sketchplanations.reader.ui.components.Pill
import com.sketchplanations.reader.ui.components.PressRow
import com.sketchplanations.reader.ui.components.PrimaryButton
import com.sketchplanations.reader.ui.components.T
import com.sketchplanations.reader.ui.components.noRippleClick
import com.sketchplanations.reader.ui.components.readMeasure
import com.sketchplanations.reader.ui.theme.Art
import com.sketchplanations.reader.ui.theme.Sk
import com.sketchplanations.reader.ui.theme.hand
import com.sketchplanations.reader.ui.theme.mono
import com.sketchplanations.reader.ui.theme.sans

/** Inline colours are the text-weight ink variants — the display pens fail contrast at 16px. */
@Composable
fun kindColor(k: LinkKind): Color = when (k) {
    LinkKind.Sketch -> Sk.colors.accent
    LinkKind.Pod -> Sk.colors.penBlueInk
    LinkKind.Book -> Sk.colors.penOrangeInk
    LinkKind.Paper -> Sk.colors.penGreenInk
    LinkKind.Video -> Sk.colors.textLinkHover
    LinkKind.Web -> Sk.colors.textSecondary
}

@Composable
fun kindWash(k: LinkKind): Color = when (k) {
    LinkKind.Sketch -> Sk.colors.accentWash
    LinkKind.Pod -> Sk.colors.infoWash
    LinkKind.Book -> Sk.colors.warningWash
    LinkKind.Paper -> Sk.colors.successWash
    LinkKind.Video -> Sk.colors.errorWash
    LinkKind.Web -> Sk.colors.surfaceSunken
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(m: AppModel) {
    val d = m.sketch
    val tablet = LocalTablet.current
    val big = m.settings.bigText
    val links = Catalogue.linksFor(d.n)
    ScreenScroll(m, align = Alignment.CenterHorizontally) {
        if (m.imageHeld(d.n)) HeldPanel { m.reveal(d.n) }
        if (m.needsNet(d.n)) OfflinePanel { m.retryFetch(d.n) }

        Box(
            Modifier.fillMaxWidth().height(300.dp).background(Art.paper).noRippleClick({ m.zoom = true }),
        ) {
            Row(Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                T("sketch ${d.num} · drop image ✎", hand(17), Modifier.weight(1f), Art.caption)
                T("Tap to zoom", sans(12, 700), Modifier.clip(Pill).background(Art.line).padding(horizontal = 12.dp, vertical = 5.dp), Art.page)
            }
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(Art.line))
        }

        Column(Modifier.fillMaxWidth().readMeasure(tablet).padding(start = 16.dp, end = 16.dp, top = 20.dp)) {
            T("${d.num} · ${d.date}", mono(12), color = Sk.colors.textTertiary)
            Spacer(Modifier.height(8.dp))
            T(d.title, sans(if (big) 36 else 32, 800, 1.2f, -0.01f))
            Spacer(Modifier.height(12.dp))
            T(d.lead, sans(if (big) 20 else 18, 600, 1.5f))
            Spacer(Modifier.height(14.dp))
            parseBody(d.body, links).forEach { parts ->
                Prose(parts, if (big) 18 else 16, m.settings.linkStyle) { link, label -> m.follow(link, label) }
                Spacer(Modifier.height(14.dp))
            }

            if (links.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Eyebrow("Links in this sketch")
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    links.forEach { l ->
                        val col = kindColor(l.kind)
                        val label = l.sketchRef?.let { Catalogue.sketch(it)?.title } ?: l.label
                        PressRow({ m.follow(l, l.label) },
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Sk.colors.surfaceCard)
                                .border(1.dp, Sk.colors.borderQuiet, RoundedCornerShape(12.dp))) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(kindWash(l.kind)), contentAlignment = Alignment.Center) {
                                    Icon(if (l.kind.internal) Lucide.ArrowRight else Lucide.External, col, 13.dp, stroke = 2.2f)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    T(label, sans(15, 600, 1.35f))
                                    Spacer(Modifier.height(2.dp))
                                    T(l.kind.label + if (l.kind.internal) " · stays in the app" else " · ${l.host}", mono(11), color = col)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Eyebrow("Tags")
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                d.tags.forEach { t ->
                    Chip(t, { m.openTag(t) }, height = null, hPad = 10.dp, vPad = 3.dp, style = sans(12, 600), fg = Sk.colors.textTertiary)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** Mobile data + Images on Wi-Fi only: the image waits, and the line says which setting is why. */
@Composable
fun HeldPanel(onLoad: () -> Unit) {
    InkCard(Modifier.padding(16.dp).fillMaxWidth(), bg = Art.paper, shadowColor = Art.line) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            T("image held back — you're on mobile data ✎", hand(17), color = Art.caption)
            InkButton(onLoad, bg = Sk.colors.accentAlt, border = Art.line, shadowColor = Art.line) { fg ->
                Icon(Lucide.Download, fg, 16.dp, stroke = 2.2f); Spacer(Modifier.width(8.dp)); T("Load image", sans(15, 700), color = fg)
            }
            T("Settings › Offline › Images on Wi-Fi only", sans(13, 400, 1.45f), color = Art.caption)
        }
    }
}

@Composable
fun OfflinePanel(onRetry: () -> Unit) {
    InkCard(Modifier.padding(16.dp).fillMaxWidth(), bg = Sk.colors.warningWash) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Lucide.WifiOff, Sk.colors.penOrange, 20.dp)
                Spacer(Modifier.width(10.dp))
                T("You're offline", sans(18, 800))
            }
            Spacer(Modifier.height(8.dp))
            T("This one hasn't been downloaded yet, so there's nothing stored on the device to show. Connect and it'll fetch in a second.",
                sans(15, 400, 1.5f), color = Sk.colors.textSecondary)
            Spacer(Modifier.height(14.dp))
            InkButton(onRetry, height = 40.dp) { fg -> T("Try again", sans(15, 700), color = fg) }
        }
    }
}

private const val ICON = "icon"

/**
 * A paragraph with inline links. Badges sit on the card ground with a 1px border in the kind
 * colour and wrap line by line (box-decoration-break: clone); the underlined style drops the
 * ground. Both carry an 11px glyph after the text: an arrow for in-app, external otherwise.
 */
@Composable
private fun Prose(parts: List<ProsePart>, size: Int, style: LinkStyle, onLink: (com.sketchplanations.reader.data.SketchLink?, String) -> Unit) {
    val badge = style == LinkStyle.Badge
    val ranges = mutableListOf<Triple<Int, Int, Color>>()
    val linkColors = parts.filterIsInstance<ProsePart.Link>().map { kindColor(it.kind) }
    val icons = mutableMapOf<String, Pair<Lucide, Color>>()
    val text: AnnotatedString = buildAnnotatedString {
        var li = 0
        parts.forEach { p ->
            when (p) {
                is ProsePart.Plain -> append(p.text)
                is ProsePart.Link -> {
                    val col = linkColors[li]
                    val id = "$ICON$li"
                    icons[id] = (if (p.kind.internal) Lucide.ArrowRight else Lucide.External) to col
                    val start = length
                    withLink(LinkAnnotation.Clickable(id) { onLink(p.target, p.text) }) {
                        withStyle(SpanStyle(color = col, fontWeight = FontWeight.SemiBold, textDecoration = if (badge) null else TextDecoration.Underline)) {
                            append(p.text)
                            appendInlineContent(id, "→")
                        }
                    }
                    ranges += Triple(start, length, col)
                    li++
                }
            }
        }
    }
    val inline = icons.mapValues { (_, v) ->
        InlineTextContent(Placeholder(14.sp, 11.sp, PlaceholderVerticalAlign.TextCenter)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) { Icon(v.first, v.second, 11.dp, stroke = 2.6f) }
        }
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val card = Sk.colors.surfaceCard
    val base = sans(size, 400, 1.65f).copy(color = Sk.colors.textPrimary)
    BasicText(
        text,
        style = base,
        inlineContent = inline,
        onTextLayout = { layout = it },
        modifier = Modifier.fillMaxWidth().drawBehind {
            val l = layout ?: return@drawBehind
            if (!badge) return@drawBehind
            val fontPx = size.sp.toPx()
            ranges.forEach { (s, e, col) ->
                val first = l.getLineForOffset(s)
                val last = l.getLineForOffset((e - 1).coerceAtLeast(s))
                for (line in first..last) {
                    val left = if (line == first) l.getHorizontalPosition(s, true) else l.getLineLeft(line)
                    val right = if (line == last) l.getHorizontalPosition(e, true).let { if (it <= left) l.getLineRight(line) else it } else l.getLineRight(line)
                    if (right - left < 2f) continue
                    // CSS `padding: 0 3px` — the box grows outward; the neighbouring spaces absorb it.
                    val pad = 3.dp.toPx()
                    val mid = (l.getLineTop(line) + l.getLineBottom(line)) / 2f
                    val h = fontPx * 1.3f
                    val tl = Offset(left - pad, mid - h / 2f)
                    val sz = Size(right - left + pad * 2, h)
                    val r = CornerRadius(4.dp.toPx())
                    drawRoundRect(card, tl, sz, r)
                    drawRoundRect(col, tl, sz, r, style = Stroke(1.dp.toPx()))
                }
            }
        },
    )
}

/** Anchored action bar — does not scroll. Favourite · Share · Download · Save image. */
@Composable
fun DetailActions(m: AppModel) {
    val d = m.sketch
    val tablet = LocalTablet.current
    val fav = m.isFavourite(d.n)
    val off = m.isOffline(d.n)
    val gal = m.inGallery(d.n)
    Column(Modifier.fillMaxWidth().background(Sk.colors.surfaceChrome), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(Sk.colors.ink))
        Column(Modifier.fillMaxWidth().readMeasure(tablet).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(m.pulseFor("fav") ?: if (fav) "Favourited" else "Favourite", if (fav) Lucide.StarFilled else Lucide.Star, fav, Modifier.weight(1f)) { m.toggleFavourite(d.n) }
                ActionButton(m.pulseFor("share") ?: "Share", Lucide.Share, false, Modifier.weight(1f)) { m.share(d) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val running = m.dl == d.n
                ActionButton(
                    m.pulseFor("dl") ?: if (off) "Downloaded" else "Download post", if (off) Lucide.Check else Lucide.Download, off,
                    Modifier.weight(1f), progress = if (running) m.pct / 100f else null,
                ) { m.toggleDownload(d.n) }
                ActionButton(m.pulseFor("gal") ?: if (gal) "In gallery" else "Save image", if (gal) Lucide.Check else Lucide.Image, gal, Modifier.weight(1f)) { m.toggleGallery(d.n) }
            }
        }
    }
}

/** Ink-filled when the thing is done; while downloading, the live percentage replaces the label. */
@Composable
fun ActionButton(label: String, icon: Lucide, on: Boolean, modifier: Modifier = Modifier, progress: Float? = null, onClick: () -> Unit) {
    InkButton(
        onClick, modifier,
        bg = if (on && progress == null) Sk.colors.ink else Sk.colors.surfaceCard,
        fg = if (on && progress == null) Sk.colors.paperTint else Sk.colors.textPrimary,
        padding = 12.dp, progress = progress,
    ) { fg ->
        if (progress != null) {
            T("${(progress * 100).toInt()}%", sans(14, 700).copy(fontFeatureSettings = "tnum"), color = Sk.colors.textPrimary)
        } else {
            Icon(icon, fg, 16.dp)
            Spacer(Modifier.width(8.dp))
            T(label, sans(14, 700), color = fg, maxLines = 1)
        }
    }
}

/** Big-image viewer: pinch to zoom, drag to pan, tap to close. The sketch keeps its cream paper. */
@Composable
fun ZoomViewer(m: AppModel) {
    val d = m.sketch
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val tf = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 5f)
        offset = if (scale == 1f) Offset.Zero else offset + pan
    }
    Column(
        Modifier.fillMaxSize().background(Art.viewer).pointerInput(Unit) {
            detectTapGestures(onDoubleTap = { scale = if (scale > 1f) 1f else 2.5f; offset = Offset.Zero }, onTap = { m.zoom = false })
        },
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            T("${d.num} · pinch to zoom", mono(13), color = Color(0xBFFBF8DE))
            Icon(Lucide.Close, Art.page, 22.dp)
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp).transformable(tf), contentAlignment = Alignment.Center) {
            ArtBlock(
                Modifier.fillMaxWidth().fillMaxHeight(.72f)
                    .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y }
                    .border(2.dp, Color(0xFFFFFDF2), RoundedCornerShape(12.dp)),
                radius = 12.dp, border = false, caption = "the sketch keeps its cream paper ✎",
            )
        }
        T(d.title, sans(15, 600), Modifier.padding(horizontal = 16.dp, vertical = 18.dp), Color(0xD9FBF8DE))
    }
}
