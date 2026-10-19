package org.rdcl.nox.sketchplanationsreader.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.rdcl.nox.sketchplanationsreader.ui.theme.EaseOut
import org.rdcl.nox.sketchplanationsreader.ui.theme.Sk

/** True when Reduce motion is on: every animation here then jumps straight to its end. */
@Composable
fun still() = Sk.motion.base == 0

/**
 * Download progress inside a button: a wash that eases up to [progress] with diagonal stripes
 * marching through it, so a slow download still visibly moves.
 */
@Composable
fun Modifier.progressFill(progress: Float?, color: Color, stripe: Color): Modifier {
    if (progress == null) return this
    val still = still()
    val shown by animateFloatAsState(progress, if (still) snap() else tween(350, easing = EaseOut), label = "fill")
    val phase = if (still) 0f else rememberInfiniteTransition(label = "stripes")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(700, easing = LinearEasing)), label = "phase").value
    return drawBehind {
        val w = size.width * shown
        drawRect(color, size = Size(w, size.height))
        clipRect(right = w) {
            val gap = 14.dp.toPx()
            var x = -size.height - gap + phase * gap
            while (x < w) {
                drawLine(stripe, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = 5.dp.toPx())
                x += gap
            }
        }
    }
}

/** An icon that pops with a little overshoot whenever [key] changes, e.g. a star being filled. */
@Composable
fun PopIcon(icon: Lucide, color: Color, size: Dp, key: Any, stroke: Float = 2f) {
    val still = still()
    val s = remember { Animatable(1f) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(key) {
        if (first[0]) { first[0] = false; return@LaunchedEffect }
        if (still) return@LaunchedEffect
        s.snapTo(.55f)
        s.animateTo(1f, spring(dampingRatio = .35f, stiffness = 650f))
    }
    Icon(icon, color, size, stroke, Modifier.graphicsLayer { scaleX = s.value; scaleY = s.value; rotationZ = (1f - s.value) * -40f })
}
