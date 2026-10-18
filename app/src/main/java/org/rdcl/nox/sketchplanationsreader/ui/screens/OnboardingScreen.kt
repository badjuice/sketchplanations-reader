package org.rdcl.nox.sketchplanationsreader.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.rdcl.nox.sketchplanationsreader.data.Catalogue
import org.rdcl.nox.sketchplanationsreader.state.AppModel
import org.rdcl.nox.sketchplanationsreader.state.SyncState
import org.rdcl.nox.sketchplanationsreader.ui.components.Icon
import org.rdcl.nox.sketchplanationsreader.ui.components.InkButton
import org.rdcl.nox.sketchplanationsreader.ui.components.LocalTablet
import org.rdcl.nox.sketchplanationsreader.ui.components.Lucide
import org.rdcl.nox.sketchplanationsreader.ui.components.Pill
import org.rdcl.nox.sketchplanationsreader.ui.components.T
import org.rdcl.nox.sketchplanationsreader.ui.components.hardShadow
import org.rdcl.nox.sketchplanationsreader.ui.components.noRippleClick
import org.rdcl.nox.sketchplanationsreader.ui.components.readMeasure
import org.rdcl.nox.sketchplanationsreader.ui.theme.Art
import org.rdcl.nox.sketchplanationsreader.ui.theme.EaseOut
import org.rdcl.nox.sketchplanationsreader.ui.theme.Radius
import org.rdcl.nox.sketchplanationsreader.ui.theme.Shadow
import org.rdcl.nox.sketchplanationsreader.ui.theme.Sk
import org.rdcl.nox.sketchplanationsreader.ui.theme.hand
import org.rdcl.nox.sketchplanationsreader.ui.theme.heading
import org.rdcl.nox.sketchplanationsreader.ui.theme.mono
import org.rdcl.nox.sketchplanationsreader.ui.theme.sans

private class Step(val title: String, val body: String)

private val steps = listOf(
    Step("One idea, one sketch, every week.", "Jono Hey’s whole archive, laid out so you can actually find things. This is an unofficial reader — the sketches are his."),
    Step("Download post, or save the image.", "Download keeps the drawing and its words in the app for reading with no signal. Save image drops just the picture into your phone gallery."),
    Step("Favourite the ones that stick.", "A star marks what you want to come back to. Favourites and downloads are separate lists, so keeping a sketch never means storing it."),
    Step("Building your index.", "Archive and Topics need the list of every sketch before they can work. Titles and dates only — the drawings stay on the website until you ask for one."),
)

