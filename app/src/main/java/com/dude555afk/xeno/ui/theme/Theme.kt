package com.dude555afk.xeno.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalAccent = staticCompositionLocalOf { DefaultAccent }

private fun xenoColorScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = TextOnAccent,
    secondary = accent,
    onSecondary = TextOnAccent,
    background = XenoBlack,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardHigh,
    onSurfaceVariant = TextSecondary,
    outline = StrokeSubtle,
    outlineVariant = StrokeSubtle,
    error = Color(0xFFFF6B79)
)

@Composable
fun XenoTheme(
    accent: Color = DefaultAccent,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = xenoColorScheme(accent),
        typography = XenoTypography,
        shapes = XenoShapes
    ) {
        CompositionLocalProvider(LocalAccent provides accent) {
            content()
        }
    }
}
