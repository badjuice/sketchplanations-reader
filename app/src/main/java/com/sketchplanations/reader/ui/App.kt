package com.sketchplanations.reader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sketchplanations.reader.data.Catalogue
import com.sketchplanations.reader.state.AppModel
import com.sketchplanations.reader.state.Screen
import com.sketchplanations.reader.ui.components.BarIcon
import com.sketchplanations.reader.ui.components.Chip
import com.sketchplanations.reader.ui.components.Icon
import com.sketchplanations.reader.ui.components.LocalHints
import com.sketchplanations.reader.ui.components.LocalTablet
import com.sketchplanations.reader.ui.components.Lucide
import com.sketchplanations.reader.ui.components.Pill
import com.sketchplanations.reader.ui.components.PressRow
import com.sketchplanations.reader.ui.components.T
import com.sketchplanations.reader.ui.components.HintState
import com.sketchplanations.reader.ui.components.noRippleClick
import com.sketchplanations.reader.ui.screens.AboutScreen
import com.sketchplanations.reader.ui.screens.ArchiveScreen
import com.sketchplanations.reader.ui.screens.BrowseScreen
import com.sketchplanations.reader.ui.screens.CategoriesScreen
import com.sketchplanations.reader.ui.screens.ConfirmDialog
import com.sketchplanations.reader.ui.screens.DetailActions
import com.sketchplanations.reader.ui.screens.DetailScreen
import com.sketchplanations.reader.ui.screens.EasterEgg
import com.sketchplanations.reader.ui.screens.EpisodeScreen
import com.sketchplanations.reader.ui.screens.LibraryScreen
import com.sketchplanations.reader.ui.screens.OnboardingScreen
import com.sketchplanations.reader.ui.screens.PodLibraryScreen
import com.sketchplanations.reader.ui.screens.PodcastAboutScreen
import com.sketchplanations.reader.ui.screens.PodcastContactScreen
import com.sketchplanations.reader.ui.screens.PodcastScreen
import com.sketchplanations.reader.ui.screens.PrintsScreen
import com.sketchplanations.reader.ui.screens.SearchScreen
import com.sketchplanations.reader.ui.screens.SettingsScreen
import com.sketchplanations.reader.ui.screens.TagScreen
import com.sketchplanations.reader.ui.screens.TodayScreen
import com.sketchplanations.reader.ui.screens.ZoomViewer
import com.sketchplanations.reader.ui.theme.EaseOut
import com.sketchplanations.reader.ui.theme.Sk
import com.sketchplanations.reader.ui.theme.hand
import com.sketchplanations.reader.ui.theme.mono
import com.sketchplanations.reader.ui.theme.sans

private data class NavItem(val screen: Screen, val label: String, val icon: Lucide)

private val drawerItems = listOf(
    NavItem(Screen.Today, "Today", Lucide.Today), NavItem(Screen.Archive, "Archive", Lucide.Archive),
    NavItem(Screen.Browse, "Browse", Lucide.Browse), NavItem(Screen.Search, "Search", Lucide.Search),
    NavItem(Screen.Library, "Library", Lucide.Library), NavItem(Screen.Categories, "Topics", Lucide.Chart),
    NavItem(Screen.Podcast, "The podcast", Lucide.Mic), NavItem(Screen.PodLibrary, "Podcast library", Lucide.Mic),
    NavItem(Screen.Prints, "Support the author", Lucide.Image), NavItem(Screen.About, "About this app", Lucide.Info),
    NavItem(Screen.Settings, "Settings", Lucide.Settings),
)

private val tabItems = listOf(
    NavItem(Screen.Today, "Today", Lucide.Today), NavItem(Screen.Archive, "Archive", Lucide.Archive),
    NavItem(Screen.Search, "Search", Lucide.Search), NavItem(Screen.Library, "Library", Lucide.Library),
)

