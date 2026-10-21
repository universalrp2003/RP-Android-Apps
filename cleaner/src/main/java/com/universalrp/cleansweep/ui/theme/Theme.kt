package com.universalrp.cleansweep.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.universalrp.cleansweep.data.AppLang
import com.universalrp.cleansweep.data.Lang

/**
 * 2.52 — these names did not change, only where they come from.
 *
 * Every screen in the app imports `AccentCyan`, `SurfaceHigh`, `TextPrimary` … from this file. Each
 * one is now a getter that reads the selected [AppPalette] instead of a fixed constant, so adding
 * a theme (Palettes.kt) needs no screen changes at all. A getter keeps the same type (`Color`), so
 * `modifier.background(Bg)` still compiles exactly as before.
 */
val Bg: Color get() = Palette.current.bg
val SurfaceC: Color get() = Palette.current.surface
val SurfaceHigh: Color get() = Palette.current.surfaceHigh
val OutlineC: Color get() = Palette.current.outline
val AccentCyan: Color get() = Palette.current.accent
val AccentViolet: Color get() = Palette.current.accentAlt
val GoodGreen: Color get() = Palette.current.good
val WarnAmber: Color get() = Palette.current.warn
val DangerRed: Color get() = Palette.current.danger
val TextPrimary: Color get() = Palette.current.textPrimary
val TextSecondary: Color get() = Palette.current.textSecondary
val OnAccent: Color get() = Palette.current.onAccent

/** The palette's two signature colours, for headers, the hero card and the menu banner. */
val HeroGradient: List<Color> get() = listOf(Palette.current.accent, Palette.current.accentAlt)

/**
 * The language as Compose can observe it.
 *
 * `Lang.current` is a plain `@Volatile` field because workers, the widget and the notification
 * code read it without a composition. Reading it from a composable would never recompose, so the
 * theme mirrors it here and both writers (MainActivity at launch, MainViewModel.setLanguage when
 * the user switches) update this state as well. That is what lets the typography below change
 * with the language instead of staying at whatever it was on the first frame.
 */
object UiMode {
    val tamil = mutableStateOf(Lang.current == AppLang.TA)

    fun setLanguage(lang: AppLang) {
        tamil.value = lang == AppLang.TA
    }
}

/**
 * 2.52 — Tamil needs its own line rhythm.
 *
 * Tamil glyphs are taller and carry more diacritic marks above and below the baseline than Latin
 * ones. At Material's Latin line height they crowd each other, and the owner's description was
 * exact: *"the letters and size placement making the look bad"*. So for Tamil every style gets
 * a taller line box, no extra letter spacing (Latin tracking pulls Tamil clusters apart) and a
 * barely larger glyph size. English keeps Material's defaults untouched.
 */
private fun TextStyle.forTamil(): TextStyle {
    if (!fontSize.isSpecified) return this
    return copy(
        fontSize = (fontSize.value * 1.03f).sp,
        lineHeight = (fontSize.value * 1.62f).sp,
        letterSpacing = 0.sp,
    )
}

private fun typography(tamil: Boolean): Typography {
    if (!tamil) return Typography()
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.forTamil(),
        displayMedium = base.displayMedium.forTamil(),
        displaySmall = base.displaySmall.forTamil(),
        headlineLarge = base.headlineLarge.forTamil(),
        headlineMedium = base.headlineMedium.forTamil(),
        headlineSmall = base.headlineSmall.forTamil(),
        titleLarge = base.titleLarge.forTamil(),
        titleMedium = base.titleMedium.forTamil(),
        titleSmall = base.titleSmall.forTamil(),
        bodyLarge = base.bodyLarge.forTamil(),
        bodyMedium = base.bodyMedium.forTamil(),
        bodySmall = base.bodySmall.forTamil(),
        labelLarge = base.labelLarge.forTamil(),
        labelMedium = base.labelMedium.forTamil(),
        labelSmall = base.labelSmall.forTamil(),
    )
}

/** Built from the current palette each time it is requested, so a theme change is one rebuild. */
private fun darkScheme(p: AppPalette) = darkColorScheme(
    primary = p.accent,
    onPrimary = p.onAccent,
    primaryContainer = p.accent.copy(alpha = 0.22f),
    onPrimaryContainer = p.accent,
    secondary = p.accentAlt,
    onSecondary = p.onAccent,
    tertiary = p.good,
    onTertiary = p.onAccent,
    background = p.bg,
    onBackground = p.textPrimary,
    surface = p.surface,
    onSurface = p.textPrimary,
    surfaceVariant = p.surfaceHigh,
    onSurfaceVariant = p.textSecondary,
    outline = p.outline,
    error = p.danger,
    onError = Color(0xFF3D0707),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun CleanSweepTheme(content: @Composable () -> Unit) {
    // The app is designed dark-first; every palette is dark, so the scheme is always dark.
    val tamil = UiMode.tamil.value
    MaterialTheme(
        colorScheme = darkScheme(Palette.current),
        typography = typography(tamil),
        shapes = AppShapes,
        content = {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides Palette.current.textPrimary,
                content = content,
            )
        },
    )
}
