package com.dude555afk.xeno.ui.corebrowser

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

private val DockPlus = Color(0xFF566076)

@Composable
fun AmbientMeshBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(Color(0xFF1A1438))
        val lights = listOf(
            Triple(Offset(size.width * 0.08f, size.height * 0.04f), Color(0xFF1A1438), size.maxDimension * 0.9f),
            Triple(Offset(size.width * 0.50f, size.height * 0.04f), Color(0xFF332034), size.maxDimension * 0.8f),
            Triple(Offset(size.width * 0.96f, size.height * 0.05f), Color(0xFF572F38), size.maxDimension * 0.82f),
            Triple(Offset(size.width * 0.48f, size.height * 1.02f), Color(0xFF38425D), size.maxDimension * 1.0f)
        )
        lights.forEach { (center, color, radius) ->
            drawRect(
                Brush.radialGradient(
                    colors = listOf(color, color.copy(alpha = 0f)),
                    center = center,
                    radius = radius
                )
            )
        }
    }
}

@Composable
fun HomeScreen(
    s: Float,
    onOpenTabs: () -> Unit,
    onNewSearch: () -> Unit,
    onSettings: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        AmbientMeshBackdrop()
        ArcStarburstLogo(s, Modifier.align(Alignment.Center))
        CoreBottomDock(
            s = s,
            browserMode = false,
            onOpenTabs = onOpenTabs,
            onNewSearch = onNewSearch,
            onRightAction = onSettings,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun ArcStarburstLogo(s: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.size((183f * s).dp, (151f * s).dp)) {
        val sx = size.width / 183f
        val sy = size.height / 151f
        val points = listOf(
            Offset(91f, 2f), Offset(111f, 48f), Offset(179f, 31f),
            Offset(132f, 78f), Offset(158f, 145f), Offset(91f, 112f),
            Offset(25f, 148f), Offset(50f, 82f), Offset(4f, 34f), Offset(73f, 49f)
        )
        val path = Path().apply {
            moveTo(points.first().x * sx, points.first().y * sy)
            for (i in points.indices) {
                val a = points[i]
                val b = points[(i + 1) % points.size]
                val midX = (a.x + b.x) * 0.5f * sx
                val midY = (a.y + b.y) * 0.5f * sy
                quadraticTo(a.x * sx, a.y * sy, midX, midY)
            }
            close()
        }
        val gradient = Brush.verticalGradient(
            colors = listOf(Color(0xFFF3ABD9), Color(0xFFF5BBC9), Color(0xFFF8D4B2)),
            startY = 0f,
            endY = size.height
        )
        drawPath(path, gradient)
        drawPath(
            path,
            Color.White,
            style = Stroke(width = 7f * s * (size.width / (183f * s)), cap = StrokeCap.Round)
        )
        val triangle = Path().apply {
            moveTo(91f * sx, 57f * sy)
            lineTo(111f * sx, 91f * sy)
            lineTo(71f * sx, 91f * sy)
            close()
        }
        drawPath(triangle, Color.White)
    }
}

@Composable
fun CoreBottomDock(
    s: Float,
    browserMode: Boolean,
    onOpenTabs: () -> Unit,
    onNewSearch: () -> Unit,
    onRightAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height((85f * s).dp)
            .navigationBarsPadding()
            .background(if (browserMode) Color(0xFF2D2E32) else Color.Transparent)
    ) {
        IconButton(
            onClick = onOpenTabs,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (53f * s).dp)
                .size((48f * s).dp)
        ) {
            if (browserMode) {
                Box(Modifier.size((49f * s).dp, (48f * s).dp)) {
                    Box(
                        Modifier.align(Alignment.Center).offset(x = (-5f * s).dp, y = (1f * s).dp)
                            .size((31f * s).dp, (42f * s).dp)
                            .graphicsLayer(rotationZ = -9f)
                            .background(Color(0xFF2D2E32), RoundedCornerShape((7f * s).dp))
                            .border(1.5.dp, Color.White, RoundedCornerShape((7f * s).dp))
                    )
                    Box(
                        Modifier.align(Alignment.Center).offset(x = (6f * s).dp, y = (1f * s).dp)
                            .size((31f * s).dp, (42f * s).dp)
                            .graphicsLayer(rotationZ = 8f)
                            .background(Color(0xFF2D2E32), RoundedCornerShape((7f * s).dp))
                            .border(1.5.dp, Color.White, RoundedCornerShape((7f * s).dp))
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.Archive,
                    contentDescription = "Archive",
                    tint = Color.White,
                    modifier = Modifier.size((25f * s).dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size((96f * s).dp, (55f * s).dp)
                .background(
                    if (browserMode) Color(0xFF4A4E59) else DockPlus,
                    RoundedCornerShape((27.5f * s).dp)
                )
                .clickable(onClick = onNewSearch),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = "New search",
                tint = Color.White,
                modifier = Modifier.size((28f * s).dp)
            )
        }
        IconButton(
            onClick = onRightAction,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-53f * s).dp)
                .size((48f * s).dp)
        ) {
            if (browserMode) {
                androidx.compose.material3.Text(
                    "^",
                    color = Color.White,
                    fontSize = (27f * s).sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size((26f * s).dp)
                )
            }
        }
    }
}
