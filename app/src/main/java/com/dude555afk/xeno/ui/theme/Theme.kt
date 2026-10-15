package com.dude555afk.xeno.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The ambient accent for the current space.
 *
 * Reads inside any composable that wants to tint itself without threading an
 * `accent` parameter through five layers of call sites.
 */
val LocalAccent = staticCompositionLocalOf { DefaultAccent }

/**
 * Material 3 is the foundation, but the scheme is entirely ours.
 */
private fun xenoColorScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = TextOnAccent,
    secondary = accent,
    onSecondary = TextOnAccent,
    background = XenoBlack,
    onBackground = TextPrimary,
    surface = XenoBlack,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = StrokeSubtle,
    outlineVariant = StrokeSubtle,
    error = Color(0xFFFF5C5C)
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
