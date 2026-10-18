package org.rdcl.nox.sketchplanationsreader.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.rdcl.nox.sketchplanationsreader.state.Confirm
import org.rdcl.nox.sketchplanationsreader.ui.components.InkButton
import org.rdcl.nox.sketchplanationsreader.ui.components.InkCard
import org.rdcl.nox.sketchplanationsreader.ui.components.T
import org.rdcl.nox.sketchplanationsreader.ui.components.noRippleClick
import org.rdcl.nox.sketchplanationsreader.ui.theme.EaseBounce
import org.rdcl.nox.sketchplanationsreader.ui.theme.EaseOut
import org.rdcl.nox.sketchplanationsreader.ui.theme.Sk
import org.rdcl.nox.sketchplanationsreader.ui.theme.mono
import org.rdcl.nox.sketchplanationsreader.ui.theme.sans
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ConfirmDialog(c: Confirm, onNo: () -> Unit, onYes: () -> Unit) {
    val pop = remember(c) { Animatable(.97f) }
    val dur = Sk.motion.bounce
    LaunchedEffect(c) { pop.animateTo(1f, tween(dur, easing = EaseBounce)) }
    Box(Modifier.fillMaxSize().background(Sk.colors.scrim).noRippleClick(onNo).padding(24.dp), contentAlignment = Alignment.Center) {
        InkCard(
            Modifier.widthIn(max = 480.dp).fillMaxWidth().graphicsLayer { scaleX = pop.value; scaleY = pop.value }.noRippleClick({}),
            bg = Sk.colors.surfaceOverlay, shadow = 0.dp,
        ) {
            Column(Modifier.padding(22.dp)) {
                T(c.title, sans(21, 800, 1.25f))
                Spacer(Modifier.height(8.dp))
                T(c.body, sans(15, 400, 1.5f), color = Sk.colors.textSecondary)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                    GhostButton("Cancel", onNo)
                    InkButton(onYes, bg = Sk.colors.error, fg = Color(0xFFFFFDF2)) { fg -> T(c.cta, sans(15, 700), color = fg) }
                }
            }
        }
    }
}

/** 👾 Settings › App › Version, tapped ten times. Respects Reduce motion. */
@Composable
fun EasterEgg(onClose: () -> Unit) {
    val still = Sk.motion.base == 0
    val pens = listOf(Sk.colors.penRed, Sk.colors.penBlue, Sk.colors.penYellow, Sk.colors.penGreen, Sk.colors.penOrange)
    val t = rememberInfiniteTransition(label = "egg")
    val pop = remember { Animatable(if (still) 1f else .2f) }
    LaunchedEffect(Unit) { if (!still) pop.animateTo(1f, tween(420, easing = EaseBounce)) }
    BoxWithConstraints(Modifier.fillMaxSize().background(Sk.colors.scrim).noRippleClick(onClose)) {
        val w = maxWidth.value
        val h = maxHeight.value
        // Particle burst
        for (i in 0 until 48) {
            val a = Math.toRadians(((i * 29) % 360).toDouble())
            val r = 8 + ((i * 41) % 70)
            val x = (50 + cos(a) * r) / 100 * w
            val y = (50 + sin(a) * r * .9) / 100 * h
            val dur = (900 + (i % 7) * 180)
            val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(dur, easing = EaseOut), RepeatMode.Restart, StartOffset((i % 12) * 50)), label = "d$i")
            val sz = 4 + (i % 4) * 3
            val prog = if (still) .3f else p
            Box(
                Modifier.offset(x.dp - (sz / 2).dp, y.dp - (sz / 2).dp).size(sz.dp)
                    .graphicsLayer {
                        val s = if (prog < .25f) prog / .25f else 1f - (prog - .25f) / .75f * .6f
                        scaleX = s; scaleY = s
                        alpha = if (prog < .25f) prog / .25f else 1f - (prog - .25f) / .75f
                        translationY = -120.dp.toPx() * prog
                    }
                    .clip(CircleShape).background(pens[i % 5]),
            )
        }
        // A scatter of smaller invaders
        for (i in 0 until 26) {
            val a = Math.toRadians((i * 137.5) % 360)
            val r = 18 + ((i * 53) % 60)
            val x = (50 + cos(a) * r) / 100 * w
            val y = (50 + sin(a) * r * .8) / 100 * h
            val dur = (1100 + (i % 6) * 220)
            val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(dur, easing = EaseOut), RepeatMode.Restart, StartOffset((i % 9) * 70)), label = "b$i")
            val prog = if (still) .3f else p
            val size = 10 + (i % 5) * 5
            BasicText(
                "👾",
                Modifier.offset(x.dp - (size / 2).dp, y.dp - (size / 2).dp).graphicsLayer {
                    val s = .3f + 1.3f * prog
                    scaleX = s; scaleY = s
                    alpha = if (prog < .3f) prog / .3f else 1f - (prog - .3f) / .7f
                    translationY = -70.dp.toPx() * prog
                },
                style = sans(size).copy(fontSize = size.sp),
            )
        }
        BasicText(
            "👾",
            Modifier.align(Alignment.Center).graphicsLayer { scaleX = pop.value; scaleY = pop.value; rotationZ = (1f - pop.value) * -12f },
            style = sans(96).copy(fontSize = 96.sp),
        )
        T("you found it · tap to dismiss", mono(13), Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp), Sk.colors.paperTint.copy(alpha = .85f))
    }
}
