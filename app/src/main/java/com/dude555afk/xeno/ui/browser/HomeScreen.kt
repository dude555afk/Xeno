package com.dude555afk.xeno.ui.browser

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private val ArcLilac = Color(0xFF8C7DE5)
private val ArcPurple = Color(0xFFB5A0EE)
private val ArcPink = Color(0xFFF2C5E0)
private val ArcPeach = Color(0xFFF7DBDF)

/**
 * Search-first new-tab canvas. Xeno uses its own mark while following Arc
 * Search's sparse mobile composition, gradient atmosphere and animated centre.
 */
@Composable
fun HomeScreen(
    onNewSearch: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenSettings: () -> Unit,
    onVoiceSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "xeno-home-mark")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "xeno-home-rotation"
    )
    val breathing by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "xeno-home-breathing"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(ArcLilac, ArcPurple, ArcPink, ArcPeach)
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.32f), Color.Transparent),
                    center = Offset(size.width * 0.22f, size.height * 0.20f),
                    radius = size.minDimension * 0.72f
                ),
                radius = size.minDimension * 0.72f,
                center = Offset(size.width * 0.22f, size.height * 0.20f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE9D9FF).copy(alpha = 0.40f), Color.Transparent),
                    center = Offset(size.width * 0.80f, size.height * 0.72f),
                    radius = size.minDimension * 0.62f
                ),
                radius = size.minDimension * 0.62f,
                center = Offset(size.width * 0.80f, size.height * 0.72f)
            )
        }

        HomeMark(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 25.dp)
                .size(82.dp)
                .scale(breathing)
                .rotate(rotation)
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 26.dp, end = 26.dp, bottom = 18.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.48f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.52f)),
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DockAction(Icons.Rounded.Layers, "Open tabs", onOpenTabs)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFFDFBFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(width = 76.dp, height = 48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .combinedClickable(onClick = onNewSearch, onLongClick = onVoiceSearch),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "New search; long-press for voice search",
                            tint = Color(0xFF3B315E),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                DockAction(Icons.Rounded.Settings, "Settings", onOpenSettings)
            }
        }
    }
}

@Composable
private fun DockAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.01f),
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = Color(0xFF41385F), modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun HomeMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.42f
        val stroke = size.minDimension * 0.095f
        val white = Color(0xFFFFFCFF)
        fun ribbon(startAngle: Double): Path {
            val sx = cx + cos(startAngle) * r.toDouble() * 0.12
            val sy = cy + sin(startAngle) * r.toDouble() * 0.12
            val ex = cx + cos(startAngle + 2.25) * r.toDouble() * 0.76
            val ey = cy + sin(startAngle + 2.25) * r.toDouble() * 0.76
            return Path().apply {
                moveTo(sx.toFloat(), sy.toFloat())
                cubicTo(
                    (cx + cos(startAngle + 0.58) * r.toDouble() * 1.02).toFloat(),
                    (cy + sin(startAngle + 0.58) * r.toDouble() * 1.02).toFloat(),
                    (cx + cos(startAngle + 1.12) * r.toDouble() * 0.88).toFloat(),
                    (cy + sin(startAngle + 1.12) * r.toDouble() * 0.88).toFloat(),
                    ex.toFloat(), ey.toFloat()
                )
                cubicTo(
                    (cx + cos(startAngle + 2.72) * r.toDouble() * 0.48).toFloat(),
                    (cy + sin(startAngle + 2.72) * r.toDouble() * 0.48).toFloat(),
                    (cx + cos(startAngle + 3.24) * r.toDouble() * 0.20).toFloat(),
                    (cy + sin(startAngle + 3.24) * r.toDouble() * 0.20).toFloat(),
                    sx.toFloat(), sy.toFloat()
                )
            }
        }
        listOf(-90.0, 30.0, 150.0).forEachIndexed { index, angle ->
            drawPath(
                path = ribbon(Math.toRadians(angle)),
                color = white.copy(alpha = if (index == 1) 0.86f else 0.97f),
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        drawCircle(white, radius = stroke * 0.63f, center = Offset(cx, cy))
    }
}
