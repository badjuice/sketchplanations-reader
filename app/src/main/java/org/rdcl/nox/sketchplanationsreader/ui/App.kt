package org.rdcl.nox.sketchplanationsreader.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.graphicsLayer
import org.rdcl.nox.sketchplanationsreader.ui.components.still
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import org.rdcl.nox.sketchplanationsreader.BuildConfig
import org.rdcl.nox.sketchplanationsreader.state.AppModel
import org.rdcl.nox.sketchplanationsreader.state.Screen
import org.rdcl.nox.sketchplanationsreader.ui.components.BarIcon
import org.rdcl.nox.sketchplanationsreader.ui.components.Icon
import org.rdcl.nox.sketchplanationsreader.ui.components.LocalHints
import org.rdcl.nox.sketchplanationsreader.ui.components.LocalTablet
import org.rdcl.nox.sketchplanationsreader.ui.components.LocalThumbWhole
import org.rdcl.nox.sketchplanationsreader.ui.components.LocalImageSource
import org.rdcl.nox.sketchplanationsreader.ui.components.Lucide
import org.rdcl.nox.sketchplanationsreader.ui.components.Pill
import org.rdcl.nox.sketchplanationsreader.ui.components.PressRow
import org.rdcl.nox.sketchplanationsreader.ui.components.T
import org.rdcl.nox.sketchplanationsreader.ui.components.HintState
import org.rdcl.nox.sketchplanationsreader.ui.components.InkCard
import org.rdcl.nox.sketchplanationsreader.ui.components.noRippleClick
import org.rdcl.nox.sketchplanationsreader.ui.screens.AboutScreen
import org.rdcl.nox.sketchplanationsreader.ui.screens.ArchiveScreen
import org.rdcl.nox.sketchplanationsreader.ui.screens.ConfirmDialog
import org.rdcl.nox.sketchplanationsreader.ui.screens.DetailActions
import org.rdcl.nox.sketchplanationsreader.ui.screens.DetailScreen
import org.rdcl.nox.sketchplanationsreader.ui.screens.EasterEgg
import org.rdcl.nox.sketchplanationsreader.ui.screens.EpisodeScreen
import org.rdcl.nox.sketchplanationsreader.ui.screens.LibraryScreen
import org.rdcl.nox.sketchplanationsreader.ui.screens.OnboardingScreen
import org.rdcl.nox.sketchplanationsreader.ui.screens.PodcastScreen
import org.rdcl.nox.sketchplanationsreader.ui.screens.SearchScreen
import org.rdcl.nox.sketchplanationsreader.ui.screens.SettingsScreen
import org.rdcl.nox.sketchplanationsreader.ui.screens.TagScreen
import org.rdcl.nox.sketchplanationsreader.ui.screens.TodayScreen
import org.rdcl.nox.sketchplanationsreader.ui.screens.LinkSheet
import org.rdcl.nox.sketchplanationsreader.ui.screens.OptionsSheet
import org.rdcl.nox.sketchplanationsreader.ui.screens.title
import org.rdcl.nox.sketchplanationsreader.state.Options
import org.rdcl.nox.sketchplanationsreader.ui.screens.ZoomViewer
import org.rdcl.nox.sketchplanationsreader.ui.theme.EaseOut
import org.rdcl.nox.sketchplanationsreader.ui.theme.Sk
import org.rdcl.nox.sketchplanationsreader.ui.theme.mono
import org.rdcl.nox.sketchplanationsreader.ui.theme.sans

private data class NavItem(val screen: Screen, val label: String, val icon: Lucide)

/** The four places people go every day. Everything else sits behind the ⋮ menu. */
private val tabItems = listOf(
    NavItem(Screen.Today, "Today", Lucide.Today), NavItem(Screen.Archive, "Archive", Lucide.Archive),
    NavItem(Screen.Podcast, "Podcast", Lucide.Mic), NavItem(Screen.Library, "Library", Lucide.Library),
)

private class MenuItem(val label: String, val icon: Lucide, val act: (AppModel) -> Unit)

private val menuItems = listOf(
    MenuItem("Random sketch", Lucide.Shuffle) { it.shuffle() },
    MenuItem("Settings", Lucide.Settings) { it.go(Screen.Settings) },
    MenuItem("About this app", Lucide.Info) { it.go(Screen.About) },
)

/** The settings sheet a screen offers in its top bar. */
private val Screen.options get() = when (this) {
    Screen.Today -> Options.Today
    Screen.Detail -> Options.Reading
    Screen.Archive, Screen.Tag -> Options.Archive
    Screen.Search -> Options.Search
    Screen.Podcast, Screen.Episode -> Options.Podcast
    else -> null
}

/** Identifies a history entry: its depth, its screen and what it shows. */
private fun AppModel.frameKey() = "${stack.size}/$screen/" + when (screen) {
    Screen.Episode -> ep.toString()
    Screen.Tag -> tag
    else -> ""
}

