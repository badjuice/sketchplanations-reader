package org.rdcl.nox.sketchplanationsreader.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import org.rdcl.nox.sketchplanations.explode.Images
import org.rdcl.nox.sketchplanations.explode.Rendition
import org.rdcl.nox.sketchplanations.explode.SketchImage
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.rdcl.nox.sketchplanationsreader.ui.theme.Art
import org.rdcl.nox.sketchplanationsreader.ui.theme.EaseBounce
import org.rdcl.nox.sketchplanationsreader.ui.theme.EaseOut
import org.rdcl.nox.sketchplanationsreader.ui.theme.Radius
import org.rdcl.nox.sketchplanationsreader.ui.theme.Shadow
import org.rdcl.nox.sketchplanationsreader.ui.theme.Sk
import org.rdcl.nox.sketchplanationsreader.ui.theme.hand
import org.rdcl.nox.sketchplanationsreader.ui.theme.mono
import org.rdcl.nox.sketchplanationsreader.ui.theme.sans
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val Pill = RoundedCornerShape(9999.dp)

/** Phone vs tablet. Tablet pins the drawer, drops the tabs and runs lists two-up. */
val LocalTablet = staticCompositionLocalOf { false }

/**
 * Thumbnail framing: false crops to the slot (top-anchored), true letterboxes the whole sketch so
 * the Sketchplanations mark stays in view.
 */
val LocalThumbWhole = staticCompositionLocalOf { false }

/** Resolves what to load for an image: a downloaded file when there is one, else its URL. */
val LocalImageSource = staticCompositionLocalOf<(SketchImage, Rendition) -> String> { { i, r -> Images.url(i, r) } }

/** Reading measure on wide screens: prose holds ~68 characters and centres. */
val ReadWidth = 600.dp

fun Modifier.readMeasure(tablet: Boolean) = if (tablet) this.widthIn(max = ReadWidth) else this

/** A hard offset shadow with no blur, like a sticker on paper. */
fun Modifier.hardShadow(offset: Dp, color: Color, shape: Shape) = drawBehind {
    if (offset > 0.dp) {
        val o = offset.toPx()
        val outline = shape.createOutline(size, layoutDirection, this)
        translate(o, o) { drawOutline(outline, color) }
    }
}

fun Modifier.noRippleClick(onClick: () -> Unit, interaction: MutableInteractionSource? = null) = this.then(
    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
)

// ---- long-press tooltips ------------------------------------------------------------------

@Stable
class HintState {
    var label by mutableStateOf<String?>(null)
    /** Anchor in root coordinates: horizontal centre of the button, 6dp under its bottom edge. */
    var x by mutableStateOf(0f)
    var y by mutableStateOf(0f)
}

val LocalHints = staticCompositionLocalOf { HintState() }

/**
 * Tap runs [onClick]; holding for ~480ms shows [label] anchored directly under the button
 * (Android's standard tooltip, not a centred toast) and clears it on release.
 */
@Composable
fun Modifier.tapOrHold(label: String, enabled: Boolean = true, onPress: (Boolean) -> Unit = {}, onClick: () -> Unit): Modifier {
    val hints = LocalHints.current
    val scope = rememberCoroutineScope()
    var bounds by remember { mutableStateOf(Rect.Zero) }
    return this
        .onGloballyPositioned { bounds = it.boundsInRoot() }
        .semantics {
            contentDescription = label
            role = Role.Button
            onClick { if (enabled) onClick(); true }
        }
        .pointerInput(label, enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                awaitFirstDown()
                onPress(true)
                var held = false
                val gap = 6.dp.toPx()
                val job: Job = scope.launch {
                    delay(480)
                    held = true
                    hints.x = bounds.center.x
                    hints.y = bounds.bottom + gap
                    hints.label = label
                }
                val up = waitForUpOrCancellation()
                job.cancel()
                onPress(false)
                if (held) hints.label = null else if (up != null) onClick()
            }
        }
}

// ---- buttons ------------------------------------------------------------------------------

/**
 * The design system's primary and card buttons: 2px ink edge and a hard offset shadow.
 * Pressing translates 1px down-right and drops the shadow.
 */
