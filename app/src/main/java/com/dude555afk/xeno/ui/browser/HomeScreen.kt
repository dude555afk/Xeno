package com.dude555afk.xeno.ui.browser

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dude555afk.xeno.core.storage.CollectedPage

private val ArcLilac = Color(0xFF251126)
private val ArcPurple = Color(0xFF30154D)
private val ArcPink = Color(0xFF542A82)
private val ArcPeach = Color(0xFFB64D8F)

/**
 * Search-first new-tab canvas. Xeno uses its own mark while following Arc
 * Search's sparse mobile composition, gradient atmosphere and animated centre.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNewSearch: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenSettings: () -> Unit,
    onVoiceSearch: () -> Unit,
    pinnedPages: List<CollectedPage> = emptyList(),
    onOpenPinnedPage: (CollectedPage) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "xeno-home-mark")
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
                    colors = listOf(Color(0xFFB65AD2).copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(size.width * 0.22f, size.height * 0.20f),
                    radius = size.minDimension * 0.72f
                ),
                radius = size.minDimension * 0.72f,
                center = Offset(size.width * 0.22f, size.height * 0.20f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE48AC7).copy(alpha = 0.22f), Color.Transparent),
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
        )

        if (pinnedPages.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 84.dp)
                    .height(42.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(pinnedPages.take(8), key = { it.url }) { page ->
                    Surface(
                        onClick = { onOpenPinnedPage(page) },
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xCC2C2935),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.widthIn(max = 210.dp)
                    ) {
                        Text(
                            text = page.title.ifBlank { page.url },
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                            color = Color(0xFF41385F),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 26.dp, end = 26.dp, bottom = 18.dp),
            shape = CircleShape,
            color = Color(0xC6292731),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.13f)),
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
                    color = Color(0xFF5D4BE8),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA69AFF)),
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
                            tint = Color.White,
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
            Icon(icon, contentDescription = label, tint = Color(0xFFECE7F7), modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun HomeMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val petalRadius = size.minDimension * 0.23f
        val spread = size.minDimension * 0.18f
        val petals = listOf(
            Offset(cx, cy - spread) to Color(0xFFFFD7E8),
            Offset(cx + spread, cy) to Color(0xFFFFD3B8),
            Offset(cx, cy + spread) to Color(0xFFD9C8FF),
            Offset(cx - spread, cy) to Color(0xFFFFE8A9)
        )
        petals.forEach { (center, tint) ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF7FC), tint, tint.copy(alpha = 0.82f)),
                    center = Offset(center.x - petalRadius * 0.22f, center.y - petalRadius * 0.25f),
                    radius = petalRadius * 1.8f
                ),
                radius = petalRadius,
                center = center
            )
        }
        drawCircle(Color(0xFFFFF4FA), radius = petalRadius * 0.30f, center = Offset(cx, cy))
    }
}
