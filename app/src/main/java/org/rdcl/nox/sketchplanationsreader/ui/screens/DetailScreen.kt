package org.rdcl.nox.sketchplanationsreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import org.rdcl.nox.sketchplanations.explode.Block
import org.rdcl.nox.sketchplanations.explode.Images
import org.rdcl.nox.sketchplanations.explode.Link
import org.rdcl.nox.sketchplanations.explode.LinkKind
import org.rdcl.nox.sketchplanations.explode.LinkTarget
import org.rdcl.nox.sketchplanations.explode.Links
import org.rdcl.nox.sketchplanations.explode.Rendition
import org.rdcl.nox.sketchplanations.explode.RichText
import org.rdcl.nox.sketchplanations.explode.Sketch
import org.rdcl.nox.sketchplanations.explode.SpanStyle as RichStyle
import org.rdcl.nox.sketchplanationsreader.data.LinkStyle
import org.rdcl.nox.sketchplanationsreader.state.AppModel
import org.rdcl.nox.sketchplanationsreader.ui.code
import org.rdcl.nox.sketchplanationsreader.ui.date
import org.rdcl.nox.sketchplanationsreader.ui.image
import org.rdcl.nox.sketchplanationsreader.ui.num
import org.rdcl.nox.sketchplanationsreader.ui.components.ArtBlock
import org.rdcl.nox.sketchplanationsreader.ui.components.Chip
import org.rdcl.nox.sketchplanationsreader.ui.components.Eyebrow
import org.rdcl.nox.sketchplanationsreader.ui.components.Icon
import org.rdcl.nox.sketchplanationsreader.ui.components.InkButton
import org.rdcl.nox.sketchplanationsreader.ui.components.InkCard
import org.rdcl.nox.sketchplanationsreader.ui.components.LocalTablet
import org.rdcl.nox.sketchplanationsreader.ui.components.Lucide
import org.rdcl.nox.sketchplanationsreader.ui.components.Pill
import org.rdcl.nox.sketchplanationsreader.ui.components.PressRow
import org.rdcl.nox.sketchplanationsreader.ui.components.T
import org.rdcl.nox.sketchplanationsreader.ui.components.noRippleClick
import org.rdcl.nox.sketchplanationsreader.ui.components.readMeasure
import org.rdcl.nox.sketchplanationsreader.ui.theme.Art
import org.rdcl.nox.sketchplanationsreader.ui.theme.Shadow
import org.rdcl.nox.sketchplanationsreader.ui.theme.Sk
import org.rdcl.nox.sketchplanationsreader.ui.theme.hand
import org.rdcl.nox.sketchplanationsreader.ui.theme.heading
import org.rdcl.nox.sketchplanationsreader.ui.theme.mono
import org.rdcl.nox.sketchplanationsreader.ui.theme.sans

/** How a link is badged, so the reader can tell where it goes before tapping. */
enum class LinkBadge(val label: String, val internal: Boolean) {
    Sketch("Sketchplanation", true),
    Topic("Topic", true),
    Pod("Podcast episode", true),
    Version("Another version", true),
    Affiliate("Book (affiliate)", false),
    Book("Book", false),
    Paper("Paper / study", false),
    Video("Video", false),
    Site("Sketchplanations site", false),
    Web("External site", false),
}

fun badgeOf(l: Link): LinkBadge = when (val t = l.target) {
    is LinkTarget.Sketch -> LinkBadge.Sketch
    is LinkTarget.Topic -> LinkBadge.Topic
    is LinkTarget.Episode -> LinkBadge.Pod
    is LinkTarget.ImageVersion -> LinkBadge.Version
    is LinkTarget.External -> when (t.kind) {
        LinkKind.Book -> if (Links.isAffiliate(t.url)) LinkBadge.Affiliate else LinkBadge.Book
        LinkKind.Paper -> LinkBadge.Paper
        LinkKind.Video -> LinkBadge.Video
        LinkKind.Site -> LinkBadge.Site
        LinkKind.Web -> LinkBadge.Web
    }
}

private fun hostOf(l: Link) = (l.target as? LinkTarget.External)?.url
    ?.let { runCatching { java.net.URI(it).host?.removePrefix("www.") }.getOrNull() }.orEmpty()