@Composable
fun InkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bg: Color = Sk.colors.accentAlt,
    fg: Color = Sk.colors.textOnAccent,
    border: Color = Sk.colors.borderDefault,
    shadowColor: Color = Sk.colors.shadowInk,
    shape: Shape = Pill,
    shadow: Dp = Shadow.sm,
    height: Dp? = 44.dp,
    padding: Dp = 20.dp,
    arrangement: Arrangement.Horizontal = Arrangement.Center,
    enabled: Boolean = true,
    label: String? = null,
    /** Download progress, 0..1: a light wash fills the button's own ground left to right. */
    progress: Float? = null,
    content: @Composable RowScope.(Color) -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    var held by remember { mutableStateOf(false) }
    val down = (pressed || held) && enabled
    val fillColor = Sk.colors.infoWash
    val nudge = if (down) 1.dp else 0.dp
    val base = modifier
        .offset(nudge, nudge)
        .hardShadow(if (down) 0.dp else shadow, shadowColor, shape)
        .clip(shape)
        .background(bg)
        .then(if (progress != null) Modifier.drawBehind {
            drawRect(fillColor, size = androidx.compose.ui.geometry.Size(size.width * progress, size.height))
        } else Modifier)
        .border(2.dp, border, shape)
    val click = if (label != null) base.tapOrHold(label, enabled, { held = it }, onClick)
    else base.clickable(src, null, enabled = enabled, role = Role.Button, onClick = onClick)
    Row(
        click.then(if (height != null) Modifier.height(height) else Modifier.heightIn(min = 44.dp)).padding(horizontal = padding),
        horizontalArrangement = arrangement, verticalAlignment = Alignment.CenterVertically,
    ) {
        content(fg)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Lucide? = null, trailing: Lucide? = null, size: Int = 16) {
    InkButton(onClick, modifier) { fg ->
        if (icon != null) { Icon(icon, fg, 16.dp); Spacer(Modifier.width(8.dp)) }
        BasicText(text, style = sans(size, 700).copy(color = fg), maxLines = 1)
        if (trailing != null) { Spacer(Modifier.width(8.dp)); Icon(trailing, fg, 18.dp) }
    }
}

/** Card-coloured pill with the label left and a glyph right. Used for every "go somewhere" row. */
@Composable
fun CardPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, trailing: Lucide = Lucide.Chevron, size: Int = 16, trailingSize: Dp = 18.dp) {
    InkButton(
        onClick, modifier.fillMaxWidth(), bg = Sk.colors.surfaceCard, fg = Sk.colors.textPrimary,
        arrangement = Arrangement.SpaceBetween, padding = if (trailing == Lucide.External) 16.dp else 20.dp,
    ) { fg ->
        BasicText(text, style = sans(size, 700).copy(color = fg), modifier = Modifier.weight(1f), maxLines = 2)
        Icon(trailing, fg, trailingSize, stroke = if (trailing == Lucide.External) 2.2f else 2f)
    }
}

data class SegColors(val bg: Color, val fg: Color, val border: Color)

@Composable
fun seg(on: Boolean) = if (on) SegColors(Sk.colors.ink, Sk.colors.paperTint, Sk.colors.ink)
else SegColors(Color.Transparent, Sk.colors.textSecondary, Sk.colors.borderQuiet)

/** Quiet outline chip / segmented option. Presses scale to 0.96 with the bounce ease. */
@Composable
fun Chip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    on: Boolean = false,
    height: Dp? = 36.dp,
    hPad: Dp = 16.dp,
    vPad: Dp = 0.dp,
    style: TextStyle = sans(14, 600),
    fg: Color? = null,
    count: String? = null,
    countStyle: TextStyle = mono(13),
    leading: Lucide? = null,
    centered: Boolean = true,
) {
    val c = seg(on)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .96f else 1f, tween(Sk.motion.fast, easing = EaseBounce), label = "chip")
    val bg by animateColorAsState(if (pressed && !on) Sk.colors.accentAltWash else c.bg, tween(Sk.motion.fast, easing = EaseOut), label = "chipbg")
    val textColor = fg ?: c.fg
    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(Pill)
            .background(bg)
            .border(1.dp, c.border, Pill)
            .clickable(src, null, role = Role.Button, onClick = onClick)
            .then(if (height != null) Modifier.height(height) else Modifier)
            .padding(horizontal = hPad, vertical = vPad),
        horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) { Icon(leading, textColor, 15.dp); Spacer(Modifier.width(6.dp)) }
        BasicText(text, style = style.copy(color = textColor), maxLines = 1)
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            BasicText(count, style = countStyle.copy(color = Sk.colors.textTertiary), maxLines = 1)
        }
    }
}

