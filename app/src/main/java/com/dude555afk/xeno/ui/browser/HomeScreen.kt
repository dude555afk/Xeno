package com.dude555afk.xeno.ui.browser

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
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
        val controlInteraction = remember { MutableInteractionSource() }
        val controlPressed by controlInteraction.collectIsPressedAsState()
        val controlScale by animateFloatAsState(
            targetValue = if (controlPressed) 0.94f else 1f,
            animationSpec = spring(dampingRatio = 0.62f, stiffness = 520f),
            label = "home-plus-press-scale"
        )

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
                // Source pixels inside the reference plus capsule cluster around #556179.\n                color = Color(0xFF556179),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .size(width = controlWidth, height = controlHeight)
                    .graphicsLayer(scaleX = controlScale, scaleY = controlScale)
                    .combinedClickable(
                        interactionSource = controlInteraction,
                        indication = null,
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
    // The Android reference uses a living four-colour mark, not a static logo.
    // The exact source animation video is unavailable, so this is deliberately
    // restrained: continuous rotation, a small breathing scale and a soft halo.
    val motion = rememberInfiniteTransition(label = "arc-home-mark")
    val rotation by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "arc-mark-rotation"
    )
    val breathing by motion.animateFloat(
        initialValue = 0.975f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arc-mark-breathing"
    )
    val haloAlpha by motion.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.23f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arc-mark-halo"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val unit = size.minDimension
            val radius = unit * 0.126f * breathing
            val spread = unit * 0.125f * breathing

            // The halo is kept low contrast so the mark doesn't turn into a neon
            // loading indicator on darker displays.
            drawCircle(
                color = Color(0xFFFFDDEB).copy(alpha = haloAlpha),
                radius = unit * 0.245f * breathing,
                center = Offset(cx, cy)
            )

            rotate(rotation, pivot = Offset(cx, cy)) {
                val petals = listOf(
                    Triple(Offset(cx, cy - spread), Color(0xFFFFD7E8), 0.94f),
                    Triple(Offset(cx + spread, cy), Color(0xFFFFD3B8), 1.00f),
                    Triple(Offset(cx, cy + spread), Color(0xFFD9C8FF), 0.97f),
                    Triple(Offset(cx - spread, cy), Color(0xFFFFE8A9), 1.00f)
                )
                petals.forEach { (center, tint, stretch) ->
                    val petalRadius = radius * stretch
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFFBFF),
                                tint,
                                tint.copy(alpha = 0.88f)
                            ),
                            center = Offset(
                                center.x - petalRadius * 0.22f,
                                center.y - petalRadius * 0.25f
                            ),
                            radius = petalRadius * 1.8f
                        ),
                        radius = petalRadius,
                        center = center
                    )
                }
                drawCircle(
                    color = Color(0xFFFFF4FA),
                    radius = radius * 0.29f,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}
