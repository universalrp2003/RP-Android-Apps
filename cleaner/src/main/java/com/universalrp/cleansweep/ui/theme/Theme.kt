package com.universalrp.cleansweep.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 2.51 — these names did not change, only where they come from.
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
    MaterialTheme(
        colorScheme = darkScheme(Palette.current),
        shapes = AppShapes,
        content = {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides Palette.current.textPrimary,
                content = content,
            )
        },
    )
}