/** 52×30 switch; on is blue with a cream knob. */
@Composable
fun Toggle(on: Boolean, onClick: () -> Unit, label: String) {
    val bg by animateColorAsState(if (on) Sk.colors.accentAlt else Color.Transparent, tween(Sk.motion.fast, easing = EaseOut), label = "tg")
    val knobX by animateFloatAsState(if (on) 1f else 0f, tween(Sk.motion.fast, easing = EaseOut), label = "knob")
    Box(
        Modifier
            .size(52.dp, 30.dp)
            .clip(Pill)
            .background(bg)
            .border(2.dp, Sk.colors.borderDefault, Pill)
            .semantics { contentDescription = label; role = Role.Switch }
            .noRippleClick(onClick)
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .offset { IntOffset((knobX * 22.dp.toPx()).toInt(), 0) }
                .size(22.dp)
                .clip(CircleShape)
                .background(if (on) Sk.colors.textOnAccent else Sk.colors.textTertiary)
                .align(Alignment.CenterStart),
        )
    }
}

/** Red underlined text button. */
@Composable
fun TextLink(text: String, onClick: () -> Unit, style: TextStyle = sans(15, 700), color: Color = Sk.colors.textLink) {
    BasicText(
        text,
        style = style.copy(color = color, textDecoration = TextDecoration.Underline),
        modifier = Modifier
            .semantics { role = Role.Button }
            .noRippleClick(onClick)
            .heightIn(min = 32.dp)
            .padding(vertical = 6.dp),
    )
}

/** 44px circular icon target with a blue wash while pressed. */
@Composable
fun BarIcon(icon: Lucide, label: String, onClick: () -> Unit, color: Color = Sk.colors.textPrimary) {
    var held by remember { mutableStateOf(false) }
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (held) Sk.colors.accentAltWash else Color.Transparent)
            .tapOrHold(label, onPress = { held = it }, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, color, 22.dp) }
}

// ---- text helpers -------------------------------------------------------------------------

@Composable
fun T(text: String, style: TextStyle, modifier: Modifier = Modifier, color: Color = Sk.colors.textPrimary, maxLines: Int = Int.MAX_VALUE) {
    BasicText(text, modifier, style = style.copy(color = color), maxLines = maxLines, overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis)
}

/** 12px/700 uppercase eyebrow label. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    T(text.uppercase(), sans(12, 700, tracking = .08f), modifier, Sk.colors.textTertiary)
}

/** Yellow highlighter swipe behind Patrick Hand. The type itself is never yellow. */
@Composable
fun Highlighter(text: String) {
    BasicText(
        text,
        style = hand(18).copy(color = Art.line),
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Sk.colors.penYellow).padding(horizontal = 10.dp, vertical = 2.dp),
    )
}

@Composable
fun InkRule(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(2.dp).background(Sk.colors.ink))

@Composable
fun Hairline(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(1.dp).background(Sk.colors.borderQuiet))

// ---- artwork ------------------------------------------------------------------------------

/**
 * A sketch's artwork. Always on cream, always ink-lined, in both themes. Artwork never inverts,
 * tints or takes a shadow of its own colour.
 *
 * With an [image], the [rendition] is fetched (or served from the disk cache when offline). A
 * [framed] slot follows the Thumbnails setting: cropped fills the slot anchored at the top, where
 * the idea sits; whole letterboxes the sketch on the page ground so the logo stays in view. [fit]
 * always shows the whole image. The [caption] shows while there is no picture.
 */
@Composable
fun ArtBlock(
    modifier: Modifier = Modifier,
    radius: Dp = Radius.thumb,
    border: Boolean = true,
    caption: String? = null,
    framed: Boolean = false,
    image: SketchImage? = null,
    rendition: Rendition = Rendition.Thumb,
    fit: Boolean = false,
    content: @Composable () -> Unit = {},
) {
    val shape = RoundedCornerShape(radius)
    val whole = fit || (framed && LocalThumbWhole.current)
    var shown by remember(image?.url) { mutableStateOf(false) }
    Box(
        modifier
            .clip(shape)
            .background(if (whole) Art.page else Art.paper)
            .then(if (border) Modifier.border(2.dp, Art.line, shape) else Modifier),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (image != null) {
            val r = when {
                !whole -> rendition
                rendition == Rendition.Thumb -> Rendition.ThumbWhole
                rendition == Rendition.Card -> Rendition.Read
                else -> rendition
            }
            AsyncImage(
                model = LocalImageSource.current(image, r),
                contentDescription = image.alt,
                contentScale = if (whole) ContentScale.Fit else ContentScale.Crop,
                alignment = if (whole) Alignment.Center else Alignment.TopCenter,
                onState = { shown = it is AsyncImagePainter.State.Success },
                modifier = Modifier.fillMaxSize().padding(if (border) 2.dp else 0.dp),
            )
        }
        if (caption != null && !shown) T(caption, hand(17), Modifier.padding(12.dp), Art.caption)
        content()
    }
}