@Composable
fun App(m: AppModel, dark: Boolean, onToggleTheme: () -> Unit) {
    val hints = remember { HintState() }
    BoxWithConstraints(Modifier.fillMaxSize().background(Sk.colors.surfaceChrome)) {
        // sw600dp: a phone in landscape stays a phone; a tablet either way up is a tablet.
        val tablet = minOf(maxWidth, maxHeight) >= 600.dp
        CompositionLocalProvider(LocalTablet provides tablet, LocalHints provides hints) {
            BackHandler(enabled = m.zoom || m.confirm != null || m.egg || m.drawer || m.canGoBack()) {
                when {
                    m.egg -> m.egg = false
                    m.confirm != null -> m.confirm = null
                    m.zoom -> m.zoom = false
                    m.drawer -> m.drawer = false
                    else -> m.back()
                }
            }
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Shell(m, tablet, dark, onToggleTheme)
                Overlays(m, tablet, dark, onToggleTheme)
            }
        }
    }
}

@Composable
private fun Shell(m: AppModel, tablet: Boolean, dark: Boolean, onToggleTheme: () -> Unit) {
    val scr = m.screen
    val onboarding = scr == Screen.Onboarding
    Row(Modifier.fillMaxSize()) {
        if (tablet && !onboarding) PinnedRail(m, dark, onToggleTheme)
        Column(Modifier.weight(1f).fillMaxHeight().background(Sk.colors.surfaceApp)) {
            if (!onboarding) AppBar(m, tablet)
            Box(Modifier.weight(1f).fillMaxWidth().imePadding()) {
                Crossfade(scr to m.stack.size, animationSpec = tween(Sk.motion.fast), label = "screen") { (s, _) ->
                    ScreenContent(m, s)
                }
            }
            if (scr == Screen.Detail) DetailActions(m)
            val mini = m.playing && scr != Screen.Episode && !m.settings.stopOnLeave
            AnimatedVisibility(mini, enter = slideInVertically(tween(Sk.motion.base, easing = EaseOut)) { it / 2 } + fadeIn(tween(Sk.motion.base)), exit = fadeOut(tween(Sk.motion.fast))) {
                MiniPlayer(m)
            }
            if (!tablet && !onboarding) TabBar(m)
        }
    }
}

@Composable
private fun ScreenContent(m: AppModel, s: Screen) {
    when (s) {
        Screen.Onboarding -> OnboardingScreen(m)
        Screen.Today -> TodayScreen(m)
        Screen.Detail -> DetailScreen(m)
        Screen.Archive -> ArchiveScreen(m)
        Screen.Search -> SearchScreen(m)
        Screen.Categories -> CategoriesScreen(m)
        Screen.Library -> LibraryScreen(m)
        Screen.Podcast -> PodcastScreen(m)
        Screen.PodLibrary -> PodLibraryScreen(m)
        Screen.Episode -> EpisodeScreen(m)
        Screen.Browse -> BrowseScreen(m)
        Screen.Tag -> TagScreen(m)
        Screen.PodcastAbout -> PodcastAboutScreen(m)
        Screen.PodcastContact -> PodcastContactScreen(m)
        Screen.About -> AboutScreen(m)
        Screen.Prints -> PrintsScreen(m)
        Screen.Settings -> SettingsScreen(m)
    }
}

@Composable
private fun AppBar(m: AppModel, tablet: Boolean) {
    val title = when (m.screen) {
        Screen.Tag -> m.tag
        else -> m.screen.title
    }
    Column(Modifier.background(Sk.colors.surfaceChrome)) {
        Row(Modifier.height(56.dp).fillMaxWidth().padding(start = 4.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (m.stack.isNotEmpty()) BarIcon(Lucide.Back, "Back", { m.back() })
            else if (!tablet) BarIcon(Lucide.Menu, "Menu", { m.drawer = true })
            T(title, sans(20, 800, 1.2f, -0.01f), Modifier.weight(1f).padding(start = 8.dp), maxLines = 1)
            if (m.screen != Screen.Search) BarIcon(Lucide.Search, "Search", { m.go(Screen.Search) })
            BarIcon(Lucide.Shuffle, "Random sketch", { m.shuffle() })
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Sk.colors.borderDefault))
    }
}

