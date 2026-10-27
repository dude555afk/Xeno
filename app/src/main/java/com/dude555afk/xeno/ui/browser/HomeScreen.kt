package com.dude555afk.xeno.ui.browser

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Pixel samples from the 1200x667 four-panel Arc Search Android capture.
// Points measured inside the 300x667 home panel. They are color anchors,
// not an inferred universal Arc design system.
private val ArcHomeTop = Color(0xFF1F143C)
private val ArcHomeMiddle = Color(0xFF44243C)
private val ArcHomeBottom = Color(0xFF34425F)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNewSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onVoiceSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to ArcHomeTop,
                        0.1006f to ArcHomeTop,
                        0.346f to ArcHomeMiddle,
                        0.843f to ArcHomeBottom,
                        1.0f to ArcHomeBottom
                    )
                )
            )
    ) {
        // Resolve viewport-relative dimensions here, before entering nested
        // BoxScope/Surface content lambdas where maxWidth/maxHeight are shadowed.
        val markBoxSize = (maxWidth * 0.447f).coerceIn(135.dp, 195.dp)
        val controlWidth = (maxWidth * 0.20f).coerceIn(72.dp, 88.dp)
        val controlHeight = (maxHeight * 0.045f).coerceIn(28.dp, 34.dp)
        val controlBottomInset = maxHeight * 0.055f
        val settingsEndInset = maxWidth * 0.08f

        // The reference mark's visible bounds are ~110x105 px in a 300x667
        // panel. Xeno's four-petal canvas fills ~82% of its parent box, so use
        // 44.7% of viewport width for the box to produce ~36.7% visible art.
        // The mark is centered on the viewport; no arbitrary vertical offset.
        HomeMark(
            modifier = Modifier
                .align(Alignment.Center)
                .size(markBoxSize)
        )

        // Arc's new-tab canvas has two actions in this state: a small plus
        // capsule centered horizontally and Settings at the right. The tab
        // carousel control belongs to the webpage dock, not this home state.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = controlBottomInset)
                .size(width = controlWidth, height = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = Color(0xFF4B5268),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .size(width = controlWidth, height = controlHeight)
                    .combinedClickable(
                        onClick = onNewSearch,
                        onLongClick = onVoiceSearch
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "New search; long-press for voice search",
                        tint = Color(0xFFECEEF6),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Surface(
            onClick = onOpenSettings,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = settingsEndInset, bottom = controlBottomInset)
                .size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = Color(0xFFD3D8E7),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeMark(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        // Custom Xeno artwork approximates the measured mark footprint.
        // Motion remains static until the Android reference video is measured.
        val petalRatio = 0.23f
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.minDimension * petalRatio
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
                        center = Offset(center.x - radius * 0.22f, center.y - radius * 0.25f),
                        radius = radius * 1.8f
                    ),
                    radius = radius,
                    center = center
                )
            }
            drawCircle(Color(0xFFFFF4FA), radius = radius * 0.30f, center = Offset(cx, cy))
        }
    }
}
