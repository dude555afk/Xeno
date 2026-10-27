package com.dude555afk.xeno.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalAccent = staticCompositionLocalOf { DefaultAccent }

private fun xenoColorScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = TextOnAccent,
    secondary = accent,
    onSecondary = TextOnAccent,
    background = XenoBlack,
    onBackground = TextPrimary,
    surface = Color(0xFFFCFAFE),
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardHigh,
    onSurfaceVariant = TextSecondary,
    outline = StrokeSubtle,
    outlineVariant = StrokeSubtle,
    error = Color(0xFFB3261E)
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