@Composable
fun OnboardingScreen(m: AppModel) {
    val tablet = LocalTablet.current
    val isSync = m.ob == 3
    val ss = m.syncState
    val fade = Sk.motion.base
    val cta = when {
        !isSync -> "Next"
        ss == SyncState.Running -> "Fetching…"
        ss == SyncState.Failed -> "Try again"
        ss == SyncState.Idle -> "Build the index"
        else -> "Start reading"
    }
    Column(
        Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        T("Sketchplanations", sans(16, 800), Modifier.fillMaxWidth(), Sk.colors.textLink)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).heightIn(min = maxHeight).fillMaxWidth().readMeasure(tablet).padding(vertical = 26.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            // One persistent art stage that never unmounts between steps — only its contents fade.
            val shape = RoundedCornerShape(Radius.card)
            Box(
                Modifier.height(230.dp).fillMaxWidth().then(if (tablet) Modifier.widthIn(max = 520.dp) else Modifier)
                    .hardShadow(Shadow.md, Sk.colors.shadowInk, shape).clip(shape).background(Art.paper).border(2.dp, Art.line, shape)
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                AnimatedContent(isSync, transitionSpec = { fadeIn(tween(fade, easing = EaseOut)) togetherWith fadeOut(tween(fade)) }, label = "stage") { sync ->
                    if (!sync) T("onboarding sketch · 4:3 ✎", hand(17), color = Art.caption)
                    else SyncStage(m)
                }
            }
            Spacer(Modifier.height(28.dp))
            AnimatedContent(m.ob, transitionSpec = { fadeIn(tween(fade)) togetherWith fadeOut(tween(fade)) }, label = "copy") { i ->
                Column {
                    T(steps[i].title, heading(32, 1.2f))
                    Spacer(Modifier.height(12.dp))
                    T(steps[i].body, sans(17, 400, 1.5f), color = Sk.colors.textSecondary)
                }
            }
        }
        }

        // Progress: display only — not clickable here.
        Row(Modifier.fillMaxWidth().readMeasure(tablet).padding(top = 12.dp, bottom = 28.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            steps.indices.forEach { i ->
                val c by animateColorAsState(if (i <= m.ob) Sk.colors.accentAlt else Sk.colors.rule, tween(Sk.motion.fast), label = "dot")
                Box(Modifier.weight(1f).height(4.dp).clip(Pill).background(c))
            }
        }
        // Height reserved on every step so nothing shifts when the skip link appears.
        Box(Modifier.fillMaxWidth().readMeasure(tablet).heightIn(min = 44.dp).padding(bottom = 6.dp), contentAlignment = Alignment.CenterStart) {
            if (isSync && ss != SyncState.Done) {
                T("Skip building the index", sans(14, 600).copy(textDecoration = TextDecoration.Underline), Modifier.noRippleClick({ m.askSkipIndex() }).padding(vertical = 12.dp), Sk.colors.textTertiary)
            }
        }
        Row(Modifier.fillMaxWidth().readMeasure(tablet), verticalAlignment = Alignment.CenterVertically) {
            // A fixed 44px slot: the back button appears from step 2 into reserved space.
            Box(Modifier.size(44.dp)) {
                if (m.ob > 0) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).border(2.dp, Sk.colors.borderQuiet, CircleShape).noRippleClick({ m.ob = (m.ob - 1).coerceAtLeast(0) }),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Lucide.Back, Sk.colors.textSecondary, 18.dp) }
                }
            }
            Spacer(Modifier.weight(1f))
            // Primary pill: at least half the width, grows to fit, never wraps.
            Box(Modifier.fillMaxWidth(.5f).widthIn(min = 0.dp), contentAlignment = Alignment.CenterEnd) {
                InkButton({ m.nextOnboarding() }, Modifier.fillMaxWidth(), padding = 22.dp) { fg ->
                    T(cta, sans(16, 700), color = fg, maxLines = 1)
                    Spacer(Modifier.width(8.dp))
                    Icon(Lucide.ArrowRight, fg, 18.dp)
                }
            }
        }
    }
}

@Composable
private fun SyncStage(m: AppModel) {
    val ss = m.syncState
    val (msg, note) = when (ss) {
        SyncState.Idle -> "Not built yet" to "Titles and dates for all ${Catalogue.TOTAL_POSTS} sketches. About ten requests, a few seconds."
        SyncState.Running -> "Fetching the catalogue" to "About ten requests. A few seconds."
        SyncState.Done -> "Catalogue ready" to "Archive, Topics and A–Z all work offline now."
        SyncState.Failed -> "No connection" to "Skip if you like — Today and Search still work online, and Archive will offer this again."
    }
    val blank = ss == SyncState.Failed || ss == SyncState.Idle
    val frac by animateFloatAsState(if (blank) 0f else m.syncN / Catalogue.TOTAL_POSTS.toFloat(), tween(240, easing = EaseOut), label = "sync")
    Column {
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            T(msg, sans(16, 800), color = when (ss) {
                SyncState.Failed -> Sk.colors.penOrangeInk
                SyncState.Idle -> Sk.colors.textTertiary
                else -> Sk.colors.textPrimary
            }.let { if (Sk.colors.isDark) Art.line else it })
            T(if (blank) "—" else "${m.syncN} of ${Catalogue.TOTAL_POSTS}", mono(14), color = Art.caption)
        }
        Box(Modifier.fillMaxWidth().height(14.dp).clip(Pill).background(Art.page).border(2.dp, Art.line, Pill)) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(Pill).background(Sk.colors.accentAlt))
        }
        Spacer(Modifier.height(11.dp))
        T(note, sans(14, 400, 1.5f), color = Art.caption)
    }
}