/** The tab a screen belongs to: the screen itself at the top level, else where its stack started. */
private fun AppModel.currentTab(): Screen = stack.firstOrNull()?.screen ?: screen

@Composable
fun App(m: AppModel) {
    val hints = remember { HintState() }
    BoxWithConstraints(Modifier.fillMaxSize().background(Sk.colors.surfaceChrome)) {
        // sw600dp: a phone in landscape stays a phone; a tablet either way up is a tablet.
        val tablet = minOf(maxWidth, maxHeight) >= 600.dp
        CompositionLocalProvider(LocalTablet provides tablet, LocalHints provides hints, LocalThumbWhole provides m.settings.thumbWhole, LocalImageSource provides m.downloads::image) {
            BackHandler(enabled = m.options != null || m.linkSheet != null || m.zoomUrl != null || m.confirm != null || m.egg || m.menu || m.canGoBack()) {
                when {
                    m.egg -> m.egg = false
                    m.confirm != null -> m.confirm = null
                    m.linkSheet != null -> m.linkSheet = null
                    m.options != null -> m.options = null
                    m.zoomUrl != null -> m.zoomUrl = null
                    m.menu -> m.menu = false
                    else -> m.back()
                }
            }
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Shell(m, tablet)
                Overlays(m, tablet)
            }
        }
    }
}

@Composable
private fun Shell(m: AppModel, tablet: Boolean) {
    val scr = m.screen
    val onboarding = scr == Screen.Onboarding
    Row(Modifier.fillMaxSize()) {
        if (tablet && !onboarding) PinnedRail(m)
        Column(Modifier.weight(1f).fillMaxHeight().background(Sk.colors.surfaceApp)) {
            if (!onboarding) AppBar(m, tablet)
            Box(Modifier.weight(1f).fillMaxWidth().imePadding()) {
                // Each history entry keeps its own scroll positions, so Back returns to the same spot.
                val saved = rememberSaveableStateHolder()
                val key = m.frameKey()
                val seen = remember { mutableSetOf<String>() }
                seen += key
                LaunchedEffect(m.stack.size) {
                    // Entries deeper than the current one were popped; forget them so the next visit starts fresh.
                    seen.filter { it.substringBefore('/').toInt() > m.stack.size }.forEach { saved.removeState(it); seen -= it }
                }
                val still = still()
                AnimatedContent(key to scr, transitionSpec = {
                    val from = initialState.first.substringBefore('/').toInt()
                    val to = targetState.first.substringBefore('/').toInt()
                    when {
                        still -> fadeIn(snap()) togetherWith fadeOut(snap())
                        to > from -> (slideInHorizontally(tween(280, easing = EaseOut)) { it / 3 } + fadeIn(tween(200)))
                            .togetherWith(slideOutHorizontally(tween(280, easing = EaseOut)) { -it / 8 } + fadeOut(tween(160)))
                        to < from -> (slideInHorizontally(tween(280, easing = EaseOut)) { -it / 8 } + fadeIn(tween(200)))
                            .togetherWith(slideOutHorizontally(tween(280, easing = EaseOut)) { it / 3 } + fadeOut(tween(160)))
                        else -> (fadeIn(tween(220)) + scaleIn(tween(260, easing = EaseOut), initialScale = .97f))
                            .togetherWith(fadeOut(tween(120)))
                    }.using(SizeTransform(clip = false))
                }, label = "screen") { (k, s) ->
                    saved.SaveableStateProvider(k) { ScreenContent(m, s) }
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
        Screen.Library -> LibraryScreen(m)
        Screen.Podcast -> PodcastScreen(m)
        Screen.Episode -> EpisodeScreen(m)
        Screen.Tag -> TagScreen(m)
        Screen.About -> AboutScreen(m)
        Screen.Settings -> SettingsScreen(m)
    }
}

@Composable
private fun AppBar(m: AppModel, tablet: Boolean) {
    val title = when (m.screen) {
        Screen.Tag -> m.topicName(m.tag)
        else -> m.screen.title
    }
    Column(Modifier.background(Sk.colors.surfaceChrome)) {
        Row(Modifier.height(56.dp).fillMaxWidth().padding(start = 4.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (m.stack.isNotEmpty()) BarIcon(Lucide.Back, "Back", { m.back() }) else Spacer(Modifier.width(8.dp))
            T(title, sans(20, 800, 1.2f, -0.01f), Modifier.weight(1f).padding(start = 8.dp), maxLines = 1)
            m.screen.options?.let { o -> BarIcon(Lucide.Sliders, "${o.title} settings", { m.options = o }) }
            if (m.screen != Screen.Search) BarIcon(Lucide.Search, "Search", { m.go(Screen.Search) })
            BarIcon(Lucide.More, "More", { m.menu = true })
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Sk.colors.borderDefault))
    }
}

@Composable
private fun TabBar(m: AppModel) {
    Column(Modifier.background(Sk.colors.surfaceChrome)) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(Sk.colors.borderDefault))
        val still = still()
        val active = tabItems.indexOfFirst { it.screen == m.currentTab() }
        val marker by animateFloatAsState(active.coerceAtLeast(0).toFloat(), if (still) snap() else spring(dampingRatio = .7f, stiffness = 500f), label = "marker")
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val w = maxWidth / tabItems.size
            if (active >= 0) Box(
                Modifier.offset(x = w * marker).width(w).height(62.dp).padding(horizontal = 10.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp)).background(Sk.colors.accentAltWash),
            )
            Row(Modifier.fillMaxWidth()) {
                tabItems.forEach { t ->
                    val on = m.currentTab() == t.screen
                    val fg by animateColorAsState(if (on) Sk.colors.accentAlt else Sk.colors.textTertiary, tween(Sk.motion.base), label = "tabfg")
                    val lift by animateFloatAsState(if (on) 1f else 0f, if (still) snap() else spring(dampingRatio = .4f, stiffness = 600f), label = "lift")
                    Column(
                        Modifier
                            .weight(1f)
                            .height(62.dp)
                            .noRippleClick({ m.tab(t.screen) }),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(Modifier.graphicsLayer { translationY = -3.dp.toPx() * lift; scaleX = 1f + .08f * lift; scaleY = scaleX }) { Icon(t.icon, fg, 22.dp) }
                        Spacer(Modifier.height(4.dp))
                        T(t.label, sans(11, if (on) 700 else 600), color = fg)
                    }
                }
            }
        }
    }
}

