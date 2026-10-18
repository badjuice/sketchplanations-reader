package com.sketchplanations.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sketchplanations.reader.data.Catalogue
import com.sketchplanations.reader.data.HomeLayout
import com.sketchplanations.reader.data.Sketch
import com.sketchplanations.reader.state.AppModel
import com.sketchplanations.reader.state.Screen
import com.sketchplanations.reader.ui.components.ArtBlock
import com.sketchplanations.reader.ui.components.CardPill
import com.sketchplanations.reader.ui.components.Chip
import com.sketchplanations.reader.ui.components.Highlighter
import com.sketchplanations.reader.ui.components.Icon
import com.sketchplanations.reader.ui.components.InkButton
import com.sketchplanations.reader.ui.components.InkRule
import com.sketchplanations.reader.ui.components.Lucide
import com.sketchplanations.reader.ui.components.Pill
import com.sketchplanations.reader.ui.components.PressRow
import com.sketchplanations.reader.ui.components.PrimaryButton
import com.sketchplanations.reader.ui.components.T
import com.sketchplanations.reader.ui.components.TextLink
import com.sketchplanations.reader.ui.components.Hairline
import com.sketchplanations.reader.ui.components.hardShadow
import com.sketchplanations.reader.ui.theme.Art
import com.sketchplanations.reader.ui.theme.Radius
import com.sketchplanations.reader.ui.theme.Shadow
import com.sketchplanations.reader.ui.theme.Sk
import com.sketchplanations.reader.ui.theme.hand
import com.sketchplanations.reader.ui.theme.heading
import com.sketchplanations.reader.ui.theme.mono
import com.sketchplanations.reader.ui.theme.sans
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun TodayScreen(m: AppModel) {
    val deck = Catalogue.byDate.take(5)
    ScreenScroll(m) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 12.dp)) {
            T("Today's sketch", heading(26))
            Spacer(Modifier.height(2.dp))
            T("${deck[0].date} · a new one every week 🚀", hand(18), color = Sk.colors.textSecondary)
        }

        if (m.settings.homeLayout == HomeLayout.Deck) Deck(m, deck)
        else Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            deck.forEachIndexed { i, s -> SketchCard(s, i == 0, Modifier.fillMaxWidth(), 212.dp, "sketch ${s.num} ✎") { m.open(s.n) } }
        }

        InkRule()
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            T("Popular topics", sans(21, 800, 1.3f))
            TextLink("All topics", { m.go(Screen.Categories) })
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Catalogue.topics.take(6).forEach { c ->
                Chip(c.name, { m.openTag(c.name) }, height = null, hPad = 14.dp, vPad = 8.dp, style = sans(15, 600), count = c.count.toString())
            }
        }

        InkRule()
        T("From the archive", sans(21, 800, 1.3f), Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp))
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp)) {
            Catalogue.byDate.drop(5).take(4).forEach { s ->
                PressRow({ m.open(s.n) }, Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 4.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ArtBlock(Modifier.size(68.dp, 54.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            T(s.title, sans(16, 700, 1.3f), maxLines = 1)
                            Spacer(Modifier.height(3.dp))
                            T("${s.num} · ${s.date}", mono(12), color = Sk.colors.textTertiary)
                        }
                        Icon(Lucide.Chevron, Sk.colors.textTertiary, 20.dp)
                    }
                }
                Hairline()
            }
        }

        // The podcast band follows the theme — it does not invert.
        val latest = Catalogue.episodes[0]
        InkRule()
        Column(Modifier.fillMaxWidth().background(Sk.colors.surfaceSunken).padding(horizontal = 16.dp, vertical = 22.dp)) {
            Highlighter("on the podcast this week 🎧")
            Spacer(Modifier.height(8.dp))
            T(latest.title, heading(26, 1.2f))
            Spacer(Modifier.height(6.dp))
            T("Episode ${latest.num} · ${latest.len} · with Jono, Rob and Tom", sans(15, 400, 1.5f), color = Sk.colors.textSecondary)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Play episode", { m.playLatest() }, Modifier.weight(1f), icon = Lucide.Play)
                GhostButton("All episodes") { m.go(Screen.Podcast) }
            }
        }
        InkRule()

        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 28.dp)) {
            T("Combining 🧠 curiosity, ♥️ heart, and ✍️ art since 2013", hand(19, 1.4f, italic = true).copy(textAlign = TextAlign.Center), Modifier.fillMaxWidth(), Sk.colors.textSecondary)
            Spacer(Modifier.height(16.dp))
            T("One idea, explained in one sketch, every week. The newsletter lives in About; prints and the book have their own page.", sans(16, 400, 1.5f), color = Sk.colors.textSecondary)
            Spacer(Modifier.height(14.dp))
            CardPill("About this app", { m.go(Screen.About) })
        }
    }
}

