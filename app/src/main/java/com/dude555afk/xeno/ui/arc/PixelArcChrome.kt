package com.dude555afk.xeno.ui.arc

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp

private val ArcAmbientBottom = Color(0xFF38425D)

/** The sampled Arc Search ambient background, built from overlapping colour fields. */
@Composable
fun PixelArcAmbientBackground(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(ArcAmbientBottom)
        val w = size.width
        val h = size.height
        fun field(x: Float, y: Float, radius: Float, color: Color) {
            drawRect(
                brush = Brush.radialGradient(
                    0.0f to color,
                    1.0f to Color.Transparent,
                    center = Offset(w * x, h * y),
                    radius = radius
                )
            )
        }
        field(0.03f, 0.04f, w * 0.96f, Color(0xFF1A1438))
        field(0.50f, 0.00f, w * 0.78f, Color(0xFF332034))
        field(1.00f, 0.08f, w * 0.86f, Color(0xFF572F38))
        field(0.00f, 0.49f, w * 0.82f, Color(0xFF1F1D42))
        field(1.02f, 0.49f, w * 0.82f, Color(0xFF4D313F))
        field(0.50f, 0.67f, w * 0.95f, Color(0xFF363B58))
    }
}

/** Arc's five-limbed sticker mark, sized to the audit's 183×151 base grid. */
@Composable
fun PixelArcMark(modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 156.dp, height = 128.dp)) {
        val path = Path().apply {
            moveTo(89f, 5f)
            cubicTo(97f, 0f, 106f, 4f, 110f, 13f)
            lineTo(177f, 128f)
            cubicTo(184f, 141f, 175f, 151f, 162f, 151f)
            cubicTo(153f, 151f, 147f, 146f, 142f, 137f)
            lineTo(93f, 51f)
            lineTo(43f, 139f)
            cubicTo(37f, 150f, 27f, 153f, 17f, 147f)
            cubicTo(7f, 141f, 5f, 131f, 11f, 120f)
            lineTo(77f, 13f)
            cubicTo(80f, 9f, 84f, 6f, 89f, 5f)
            close()
        }
        scale(size.width / 183f, size.height / 151f, pivot = Offset.Zero) {
            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(width = 15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFF3ABD9), Color(0xFFF5BBC9), Color(0xFFF8D4B2))
                )
            )
            val cutout = Path().apply {
                moveTo(82f, 90f)
                lineTo(101f, 90f)
                lineTo(91.5f, 108f)
                close()
            }
            drawPath(cutout, Color.White)
        }
    }
}

@Composable
fun PixelArcHomeScreen(
    onOpenSearch: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenSettings: () -> Unit,
    tabCount: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {
        PixelArcAmbientBackground()
        PixelArcMark(Modifier.align(Alignment.Center))
        PixelBottomDock(
            tabCount = tabCount,
            homeMode = true,
            onOpenTabs = onOpenTabs,
            onNewTab = onOpenSearch,
            onOpenControls = onOpenSettings,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/** Three fixed controls aligned to the audited bottom-dock coordinates. */
@Composable
fun PixelBottomDock(
    tabCount: Int,
    homeMode: Boolean,
    onOpenTabs: () -> Unit,
    onNewTab: () -> Unit,
    onOpenControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dockSurface = if (homeMode) Color.Transparent else Color(0xFF2D2E32)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(dockSurface)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Surface(
                onClick = onOpenTabs,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 45.dp)
                    .width(40.dp)
                    .height(64.dp),
                color = Color.Transparent,
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (homeMode) Icons.Rounded.Archive else Icons.Rounded.Layers,
                        contentDescription = if (homeMode) "Archive" else "Open tabs",
                        tint = Color.White,
                        modifier = Modifier.size(if (homeMode) 25.dp else 27.dp)
                    )
                }
            }

            Surface(
                onClick = onNewTab,
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(82.dp)
                    .height(46.dp),
                color = if (homeMode) Color(0xFF566076) else Color(0xFF4A4E59),
                shape = RoundedCornerShape(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "New search",
                        tint = Color.White,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }

            Surface(
                onClick = onOpenControls,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 45.dp)
                    .width(40.dp)
                    .height(64.dp),
                color = Color.Transparent,
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (homeMode) Icons.Rounded.Settings else Icons.Rounded.ExpandMore,
                        contentDescription = if (homeMode) "App settings" else "Page controls",
                        tint = Color.White,
                        modifier = Modifier
                            .size(if (homeMode) 20.dp else 17.dp)
                            .then(if (homeMode) Modifier else Modifier.rotate(180f))
                    )
                }
            }
        }
    }
}