/** Inline colours are the text-weight ink variants, because the display pens fail contrast at 16px. */
@Composable
fun badgeColor(k: LinkBadge): Color = when (k) {
    LinkBadge.Sketch, LinkBadge.Topic, LinkBadge.Version -> Sk.colors.accent
    LinkBadge.Pod -> Sk.colors.penBlueInk
    LinkBadge.Affiliate, LinkBadge.Book -> Sk.colors.penOrangeInk
    LinkBadge.Paper -> Sk.colors.penGreenInk
    LinkBadge.Video -> Sk.colors.textLinkHover
    LinkBadge.Site, LinkBadge.Web -> Sk.colors.textSecondary
}

@Composable
fun badgeWash(k: LinkBadge): Color = when (k) {
    LinkBadge.Sketch, LinkBadge.Topic, LinkBadge.Version -> Sk.colors.accentWash
    LinkBadge.Pod -> Sk.colors.infoWash
    LinkBadge.Affiliate, LinkBadge.Book -> Sk.colors.warningWash
    LinkBadge.Paper -> Sk.colors.successWash
    LinkBadge.Video -> Sk.colors.errorWash
    LinkBadge.Site, LinkBadge.Web -> Sk.colors.surfaceSunken
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(m: AppModel) {
    val s = m.sketch ?: return NothingStored(m)
    val full by produceState<Sketch?>(null, s.uid, m.contentVersion) { value = m.repo.sketch(s.uid) }
    val tablet = LocalTablet.current
    val big = m.settings.bigText
    val bodySize = if (big) 18 else 16
    val onLink: (Link) -> Unit = { m.follow(it) }
    var artFailed by remember(s.uid) { mutableStateOf(false) }

    ScreenScroll(m, align = Alignment.CenterHorizontally) {
        val held = m.imageHeld(s.uid)
        if (held) HeldPanel { m.reveal(s.uid) }
        if (artFailed && !m.online) OfflinePanel { m.retryFetch(s.uid) }

        // The whole sketch, never cropped: the licence asks that the Sketchplanations mark stays on it.
        val img = full?.image ?: s.image
        val ratio = img.width?.let { w -> img.height?.let { h -> (w.toFloat() / h).coerceIn(.6f, 2.2f) } }
        Box(
            Modifier.fillMaxWidth().then(if (ratio != null) Modifier.aspectRatio(ratio) else Modifier.height(300.dp))
                .background(Art.page).noRippleClick({ if (!held) m.zoomUrl = m.downloads.image(img, Rendition.Hd) }),
        ) {
            if (!held) {
                AsyncImage(
                    model = m.downloads.image(img, Rendition.Read), contentDescription = img.alt, contentScale = ContentScale.Fit,
                    onState = { artFailed = it is AsyncImagePainter.State.Error },
                    modifier = Modifier.fillMaxSize(),
                )
                T("Tap to zoom", sans(12, 700), Modifier.align(Alignment.BottomEnd).padding(12.dp).clip(Pill).background(Art.line).padding(horizontal = 12.dp, vertical = 5.dp), Art.page)
            }
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(Art.line))
        }

        Column(Modifier.fillMaxWidth().readMeasure(tablet).padding(start = 16.dp, end = 16.dp, top = 20.dp)) {
            T("${s.num} · ${s.date}", mono(12), color = Sk.colors.textTertiary)
            Spacer(Modifier.height(8.dp))
            T(s.title, sans(if (big) 36 else 32, 800, 1.2f, -0.01f))
            Spacer(Modifier.height(14.dp))

            val f = full
            if (f == null) {
                T(s.lead, sans(bodySize, 400, 1.65f), color = Sk.colors.textSecondary)
            } else {
                val cut = f.relatedSectionStart.takeIf { f.related.any { it in m.byUid } } ?: f.body.size
                f.body.take(cut).forEachIndexed { i, b ->
                    BlockView(b, bodySize, m.settings.linkStyle, standfirst = i == 0 && f.standfirst != null, onLink = onLink)
                }

                val related = f.related.mapNotNull { m.byUid[it] }
                if (related.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Eyebrow("Related ideas")
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        related.forEach { r ->
                            InkCard(Modifier.width(150.dp).padding(end = 3.dp, bottom = 3.dp), shadow = Shadow.sm, radius = 12.dp) {
                                Column(Modifier.noRippleClick({ m.open(r.uid) })) {
                                    ArtBlock(Modifier.fillMaxWidth().height(96.dp), radius = 0.dp, border = false, framed = true, image = r.image)
                                    Box(Modifier.fillMaxWidth().height(2.dp).background(Art.line))
                                    T(r.title, sans(14, 700, 1.3f), Modifier.padding(10.dp), maxLines = 3)
                                }
                            }
                        }
                    }
                }

                if (f.versions.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Eyebrow("Other versions")
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        f.versions.forEach { v ->
                            Chip(v.label.replaceFirstChar { it.uppercase() }, { m.zoomUrl = v.url }, height = null, hPad = 12.dp, vPad = 6.dp, style = sans(13, 600), leading = Lucide.Image)
                        }
                    }
                }

                val episode = m.episodes.indexOfFirst { it.sketchUid == s.uid }
                if (episode >= 0) {
                    val e = m.episodes[episode]
                    Spacer(Modifier.height(20.dp))
                    PressRow({ m.openEp(episode) }, Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Sk.colors.surfaceSunken)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(36.dp).clip(Pill).background(Sk.colors.accentAlt), contentAlignment = Alignment.Center) { Icon(Lucide.Play, Sk.colors.textOnAccent, 14.dp) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                T("On the podcast", sans(12, 700, tracking = .04f), color = Sk.colors.textTertiary)
                                T(e.title, sans(15, 700, 1.3f), maxLines = 2)
                                T(e.code, mono(11), color = Sk.colors.textTertiary)
                            }
                        }
                    }
                }

                val links = remember(f) {
                    f.links.filter { it.target !is LinkTarget.ImageVersion && (it.target as? LinkTarget.Sketch)?.uid !in f.related }
                        .distinctBy { it.target }
                }
                if (links.isNotEmpty() || f.printsUrl != null) {
                    Spacer(Modifier.height(20.dp))
                    Eyebrow("Links in this sketch")
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        links.forEach { l -> LinkRow(l, m) }
                        f.printsUrl?.let { LinkRow(Link("Prints of this sketch", LinkTarget.External(it, LinkKind.Web)), m) }
                    }
                }

                if (f.tags.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Eyebrow("Tags")
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        f.tags.forEach { t ->
                            Chip(m.topicName(t.uid), { m.openTag(t.uid) }, height = null, hPad = 10.dp, vPad = 3.dp, style = sans(12, 600), fg = Sk.colors.textTertiary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun LinkRow(l: Link, m: AppModel) {
    val badge = badgeOf(l)
    val col = badgeColor(badge)
    val label = (l.target as? LinkTarget.Sketch)?.uid?.let { m.byUid[it]?.title } ?: l.label.ifBlank { hostOf(l) }
    PressRow({ m.follow(l) },
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Sk.colors.surfaceCard)
            .border(1.dp, Sk.colors.borderQuiet, RoundedCornerShape(12.dp))) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(badgeWash(badge)), contentAlignment = Alignment.Center) {
                Icon(if (badge.internal) Lucide.ArrowRight else Lucide.External, col, 13.dp, stroke = 2.2f)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                T(label, sans(15, 600, 1.35f), maxLines = 2)
                Spacer(Modifier.height(2.dp))
                T(badge.label + if (badge.internal) " · stays in the app" else " · ${hostOf(l)}", mono(11), color = col, maxLines = 1)
            }
        }
    }
}

