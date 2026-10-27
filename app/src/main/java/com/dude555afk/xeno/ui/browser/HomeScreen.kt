package com.dude555afk.xeno.ui.browser

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Arc Search-inspired new-tab canvas: restrained atmospheric colour, a softly
 * moving central mark, and no dashboard clutter. Search remains in browser chrome.
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val motion = rememberInfiniteTransition(label = "homeMotion")
    val rotation by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000)),
        label = "markRotation"
    )
    val breathe by motion.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(3200), RepeatMode.Reverse),
        label = "markBreathing"
    )
    val drift by motion.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(5200), RepeatMode.Reverse),
        label = "markDrift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF29252E),
                        0.24f to Color(0xFF211E28),
                        0.58f to Color(0xFF14131A),
                        1f to Color(0xFF08080A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset(y = drift.dp)
                .size(154.dp)
                .blur(42.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFB8A0CF).copy(alpha = 0.16f), Color.Transparent)
                    )
                )
        )
        Canvas(
            modifier = Modifier
                .offset(y = drift.dp)
                .size(82.dp)
                .rotate(rotation)
        ) {
            val stroke = size.minDimension * 0.095f
            val arcSize = size.minDimension * 0.78f
            val topLeft = androidx.compose.ui.geometry.Offset(
                (size.width - arcSize) / 2f,
                (size.height - arcSize) / 2f
            )
            val arcColors = listOf(
                Color(0xFFF7D5E8),
                Color(0xFFE8C6F5),
                Color(0xFFB7B9FF),
                Color(0xFF9DE4E0)
            )
            arcColors.forEachIndexed { index, color ->
                drawArc(
                    color = color.copy(alpha = 0.94f),
                    startAngle = index * 90f + 7f,
                    sweepAngle = 69f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                    style = Stroke(width = stroke * breathe, cap = StrokeCap.Round)
                )
            }
        }
    }
}
