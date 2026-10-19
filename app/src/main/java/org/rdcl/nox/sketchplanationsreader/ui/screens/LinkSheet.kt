package org.rdcl.nox.sketchplanationsreader.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import org.rdcl.nox.sketchplanations.explode.Link
import org.rdcl.nox.sketchplanations.explode.LinkTarget
import org.rdcl.nox.sketchplanationsreader.state.AppModel
import org.rdcl.nox.sketchplanationsreader.ui.components.Icon
import org.rdcl.nox.sketchplanationsreader.ui.components.InkButton
import org.rdcl.nox.sketchplanationsreader.ui.components.InkCard
import org.rdcl.nox.sketchplanationsreader.ui.components.Lucide
import org.rdcl.nox.sketchplanationsreader.ui.components.T
import org.rdcl.nox.sketchplanationsreader.ui.components.noRippleClick
import org.rdcl.nox.sketchplanationsreader.ui.theme.Sk
import org.rdcl.nox.sketchplanationsreader.ui.theme.mono
import org.rdcl.nox.sketchplanationsreader.ui.theme.sans

/** A long-pressed link: where it goes, and Open, Copy link or Share. */
@Composable
fun LinkSheet(m: AppModel, link: Link) {
    val url = m.urlOf(link)
    val clip = LocalClipboardManager.current
    val title = (link.target as? LinkTarget.Sketch)?.uid?.let { m.byUid[it]?.title } ?: link.label.ifBlank { url }
    val rise = remember(link) { Animatable(80f) }
    LaunchedEffect(link) { rise.animateTo(0f, spring(dampingRatio = .75f, stiffness = 700f)) }
    fun close() { m.linkSheet = null }
    Box(Modifier.fillMaxSize().background(Sk.colors.scrim).noRippleClick(::close), contentAlignment = Alignment.BottomCenter) {
        InkCard(
            Modifier.padding(12.dp).widthIn(max = 520.dp).fillMaxWidth()
                .graphicsLayer { translationY = rise.value.dp.toPx(); alpha = 1f - rise.value / 80f }
                .noRippleClick({}),
            bg = Sk.colors.surfaceOverlay,
        ) {
            Column(Modifier.padding(20.dp)) {
                T(title, sans(17, 800, 1.3f), maxLines = 3)
                Spacer(Modifier.height(6.dp))
                T(url, mono(13).copy(lineHeight = mono(13).fontSize * 1.4f), color = Sk.colors.textSecondary, maxLines = 4)
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SheetButton("Open", Lucide.External, Modifier.weight(1f)) { close(); m.follow(link) }
                    SheetButton("Copy link", Lucide.Link, Modifier.weight(1f)) {
                        clip.setText(AnnotatedString(url))
                        close()
                        m.flash("Link copied")
                    }
                    SheetButton("Share", Lucide.Share, Modifier.weight(1f)) { close(); m.shareUrl(url) }
                }
            }
        }
    }
}

@Composable
private fun SheetButton(label: String, icon: Lucide, modifier: Modifier, onClick: () -> Unit) {
    InkButton(onClick, modifier, bg = Sk.colors.surfaceCard, fg = Sk.colors.textPrimary, height = 44.dp, padding = 6.dp) { fg ->
        Icon(icon, fg, 16.dp)
        Spacer(Modifier.width(6.dp))
        T(label, sans(13, 700), color = fg, maxLines = 1)
    }
}
