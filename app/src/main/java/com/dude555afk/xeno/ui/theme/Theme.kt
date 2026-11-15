package com.dude555afk.xeno.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
private fun xenoColorScheme(accent: Color, darkTheme: Boolean) =
    if (darkTheme) {
        darkColorScheme(
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
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            secondary = accent,
            onSecondary = Color.White,
            background = Color(0xFFF7F5FA),
            onBackground = Color(0xFF211D27),
            surface = Color(0xFFFFFBFF),
            onSurface = Color(0xFF211D27),
            surfaceVariant = Color(0xFFEDE8F2),
            onSurfaceVariant = Color(0xFF625B6A),
            outline = Color(0xFFB9B2C2),
            outlineVariant = Color(0xFFE4DFE9),
            error = Color(0xFFB3261E)
        )
    }

@Composable
fun XenoTheme(
    accent: Color = DefaultAccent,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = xenoColorScheme(accent, darkTheme),
        typography = XenoTypography,
        shapes = XenoShapes
    ) {
        CompositionLocalProvider(LocalAccent provides accent) {
            content()
        }
    }
}
