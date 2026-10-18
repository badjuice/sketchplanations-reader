package org.rdcl.nox.sketchplanationsreader.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.rdcl.nox.sketchplanationsreader.R

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, vararg weights: Int) = FontFamily(
    weights.map { w ->
        Font(res, FontWeight(w), FontStyle.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)

/** Nunito for everything structural, Patrick Hand for asides, Space Mono for numbers. */
private val Nunito = variable(R.font.nunito, 400, 600, 700, 800)
private val PatrickHand = FontFamily(Font(R.font.patrick_hand))
private val SpaceMono = FontFamily(Font(R.font.space_mono, FontWeight.Normal), Font(R.font.space_mono_bold, FontWeight.Bold))
private val Lora = variable(R.font.lora, 400, 600, 700)
private val Karla = variable(R.font.karla, 400, 600, 700, 800)

enum class FontPack(val label: String) { Original("Original"), Serif("Serif"), Sans("Sans"), Goofy("Goofy") }

/** The three roles every screen draws from. A typeface pack swaps all three at once. */
@Immutable
data class SkFonts(val sans: FontFamily, val hand: FontFamily, val mono: FontFamily)

fun fontsFor(pack: FontPack) = when (pack) {
    FontPack.Original -> SkFonts(Nunito, PatrickHand, SpaceMono)
    FontPack.Serif -> SkFonts(Lora, Lora, Lora)
    FontPack.Sans -> SkFonts(Karla, Karla, Karla)
    FontPack.Goofy -> SkFonts(PatrickHand, PatrickHand, PatrickHand)
}

@Immutable
data class SkMotion(val fast: Int, val base: Int, val bounce: Int) {
    companion object {
        val Normal = SkMotion(fast = 120, base = 180, bounce = 260)
        /** Reduce motion collapses every duration token to ~0. */
        val Reduced = SkMotion(0, 0, 0)
    }
}

val EaseOut = CubicBezierEasing(.2f, .8f, .3f, 1f)
val EaseBounce = CubicBezierEasing(.34f, 1.56f, .64f, 1f)

val LocalSkFonts = staticCompositionLocalOf { fontsFor(FontPack.Original) }
val LocalSkMotion = staticCompositionLocalOf { SkMotion.Normal }

object Sk {
    val colors: SkColors
        @Composable @ReadOnlyComposable get() = LocalSkColors.current
    val fonts: SkFonts
        @Composable @ReadOnlyComposable get() = LocalSkFonts.current
    val motion: SkMotion
        @Composable @ReadOnlyComposable get() = LocalSkMotion.current
}

private val Trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

@Suppress("DEPRECATION")
private fun base(family: FontFamily, size: Int, weight: Int, lineHeight: Float?, tracking: Float) = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    lineHeight = if (lineHeight != null) (size * lineHeight).sp else TextStyle.Default.lineHeight,
    letterSpacing = tracking.em,
    lineHeightStyle = Trim,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

/** Structural type. Headings pass `tracking = -0.01f`. */
@Composable
@ReadOnlyComposable
fun sans(size: Int, weight: Int = 400, lineHeight: Float? = null, tracking: Float = 0f) =
    base(Sk.fonts.sans, size, weight, lineHeight, tracking)

@Composable
@ReadOnlyComposable
fun hand(size: Int, lineHeight: Float? = null, italic: Boolean = false) =
    base(Sk.fonts.hand, size, 400, lineHeight, 0f).copy(fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal)

@Composable
@ReadOnlyComposable
fun mono(size: Int, weight: Int = 400) = base(Sk.fonts.mono, size, weight, null, 0f)

/** Headings: 800 weight, -0.01em. */
@Composable
@ReadOnlyComposable
fun heading(size: Int, lineHeight: Float = 1.25f) = sans(size, 800, lineHeight, -0.01f)

@Composable
fun SkTheme(dark: Boolean, pack: FontPack, reduceMotion: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalSkColors provides if (dark) DarkColors else LightColors,
        LocalSkFonts provides fontsFor(pack),
        LocalSkMotion provides if (reduceMotion) SkMotion.Reduced else SkMotion.Normal,
        content = content,
    )
}