/** Tablet: a permanent rail with the four tabs, then Search and Settings. */
@Composable
private fun PinnedRail(m: AppModel) {
    val items = tabItems + listOf(NavItem(Screen.Search, "Search", Lucide.Search), NavItem(Screen.Settings, "Settings", Lucide.Settings))
    Row(Modifier.width(222.dp).fillMaxHeight()) {
        Column(Modifier.width(220.dp).fillMaxHeight().background(Sk.colors.surfaceChrome)) {
            T("Sketchplanations", sans(20, 800, 1.2f, -0.01f), Modifier.padding(start = 18.dp, end = 16.dp, top = 22.dp, bottom = 16.dp))
            Box(Modifier.fillMaxWidth().height(2.dp).background(Sk.colors.borderDefault))
            Column(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items.forEach { item ->
                    val on = m.currentTab() == item.screen
                    val fg = if (on) Sk.colors.accentAlt else Sk.colors.textPrimary
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(Pill).background(if (on) Sk.colors.accentAltWash else Color.Transparent)
                            .noRippleClick({ m.tab(item.screen) }).padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(item.icon, fg, 20.dp)
                        Spacer(Modifier.width(12.dp))
                        T(item.label, sans(15, 600), color = fg)
                    }
                }
            }
            T("v${BuildConfig.VERSION_NAME}", mono(13), Modifier.padding(16.dp), Sk.colors.textTertiary)
        }
        Box(Modifier.width(2.dp).fillMaxHeight().background(Sk.colors.borderDefault))
    }
}

@Composable
private fun MiniPlayer(m: AppModel) {
    val e = m.episodes.getOrNull(m.audioEp) ?: return
    Row(
        Modifier.fillMaxWidth().background(Sk.colors.ink).padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).noRippleClick({ m.openEp(m.audioEp) })) {
            T("${clock((m.seconds(e) * m.pos).toInt())} · playing", mono(11), color = Sk.colors.paperTint.copy(alpha = .7f))
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
private fun Overlays(m: AppModel, tablet: Boolean) {
    // The ⋮ menu: a card under the top bar's right edge; tapping outside closes it.
    if (m.menu) {
        Box(Modifier.fillMaxSize().noRippleClick({ m.menu = false })) {
            InkCard(Modifier.align(Alignment.TopEnd).padding(top = 52.dp, end = 10.dp).width(230.dp), bg = Sk.colors.surfaceOverlay, radius = 14.dp) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    menuItems.forEach { item ->
                        PressRow({ m.menu = false; item.act(m) }, Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(item.icon, Sk.colors.textSecondary, 20.dp)
                                Spacer(Modifier.width(14.dp))
                                T(item.label, sans(15, 600))
                            }
                        }
                    }
                }
            }
        }
    }

    m.zoomUrl?.let { ZoomViewer(m, it) }
    m.linkSheet?.let { LinkSheet(m, it) }
    m.options?.let { OptionsSheet(m, it) }
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
        enter = fadeIn(tween(Sk.motion.bounce)) + slideInVertically(if (Sk.motion.base == 0) snap() else spring(dampingRatio = .6f, stiffness = 500f)) { it / 6 },
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