/** One block of a post: paragraph, heading, list, preformatted text or an inline image. */
@Composable
private fun BlockView(b: Block, size: Int, style: LinkStyle, standfirst: Boolean, onLink: (Link) -> Unit) {
    when (b) {
        is Block.Paragraph -> {
            RichTextView(b.content, if (standfirst) sans(size + 2, 600, 1.5f) else sans(size, 400, 1.65f), style, onLink)
            Spacer(Modifier.height(14.dp))
        }
        is Block.Heading -> {
            Spacer(Modifier.height(8.dp))
            RichTextView(b.content, heading(when (b.level) { 1 -> 24; 2 -> 21; else -> 18 }, 1.3f), style, onLink)
            Spacer(Modifier.height(10.dp))
        }
        is Block.ListBlock -> {
            Column(Modifier.padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                b.items.forEachIndexed { i, item ->
                    Row {
                        T(if (b.ordered) "${i + 1}." else "•", sans(size, 700, 1.65f), Modifier.widthIn(min = if (b.ordered) 26.dp else 18.dp), Sk.colors.textTertiary)
                        Box(Modifier.weight(1f)) { RichTextView(item, sans(size, 400, 1.65f), style, onLink) }
                    }
                }
            }
        }
        is Block.Preformatted -> {
            Box(
                Modifier.fillMaxWidth().padding(bottom = 14.dp).clip(RoundedCornerShape(12.dp)).background(Sk.colors.surfaceSunken)
                    .horizontalScroll(rememberScrollState()).padding(12.dp),
            ) { T(b.content.text, mono(14).copy(lineHeight = 21.sp)) }
        }
        is Block.Image -> {
            ArtBlock(Modifier.fillMaxWidth().heightIn(min = 120.dp).padding(bottom = 14.dp), image = b.image, rendition = Rendition.Read, fit = true)
        }
        is Block.Unknown -> {}
    }
}

