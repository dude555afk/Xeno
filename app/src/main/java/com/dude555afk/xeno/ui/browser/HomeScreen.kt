package com.dude555afk.xeno.ui.browser

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.dude555afk.xeno.R

/**
 * Quiet Arc Search-inspired new-tab canvas.
 *
 * Keep this surface intentionally free of dashboard clutter: the persistent
 * browser chrome owns search and tab actions. Ambient motion is deliberately
 * subtle and runs only while this home surface is composed.
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val motion = rememberInfiniteTransition(label = "homeMarkMotion")
    val rotation = motion.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "markRotation"
    )
    val floatY = motion.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "markFloat"
    )
    val logoScale = motion.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(3300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "markBreath"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF30252F),
                        0.28f to Color(0xFF25212D),
                        0.62f to Color(0xFF17161E),
                        1.0f to Color(0xFF09090B)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // The mark stays optically centred; only its internal transform moves.
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "Xeno",
            modifier = Modifier
                .size(78.dp)
                .graphicsLayer {
                    translationY = floatY.value
                    rotationZ = rotation.value
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                }
        )
    }
}
