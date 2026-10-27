package com.dude555afk.xeno.ui.browser

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

/**
 * Arc Search-style new-tab canvas. The browser remains quiet until the user
 * starts a search: an atmospheric gradient, an animated mark and two controls
 * are the entire start screen.
 */
@Composable
fun HomeScreen(
    onNewSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "home-mark")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "home-mark-rotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF4D39BC),
                        Color(0xFF9270E8),
                        Color(0xFFEAA6CB),
                        Color(0xFFFFD3D9)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            HomeMark(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-34).dp)
                    .size(62.dp)
                    .rotate(rotation)
            )

            Surface(
                onClick = onNewSearch,
                shape = RoundedCornerShape(22.dp),
                color = Color.White.copy(alpha = 0.86f),
                shadowElevation = 5.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
                    .size(width = 58.dp, height = 42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "New search",
                        tint = Color(0xFF34255F),
                        modifier = Modifier.size(25.dp)
                    )
                }
            }

            Surface(
                onClick = onOpenSettings,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.54f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 17.dp)
                    .size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF372960),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.42f
        val ribbon = Path().apply {
            moveTo(cx, cy - r * 0.10f)
            cubicTo(cx - r * 0.52f, cy - r * 0.02f, cx - r * 0.77f, cy - r * 0.42f, cx - r * 0.46f, cy - r * 0.73f)
            cubicTo(cx - r * 0.16f, cy - r * 1.02f, cx + r * 0.17f, cy - r * 0.75f, cx + r * 0.06f, cy - r * 0.43f)
            cubicTo(cx - r * 0.02f, cy - r * 0.18f, cx + r * 0.03f, cy - r * 0.07f, cx, cy - r * 0.10f)
        }
        drawPath(
            path = ribbon,
            color = Color(0xFFFFF8F4),
            style = Stroke(
                width = size.minDimension * 0.075f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawPath(
            path = Path().apply {
                moveTo(cx - r * 0.4f, cy + r * 0.35f)
                cubicTo(cx - r * 0.1f, cy + r * 0.05f, cx + r * 0.24f, cy + r * 0.18f, cx + r * 0.45f, cy + r * 0.55f)
            },
            color = Color(0xFFFFF8F4),
            style = Stroke(
                width = size.minDimension * 0.075f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawCircle(
            color = Color(0xFFFFF8F4).copy(alpha = 0.9f),
            radius = size.minDimension * 0.065f,
            center = Offset(cx, cy)
        )
    }
}
