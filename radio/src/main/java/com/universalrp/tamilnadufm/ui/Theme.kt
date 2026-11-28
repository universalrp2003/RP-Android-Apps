package com.universalrp.tamilnadufm.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Light, readable radio surfaces with a simple green accent.
val Bg = Color(0xFFF4F6F1)
val SurfaceC = Color(0xFFFFFFFF)
val SurfaceHigh = Color(0xFFEDF3E9)
val OutlineC = Color(0xFFD0DACE)
val Saffron = Color(0xFF356B21)
val HeaderGreen = Color(0xFF447729)
val Gold = Color(0xFF826000)
val Teal = Color(0xFF006D68)
val Good = Color(0xFF316B32)
val Warn = Color(0xFF805500)
val Danger = Color(0xFFB3261E)
val TextPrimary = Color(0xFF1B211A)
val TextSecondary = Color(0xFF515E50)

private val LightScheme = lightColorScheme(
    primary = Saffron,
    onPrimary = Color.White,
    secondary = Gold,
    onSecondary = Color.White,
    tertiary = Teal,
    onTertiary = Color.White,
    background = Bg,
    onBackground = TextPrimary,
    surface = SurfaceC,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = TextSecondary,
    outline = OutlineC,
    error = Danger,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun TamilFmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        shapes = AppShapes,
        content = content,
    )
}
