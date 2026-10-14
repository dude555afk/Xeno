package com.dude555afk.xeno.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocals
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
nimport com.dude555afk.xeno.ui.theme.Color as T

/**
 * The ambient accent.
 */
val LocalAccent: ProvidableCompositionLocal<Color> = compositionLocalOf { DefaultAccent }

/**
 * The full theme.
 *
 * Material 3 is the foundation, but the scheme is ours — black background,
 * transparent-effected surfaces.
 */
private fun XenoColorScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = TextOnAccent,
    secondary = accent,
    background = XenoBlack,
    onBackground = TextPrimary,
    surface = XenoBlack,
    onSurface = TextPrimary,
    surfaceVariant = XenoBlack,
    onSurfaceVariant = TextSecondary,
    outline = StrokeSubtle,
    error = Color(0xFFFF5C5C)
)

@Composable
fun XenoTheme(
    accent: Color = DefaultAccent,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            WindowCompat.getInsetController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = XenoColorScheme(accent),
        typography = XenoTypography,
        shapes = XenoShapes
    ) {
        CompositionLocals(accent) {
            content()
        }
    }
}