@Composable
private fun TabBar(m: AppModel) {
    Column(Modifier.background(Sk.colors.surfaceChrome)) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(Sk.colors.borderDefault))
        Row(Modifier.fillMaxWidth()) {
            tabItems.forEach { t ->
                val on = m.screen == t.screen || (t.screen == Screen.Today && m.screen == Screen.Detail && m.stack.firstOrNull()?.screen == Screen.Today)
                val fg = if (on) Sk.colors.accentAlt else Sk.colors.textTertiary
                Column(
                    Modifier
                        .weight(1f)
                        .height(62.dp)
                        .background(if (on) Sk.colors.accentAltWash else Color.Transparent)
                        .noRippleClick({ m.tab(t.screen) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(t.icon, fg, 22.dp)
                    Spacer(Modifier.height(4.dp))
                    T(t.label, sans(11, 600), color = fg)
                }
            }
        }
    }
}

@Composable
private fun DrawerList(m: AppModel, pinned: Boolean) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).then(if (pinned) Modifier.padding(horizontal = 10.dp, vertical = 8.dp) else Modifier),
        verticalArrangement = Arrangement.spacedBy(if (pinned) 2.dp else 0.dp),
    ) {
        drawerItems.forEach { item ->
            val on = m.screen == item.screen
            val fg = if (on) Sk.colors.accentAlt else Sk.colors.textPrimary
            val bg = if (on) Sk.colors.accentAltWash else Color.Transparent
            if (pinned) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(Pill).background(bg).noRippleClick({ m.go(item.screen) }).padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) { Icon(item.icon, fg, 20.dp) }
                    Spacer(Modifier.width(12.dp))
                    T(item.label, sans(15, 600), color = fg)
                }
            } else {
                Column {
                    PressRow({ m.go(item.screen) }, Modifier.fillMaxWidth().background(bg)) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp).heightIn(min = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { Icon(item.icon, fg, 20.dp) }
                            Spacer(Modifier.width(14.dp))
                            T(item.label, sans(16, 600), color = fg)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Sk.colors.borderQuiet))
                }
            }
        }
    }
}

@Composable
private fun DrawerFoot(dark: Boolean, onToggleTheme: () -> Unit) {
    Column {
        Box(Modifier.fillMaxWidth().height(2.dp).background(Sk.colors.borderDefault))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            T("v1.0.0", mono(13), color = Sk.colors.textTertiary)
            Chip(if (dark) "Light" else "Dark", onToggleTheme, height = 36.dp, hPad = 14.dp, style = sans(13, 600), leading = if (dark) Lucide.Sun else Lucide.Moon)
        }
    }
}

@Composable
private fun DrawerHead(m: AppModel, size: Int) {
    Column {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 18.dp)) {
            T("Sketchplanations", sans(size, 800, 1.2f, -0.01f))
            Spacer(Modifier.height(4.dp))
            T("${m.library.offline.size} sketches offline ✎", hand(size - 5), color = Sk.colors.textSecondary)
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Sk.colors.borderDefault))
    }
}

/** Tablet: the drawer is not a drawer at all but a permanent 268px rail. */
@Composable
private fun PinnedRail(m: AppModel, dark: Boolean, onToggleTheme: () -> Unit) {
    Row(Modifier.width(270.dp).fillMaxHeight()) {
        Column(Modifier.width(268.dp).fillMaxHeight().background(Sk.colors.surfaceChrome)) {
            DrawerHead(m, 22)
            Box(Modifier.weight(1f)) { DrawerList(m, pinned = true) }
            DrawerFoot(dark, onToggleTheme)
        }
        Box(Modifier.width(2.dp).fillMaxHeight().background(Sk.colors.borderDefault))
    }
}

