package org.rdcl.nox.sketchplanationsreader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.rdcl.nox.sketchplanationsreader.state.AppModel
import org.rdcl.nox.sketchplanationsreader.ui.components.Icon
import org.rdcl.nox.sketchplanationsreader.ui.components.InkButton
import org.rdcl.nox.sketchplanationsreader.ui.components.Lucide
import org.rdcl.nox.sketchplanationsreader.ui.components.T
import org.rdcl.nox.sketchplanationsreader.ui.components.noRippleClick
import org.rdcl.nox.sketchplanationsreader.ui.num
import org.rdcl.nox.sketchplanationsreader.ui.theme.Art
import org.rdcl.nox.sketchplanationsreader.ui.theme.mono
import org.rdcl.nox.sketchplanationsreader.ui.theme.sans
import kotlin.math.abs

/**
 * Full-screen image. Pinch or double-tap to zoom, drag to pan while zoomed. At normal size, push the
 * picture up or down to close it. A single tap hides the bars.
 */
@Composable
fun ZoomViewer(m: AppModel, url: String) {
    val d = m.sketch
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var scale by remember(url) { mutableFloatStateOf(1f) }
    var offset by remember(url) { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var chrome by remember(url) { mutableStateOf(true) }
    val drag = remember(url) { Animatable(0f) }
    val enter = remember(url) { Animatable(.92f) }
    LaunchedEffect(url) { enter.animateTo(1f, spring(dampingRatio = .7f, stiffness = 500f)) }
    val dismissAt = with(density) { 120.dp.toPx() }

    fun clamp(o: Offset, s: Float): Offset {
        val maxX = size.width * (s - 1) / 2
        val maxY = size.height * (s - 1) / 2
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    fun close(direction: Float) = scope.launch {
        drag.animateTo(direction * size.height.coerceAtLeast(1), tween(180))
        m.zoomUrl = null
    }

    val fade = (1f - abs(drag.value) / (size.height.coerceAtLeast(1) * .6f)).coerceIn(0f, 1f)
    Box(Modifier.fillMaxSize().background(Art.viewer.copy(alpha = fade))) {
        Box(
            Modifier.fillMaxSize().onSizeChanged { size = it }
                .pointerInput(url) {
                    detectTapGestures(
                        onTap = { chrome = !chrome },
                        onDoubleTap = { at ->
                            val from = scale
                            val to = if (scale > 1f) 1f else 2.5f
                            val start = offset
                            // Zoom in towards the tapped point; zooming out recentres.
                            val target = if (to == 1f) Offset.Zero else clamp((Offset(size.width / 2f, size.height / 2f) - at) * (to - 1), to)
                            scope.launch {
                                animate(0f, 1f, animationSpec = tween(220)) { t, _ ->
                                    scale = from + (to - from) * t
                                    offset = start + (target - start) * t
                                }
                            }
                        },
                    )
                }
                .pointerInput(url) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val tracker = VelocityTracker()
                        var pulling = false
                        do {
                            val event = awaitPointerEvent()
                            val fingers = event.changes.count { it.pressed }
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            if (scale > 1f || fingers > 1 || zoom != 1f) {
                                val s = (scale * zoom).coerceIn(1f, 6f)
                                offset = if (s == 1f) Offset.Zero else clamp(offset + pan, s)
                                scale = s
                                if (event.changes.any { it.positionChanged() }) event.changes.forEach { it.consume() }
                            } else if (pan != Offset.Zero && (pulling || abs(pan.y) > abs(pan.x))) {
                                pulling = true
                                scope.launch { drag.snapTo(drag.value + pan.y) }
                                event.changes.firstOrNull()?.let { tracker.addPosition(it.uptimeMillis, it.position) }
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                        if (pulling) {
                            val v = tracker.calculateVelocity().y
                            if (abs(drag.value) > dismissAt || abs(v) > 2500f) close(if (drag.value + v / 10 >= 0) 1f else -1f)
                            else scope.launch { drag.animateTo(0f, spring(dampingRatio = .6f, stiffness = 600f)) }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = url, contentDescription = d?.imageAlt, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    .graphicsLayer {
                        val s = scale * enter.value * (1f - abs(drag.value) / (size.height.coerceAtLeast(1) * 4f))
                        scaleX = s; scaleY = s
                        translationX = offset.x
                        translationY = offset.y + drag.value
                    }
                    .background(Art.page),
            )
        }

        AnimatedVisibility(chrome && drag.value == 0f, Modifier.align(Alignment.TopCenter), enter = fadeIn(), exit = fadeOut()) {
            Row(
                Modifier.fillMaxWidth().background(Art.viewer.copy(alpha = .7f)).safeDrawingPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                T("${d?.num.orEmpty()} · pinch to zoom, swipe to close", mono(13), color = Color(0xBFFBF8DE))
                Box(Modifier.noRippleClick({ m.zoomUrl = null }).padding(6.dp)) { Icon(Lucide.Close, Art.page, 22.dp) }
            }
        }
        AnimatedVisibility(chrome && drag.value == 0f, Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
            Row(
                Modifier.fillMaxWidth().background(Art.viewer.copy(alpha = .7f)).safeDrawingPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    T(d?.title.orEmpty(), sans(14, 700, 1.4f), color = Color(0xD9FBF8DE))
                    T("Image: Jono Hey, Sketchplanations", sans(13, 500, 1.4f), color = Color(0xA6FBF8DE))
                }
                if (d != null) {
                    val saved = m.imageSaved(d.uid, url)
                    Spacer(Modifier.width(12.dp))
                    InkButton(
                        { m.saveToGallery(d.uid, url) }, bg = if (saved) Art.viewer else Art.page, fg = if (saved) Art.page else Art.line,
                        border = Art.page, shadowColor = Art.page, height = 40.dp, padding = 14.dp,
                    ) { fg ->
                        Icon(if (saved) Lucide.Check else Lucide.Image, fg, 16.dp)
                        Spacer(Modifier.width(6.dp))
                        T(m.pulseFor("gal") ?: if (saved) "In gallery" else "Save to gallery", sans(13, 700), color = fg, maxLines = 1)
                    }
                }
            }
        }
    }
}
