package org.rdcl.nox.sketchplanationsreader.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Colour tokens from the Sketchplanations Browser Design System (tokens/colors.css and
 * tokens/theme-dark.css). Dark inverts chrome only: paper and ink swap so the 2px stroke and
 * the hard offset shadow survive as a cream line.
 */
@Immutable
data class SkColors(
    val paper: Color,
    val paperTint: Color,
    val paperDeep: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val rule: Color,
    val penRed: Color,
    val penOrange: Color,
    val penYellow: Color,
    val penGreen: Color,
    val penBlue: Color,
    val penBlueDeep: Color,
    val penOrangeInk: Color,
    val penGreenInk: Color,
    val penBlueInk: Color,
    val surfaceApp: Color,
    val surfaceCard: Color,
    val surfaceSunken: Color,
    val surfaceChrome: Color,
    val surfaceOverlay: Color,
    val scrim: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textOnAccent: Color,
    val textLink: Color,
    val textLinkHover: Color,
    val accent: Color,
    val accentWash: Color,
    val accentAlt: Color,
    val accentAltWash: Color,
    val borderDefault: Color,
    val borderQuiet: Color,
    val success: Color,
    val successWash: Color,
    val warningWash: Color,
    val error: Color,
    val errorWash: Color,
    val infoWash: Color,
    /** Colour of the hard offset shadow (ink in light, cream in dark). */
    val shadowInk: Color,
    val softShadow: Color,
    val isDark: Boolean,
)

val LightColors = SkColors(
    paper = Color(0xFFFBF8DE),
    paperTint = Color(0xFFFFFDF2),
    paperDeep = Color(0xFFF5EEC8),
    ink = Color(0xFF35363A),
    inkSoft = Color(0xFF5C5D62),
    inkFaint = Color(0xFF6A6B72),
    rule = Color(0xFFDED8B6),
    penRed = Color(0xFFE2544A),
    penOrange = Color(0xFFF0913C),
    penYellow = Color(0xFFF5C63D),
    penGreen = Color(0xFF67B05B),
    penBlue = Color(0xFF4C9BD6),
    penBlueDeep = Color(0xFF3A7FB4),
    penOrangeInk = Color(0xFFA85B14),
    penGreenInk = Color(0xFF3F7A36),
    penBlueInk = Color(0xFF2F6E9E),
    surfaceApp = Color(0xFFFBF8DE),
    surfaceCard = Color(0xFFFFFDF2),
    surfaceSunken = Color(0xFFF5EEC8),
    surfaceChrome = Color(0xFFFFFDF2),
    surfaceOverlay = Color(0xFFFFFDF2),
    scrim = Color(0x7335363A),
    textPrimary = Color(0xFF35363A),
    textSecondary = Color(0xFF5C5D62),
    textTertiary = Color(0xFF6A6B72),
    textOnAccent = Color(0xFFFFFDF2),
    textLink = Color(0xFFC43F36),
    textLinkHover = Color(0xFF9E2C24),
    accent = Color(0xFFC43F36),
    accentWash = Color(0x1FE2544A),
    accentAlt = Color(0xFF2F6E9E),
    accentAltWash = Color(0x244C9BD6),
    borderDefault = Color(0xFF35363A),
    borderQuiet = Color(0xFFDED8B6),
    success = Color(0xFF3F7A36),
    successWash = Color(0x2E67B05B),
    warningWash = Color(0x3DF5C63D),
    error = Color(0xFFC43F36),
    errorWash = Color(0x29E2544A),
    infoWash = Color(0x294C9BD6),
    shadowInk = Color(0xFF35363A),
    softShadow = Color(0x3335363A),
    isDark = false,
)

val DarkColors = SkColors(
    paper = Color(0xFF26272C),
    paperTint = Color(0xFF313238),
    paperDeep = Color(0xFF1C1D21),
    ink = Color(0xFFF2EFDC),
    inkSoft = Color(0xFFC3C0AE),
    inkFaint = Color(0xFF95928A),
    rule = Color(0xFF43444B),
    penRed = Color(0xFFFF8177),
    penOrange = Color(0xFFFFAB5E),
    penYellow = Color(0xFFF7CE55),
    penGreen = Color(0xFF8FD182),
    penBlue = Color(0xFF7FBEF0),
    penBlueDeep = Color(0xFFA6D3F5),
    penOrangeInk = Color(0xFFFFAB5E),
    penGreenInk = Color(0xFF8FD182),
    penBlueInk = Color(0xFF7FBEF0),
    surfaceApp = Color(0xFF26272C),
    surfaceCard = Color(0xFF313238),
    surfaceSunken = Color(0xFF1C1D21),
    surfaceChrome = Color(0xFF313238),
    surfaceOverlay = Color(0xFF313238),
    scrim = Color(0x9E0C0C0E),
    textPrimary = Color(0xFFF2EFDC),
    textSecondary = Color(0xFFC3C0AE),
    textTertiary = Color(0xFF95928A),
    textOnAccent = Color(0xFF1C1D21),
    textLink = Color(0xFFFF8177),
    textLinkHover = Color(0xFFFFA49C),
    accent = Color(0xFFFF8177),
    accentWash = Color(0x2EFF8177),
    accentAlt = Color(0xFF7FBEF0),
    accentAltWash = Color(0x337FBEF0),
    borderDefault = Color(0xFFF2EFDC),
    borderQuiet = Color(0xFF43444B),
    success = Color(0xFF8FD182),
    successWash = Color(0x338FD182),
    warningWash = Color(0x38F7CE55),
    error = Color(0xFFFF8177),
    errorWash = Color(0x33FF8177),
    infoWash = Color(0x337FBEF0),
    shadowInk = Color(0xFFF2EFDC),
    softShadow = Color(0x8C000000),
    isDark = true,
)

/**
 * Artwork constants. A sketch never inverts, so these are declared once and deliberately
 * not restated for dark.
 */
object Art {
    val page = Color(0xFFFBF8DE)
    val paper = Color(0xFFF5EEC8)
    val line = Color(0xFF35363A)
    val caption = Color(0xFF6A6B72)
    /** The zoom viewer's ground, in both themes. */
    val viewer = Color(0xFF22242A)
}

/** Hard offset shadows, no blur. */
object Shadow {
    val sm = 2.dp
    val md = 3.dp
    val lg = 5.dp
}

object Radius {
    val inline = 4.dp
    val thumb = 8.dp
    val input = 12.dp
    val card = 18.dp
}

val LocalSkColors = staticCompositionLocalOf { LightColors }