private const val ICON = "icon"

/**
 * Rich text with bold, italic and inline links. Badges sit on the card ground with a 1px border in
 * the kind colour and wrap line by line; the underlined style drops the ground. Every link ends
 * with an 11px glyph: an arrow when it stays in the app, the external mark otherwise.
 */
@Composable
private fun RichTextView(rt: RichText, base: TextStyle, style: LinkStyle, onLink: (Link) -> Unit) {
    val badge = style == LinkStyle.Badge
    val links = rt.spans.mapNotNull { sp -> (sp.style as? RichStyle.Hyperlink)?.let { sp to it.link } }
    val kinds = links.map { badgeOf(it.second) }
    val colors = kinds.map { badgeColor(it) }
    val (text, ranges) = remember(rt, badge, colors) {
        val ranges = mutableListOf<Triple<Int, Int, Color>>()
        buildAnnotatedString {
            val cuts = (listOf(0, rt.text.length) + rt.spans.flatMap { listOf(it.start, it.end) }).filter { it in 0..rt.text.length }.distinct().sorted()
            val linkStart = HashMap<Int, Int>()
            for ((a, z) in cuts.zipWithNext()) {
                if (a >= z) continue
                val covering = rt.spans.filter { it.start <= a && it.end >= z }
                val li = links.indexOfFirst { (sp, _) -> sp.start <= a && sp.end >= z }
                val weight = when {
                    covering.any { it.style == RichStyle.Strong } -> FontWeight.Bold
                    li >= 0 -> FontWeight.SemiBold
                    else -> null
                }
                val italic = covering.any { it.style == RichStyle.Em }
                val span = SpanStyle(
                    fontWeight = weight, fontStyle = if (italic) FontStyle.Italic else null,
                    color = if (li >= 0) colors[li] else Color.Unspecified,
                    textDecoration = if (li >= 0 && !badge) TextDecoration.Underline else null,
                )
                if (li >= 0) {
                    linkStart.getOrPut(li) { length }
                    withLink(LinkAnnotation.Clickable("l$li-$a") { onLink(links[li].second) }) { withStyle(span) { append(rt.text.substring(a, z)) } }
                    if (links[li].first.end == z) {
                        withLink(LinkAnnotation.Clickable("i$li") { onLink(links[li].second) }) { appendInlineContent("$ICON$li", "→") }
                        ranges += Triple(linkStart[li]!!, length, colors[li])
                    }
                } else {
                    withStyle(span) { append(rt.text.substring(a, z)) }
                }
            }
        } to ranges
    }
    val inline = links.indices.associate { i ->
        "$ICON$i" to InlineTextContent(Placeholder(14.sp, 11.sp, PlaceholderVerticalAlign.TextCenter)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                Icon(if (kinds[i].internal) Lucide.ArrowRight else Lucide.External, colors[i], 11.dp, stroke = 2.6f)
            }
        }
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val card = Sk.colors.surfaceCard
    val fontPx = base.fontSize
    BasicText(
        text,
        style = base.copy(color = Sk.colors.textPrimary),
        inlineContent = inline,
        onTextLayout = { layout = it },
        modifier = Modifier.fillMaxWidth().drawBehind {
            val l = layout ?: return@drawBehind
            if (!badge) return@drawBehind
            val h = fontPx.toPx() * 1.3f
            ranges.forEach { (s, e, col) ->
                val first = l.getLineForOffset(s)
                val last = l.getLineForOffset((e - 1).coerceAtLeast(s))
                for (line in first..last) {
                    val left = if (line == first) l.getHorizontalPosition(s, true) else l.getLineLeft(line)
                    val right = if (line == last) l.getHorizontalPosition(e, true).let { if (it <= left) l.getLineRight(line) else it } else l.getLineRight(line)
                    if (right - left < 2f) continue
                    // CSS `padding: 0 3px`: the box grows outward and the neighbouring spaces absorb it.
                    val pad = 3.dp.toPx()
                    val mid = (l.getLineTop(line) + l.getLineBottom(line)) / 2f
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

/** Mobile data + Images on Wi-Fi only: the image waits, and the line says which setting is why. */
@Composable
fun HeldPanel(onLoad: () -> Unit) {
    InkCard(Modifier.padding(16.dp).fillMaxWidth(), bg = Art.paper, shadowColor = Art.line) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            T("image held back: you're on mobile data ✎", hand(17), color = Art.caption)
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
            T("The words are here, but this drawing hasn't been downloaded or seen before, so there's no copy on the device. Connect and it'll load in a second.",
                sans(15, 400, 1.5f), color = Sk.colors.textSecondary)
            Spacer(Modifier.height(14.dp))
            InkButton(onRetry, height = 40.dp) { fg -> T("Try again", sans(15, 700), color = fg) }
        }
    }
}

/** Anchored action bar that does not scroll: Favourite · Share · Download · Save image. */
@Composable
fun DetailActions(m: AppModel) {
    val d = m.sketch ?: return
    val tablet = LocalTablet.current
    val fav = m.isFavourite(d.uid)
    val off = m.isOffline(d.uid)
    val gal = m.inGallery(d.uid)
    Column(Modifier.fillMaxWidth().background(Sk.colors.surfaceChrome), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(Sk.colors.ink))
        Column(Modifier.fillMaxWidth().readMeasure(tablet).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(m.pulseFor("fav") ?: if (fav) "Favourited" else "Favourite", if (fav) Lucide.StarFilled else Lucide.Star, fav, Modifier.weight(1f)) { m.toggleFavourite(d.uid) }
                ActionButton(m.pulseFor("share") ?: "Share", Lucide.Share, false, Modifier.weight(1f)) { m.share(d) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val running = m.dl == d.uid
                ActionButton(
                    m.pulseFor("dl") ?: if (off) "Downloaded" else "Download post", if (off) Lucide.Check else Lucide.Download, off,
                    Modifier.weight(1f), progress = if (running) m.pct / 100f else null,
                ) { m.toggleDownload(d.uid) }
                ActionButton(m.pulseFor("gal") ?: if (gal) "In gallery" else "Save image", if (gal) Lucide.Check else Lucide.Image, gal, Modifier.weight(1f)) { m.toggleGallery(d.uid) }
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
fun ZoomViewer(m: AppModel, url: String) {
    val d = m.sketch
    var scale by remember(url) { mutableFloatStateOf(1f) }
    var offset by remember(url) { mutableStateOf(Offset.Zero) }
    val tf = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 6f)
        offset = if (scale == 1f) Offset.Zero else offset + pan
    }
    Column(
        Modifier.fillMaxSize().background(Art.viewer).pointerInput(url) {
            detectTapGestures(onDoubleTap = { scale = if (scale > 1f) 1f else 2.5f; offset = Offset.Zero }, onTap = { m.zoomUrl = null })
        },
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            T("${d?.num.orEmpty()} · pinch to zoom", mono(13), color = Color(0xBFFBF8DE))
            Icon(Lucide.Close, Art.page, 22.dp)
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp).transformable(tf), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = url, contentDescription = d?.imageAlt, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth()
                    .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y }
                    .background(Art.page),
            )
        }
        T("${d?.title.orEmpty()}\nImage: Jono Hey, Sketchplanations", sans(14, 600, 1.4f), Modifier.padding(horizontal = 16.dp, vertical = 18.dp), Color(0xD9FBF8DE))
    }
}
