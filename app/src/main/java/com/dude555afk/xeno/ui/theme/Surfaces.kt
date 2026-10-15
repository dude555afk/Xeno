package com.dude555afk.xeno.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The frosted-glass card. Translucent, stroked, rounded — the Arc surface.
 *
 * Real blur needs a backdrop behind the surface (API 31+). Where there is none,
 * this still reads as a floating layer because of the gradient + hairline stroke.
 */
fun Modifier.glassSurface(
    shape: Shape,
    fill: Color = SurfaceCard,
    stroke: Color = StrokeSubtle,
    strokeWidth: Dp = 1.dp
): Modifier = this
    .background(fill, shape)
    .border(strokeWidth, stroke, shape)

/**
 * Subtle top-lit gradient — makes a flat translucent card feel like glass
 * instead of like a bug.
 */
fun Modifier.glassHighlight(
    shape: Shape,
    accent: Color,
    strength: Float = 0.10f
): Modifier = this
    .background(
        Brush.verticalGradient(
            listOf(
                accent.copy(alpha = strength),
                Color.Transparent
            )
        ),
        shape
    )
    .alpha(1f)

/**
 * Standard elevated card used across tab stack, source cards, settings rows.
 *
 * Takes the accent explicitly rather than reading [LocalAccent], because this
 * is a Modifier extension and a Modifier factory is not composable — a
 * composition local cannot be read from here.
 */
fun Modifier.xenoCard(shape: Shape, accent: Color): Modifier =
    glassSurface(shape).then(glassHighlight(shape, accent))