/** Transparent pill with a red wash while pressed. */
@Composable
fun GhostButton(text: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    Box(
        Modifier.height(44.dp).clip(Pill).background(if (pressed) Sk.colors.accentWash else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(src, null, role = Role.Button, onClick = onClick).padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) { T(text, sans(16, 700), color = if (pressed) Sk.colors.textPrimary else Sk.colors.textSecondary) }
}

@Composable
private fun Deck(m: AppModel, deck: List<Sketch>) {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val fling = rememberSnapFlingBehavior(state, SnapPosition.Center)
    val centred by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - mid) }?.index ?: 0
        }
    }
    LaunchedEffect(state) { snapshotFlow { centred }.collect { m.deckIdx = it } }

    LazyRow(
        state = state,
        flingBehavior = fling,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        itemsIndexed(deck, key = { _, s -> s.n }) { i, s ->
            // Leave room for the 3px hard shadow below and right.
            Box(Modifier.padding(end = 3.dp, bottom = 3.dp)) {
                SketchCard(s, i == 0, Modifier.width(324.dp), 244.dp, "sketch ${s.num} · drop image ✎") { m.open(s.n) }
            }
        }
    }
    // A clickable stepper: five 44px targets each holding a 4px bar.
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        deck.forEachIndexed { i, s ->
            Box(
                Modifier.weight(1f).height(44.dp).semantics { contentDescription = s.title }
                    .clickable(remember { MutableInteractionSource() }, null, role = Role.Button) {
                        scope.launch { state.animateScrollToItem(i) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.fillMaxWidth().height(4.dp).clip(Pill).background(if (i == m.deckIdx) Sk.colors.accentAlt else Sk.colors.rule))
            }
        }
    }
}

/** Deck/feed card. The newest carries the yellow offset; the rest the ink offset. */
@Composable
private fun SketchCard(s: Sketch, newest: Boolean, modifier: Modifier, artHeight: Dp, caption: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val shape = RoundedCornerShape(Radius.card)
    val nudge = if (pressed) 1.dp else 0.dp
    Column(
        modifier
            .offset(nudge, nudge)
            .hardShadow(Shadow.md, if (newest) Sk.colors.penYellow else Sk.colors.shadowInk, shape)
            .clip(shape)
            .background(Sk.colors.surfaceCard)
            .border(2.dp, Sk.colors.borderDefault, shape)
            .clickable(src, null, role = Role.Button, onClick = onClick),
    ) {
        ArtBlock(Modifier.fillMaxWidth().height(artHeight), radius = 0.dp, border = false, caption = caption)
        Box(Modifier.fillMaxWidth().height(2.dp).background(Art.line))
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 18.dp)) {
            T("${s.num} · ${s.date}", mono(12), color = Sk.colors.textTertiary)
            Spacer(Modifier.height(6.dp))
            T(s.title, heading(22))
            Spacer(Modifier.height(6.dp))
            T(s.lead, sans(15, 400, 1.5f), color = Sk.colors.textSecondary)
        }
    }
}
