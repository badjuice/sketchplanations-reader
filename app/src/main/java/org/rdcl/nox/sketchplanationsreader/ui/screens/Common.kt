package org.rdcl.nox.sketchplanationsreader.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.rdcl.nox.sketchplanationsreader.state.AppModel

/** A screen that scrolls, and scrolls home when its tab is re-tapped. */
@Composable
fun ScreenScroll(
    m: AppModel,
    modifier: Modifier = Modifier,
    state: ScrollState = rememberScrollState(),
    align: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tick = remember { m.scrollTopTick }
    LaunchedEffect(m.scrollTopTick) { if (m.scrollTopTick != tick) state.animateScrollTo(0) }
    Column(modifier.fillMaxSize().verticalScroll(state), horizontalAlignment = align, content = content)
}