/** Status tags on a thumbnail: is the post here (POST/HD), is the image in the gallery (IMG). */
@Composable
fun Thumb(width: Dp, height: Dp, image: SketchImage?, offline: Boolean = false, gallery: Boolean = false, hd: Boolean = true) {
    Box(Modifier.size(width, height)) {
        ArtBlock(Modifier.fillMaxWidth().fillMaxHeight(), framed = true, image = image)
        if (offline || gallery) {
            Row(Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (gallery) ThumbTag("IMG", Sk.colors.penGreen, Sk.colors.ink)
                if (offline) ThumbTag(if (hd) "HD" else "POST", Sk.colors.ink, Sk.colors.paperTint)
            }
        }
    }
}

@Composable
private fun ThumbTag(text: String, bg: Color, fg: Color) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        Modifier.height(17.dp).clip(shape).background(bg).border(2.dp, Sk.colors.surfaceApp, shape).padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) { T(text, sans(9, 800, tracking = .05f), color = fg) }
}

/** A list row with a blue wash while pressed. */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun PressRow(onClick: () -> Unit, modifier: Modifier = Modifier, onLongClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (pressed) Sk.colors.accentAltWash else Color.Transparent)
            .combinedClickable(src, null, role = Role.Button, onLongClick = onLongClick, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Archive-style sketch row: 78×62 thumbnail, title, `#num · date`, optional Offline pill. */
@Composable
fun SketchRow(
    title: String, meta: String, onClick: () -> Unit, image: SketchImage? = null,
    offline: Boolean = false, gallery: Boolean = false, hd: Boolean = true,
    showPill: Boolean = false, titleSize: Int = 16, thumbW: Dp = 78.dp, thumbH: Dp = 62.dp,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column {
        PressRow(onClick, Modifier.fillMaxWidth().padding(vertical = 0.dp)) {
            Row(Modifier.padding(horizontal = 6.dp, vertical = 12.dp).weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Thumb(thumbW, thumbH, image, offline, gallery, hd)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    T(title, sans(titleSize, 700, 1.3f))
                    Spacer(Modifier.height(4.dp))
                    T(meta, mono(12), color = Sk.colors.textTertiary)
                }
                if (showPill && offline) {
                    Spacer(Modifier.width(8.dp))
                    T("Offline", sans(12, 700), Modifier.clip(Pill).background(Sk.colors.infoWash).padding(horizontal = 12.dp, vertical = 4.dp), Sk.colors.penBlueDeep)
                }
                if (trailing != null) { Spacer(Modifier.width(8.dp)); trailing() }
            }
        }
        Hairline()
    }
}

/** Circular trash button with a red wash while pressed. */
@Composable
fun DeleteButton(label: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (pressed) Sk.colors.errorWash else Color.Transparent)
            .semantics { contentDescription = label }
            .clickable(src, null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(Lucide.Trash, if (pressed) Sk.colors.error else Sk.colors.textTertiary, 17.dp) }
}

/** Two items per row on tablet, one on phone. */
@Composable
fun <T> Columns(items: List<T>, gap: Dp = 24.dp, item: @Composable (T) -> Unit) {
    if (!LocalTablet.current) { items.forEach { item(it) }; return }
    items.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            pair.forEach { Box(Modifier.weight(1f)) { item(it) } }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun EmptyState(title: String, body: String, cta: String? = null, onCta: () -> Unit = {}, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 44.dp)) {
        T(title, sans(24, 800, 1.25f))
        Spacer(Modifier.height(8.dp))
        T(body, sans(16, 400, 1.5f), color = Sk.colors.textSecondary)
        if (cta != null) {
            Spacer(Modifier.height(20.dp))
            PrimaryButton(cta, onCta)
        }
    }
}

/** Bordered card with the 2px ink edge and a hard offset shadow. */
@Composable
fun InkCard(
    modifier: Modifier = Modifier,
    bg: Color = Sk.colors.surfaceCard,
    shadow: Dp = Shadow.sm,
    shadowColor: Color = Sk.colors.shadowInk,
    radius: Dp = Radius.card,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Box(modifier.hardShadow(shadow, shadowColor, shape).clip(shape).background(bg).border(BorderStroke(2.dp, Sk.colors.borderDefault), shape)) {
        content()
    }
}