@Composable
private fun MiniPlayer(m: AppModel) {
    val e = Catalogue.episodes[m.audioEp]
    Row(
        Modifier.fillMaxWidth().background(Sk.colors.ink).padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).noRippleClick({ m.openEp(m.audioEp) })) {
            T("${clock((e.seconds * m.pos).toInt())} · playing", mono(11), color = Sk.colors.paperTint.copy(alpha = .7f))
            Spacer(Modifier.height(1.dp))
            T(e.title, sans(14, 700, 1.25f), color = Sk.colors.paperTint, maxLines = 1)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Sk.colors.penBlue).border(2.dp, Sk.colors.paperTint, CircleShape)
                .noRippleClick({ m.togglePlay(m.audioEp) }),
            contentAlignment = Alignment.Center,
        ) { Icon(Lucide.Pause, Color(0xFF22242A), 14.dp) }
    }
}

fun clock(t: Int) = "${t / 60}:${(t % 60).toString().padStart(2, '0')}"

@Composable
private fun Overlays(m: AppModel, tablet: Boolean, dark: Boolean, onToggleTheme: () -> Unit) {
    val dur = Sk.motion.base
    // Drawer: slides in from the left over a fading scrim, and slides back out before unmounting.
    if (!tablet) {
        AnimatedVisibility(m.drawer, enter = fadeIn(tween(dur, easing = EaseOut)), exit = fadeOut(tween(dur, easing = EaseOut))) {
            Box(Modifier.fillMaxSize().background(Sk.colors.scrim).noRippleClick({ m.drawer = false }))
        }
        AnimatedVisibility(
            m.drawer,
            enter = slideInHorizontally(tween(dur, easing = EaseOut)) { -it },
            exit = slideOutHorizontally(tween(if (dur == 0) 0 else 170, easing = EaseOut)) { -it },
        ) {
            Row(Modifier.fillMaxHeight()) {
                Column(Modifier.width(294.dp).fillMaxHeight().background(Sk.colors.surfaceOverlay).noRippleClick({})) {
                    DrawerHead(m, 23)
                    Box(Modifier.weight(1f)) { DrawerList(m, pinned = false) }
                    DrawerFoot(dark, onToggleTheme)
                }
                Box(Modifier.width(2.dp).fillMaxHeight().background(Sk.colors.borderDefault))
            }
        }
    }

    if (m.zoom) ZoomViewer(m)
    m.confirm?.let { ConfirmDialog(it, onNo = { m.confirm = null }, onYes = { m.confirm = null; it.act() }) }
    if (m.egg) EasterEgg(onClose = { m.egg = false })

    // Long-press tooltip, anchored under the pressed button.
    val hints = LocalHints.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        hints.label?.let { label ->
            T(
                label, sans(12, 600),
                Modifier
                    .layout { measurable, constraints ->
                        val p = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            val x = (hints.x - origin.x - p.width / 2f).toInt().coerceIn(0, (constraints.maxWidth - p.width).coerceAtLeast(0))
                            p.place(x, (hints.y - origin.y).toInt())
                        }
                    }
                    .clip(RoundedCornerShape(6.dp))
                    .background(Sk.colors.ink)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                Sk.colors.paperTint,
            )
        }
    }

    val toastBottom: Dp = if (tablet) 24.dp else 78.dp
    AnimatedVisibility(
        m.toast != null,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(Sk.motion.bounce)) + scaleIn(tween(Sk.motion.bounce), initialScale = .97f),
        exit = fadeOut(tween(Sk.motion.fast)),
    ) {
        var last by remember { mutableStateOf("") }
        m.toast?.let { last = it }
        Box(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = toastBottom), contentAlignment = Alignment.BottomCenter) {
            T(
                last, sans(15, 600),
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Sk.colors.ink).padding(horizontal = 18.dp, vertical = 14.dp),
                Sk.colors.paperTint,
            )
        }
    }
}
