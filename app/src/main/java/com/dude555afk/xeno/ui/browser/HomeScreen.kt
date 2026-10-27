package com.dude555afk.xeno.ui.browser

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
 * Full-bleed Arc Search-style new-tab canvas.
 *
 * Keep the home screen quiet: the ambient colour field and Xeno mark are the
 * identity; search stays on the persistent bottom control, so the page never
 * turns into a crowded dashboard.
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val motion = rememberInfiniteTransition(label = "homeAmbientMotion")
    val glowX = motion.animateFloat(
        initialValue = -24f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(10_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "homeGlowX"
    )
    val glowY = motion.animateFloat(
        initialValue = 16f,
        targetValue = -18f,
        animationSpec = infiniteRepeatable(
            animation = tween(12_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "homeGlowY"
    )
    val logoFloat = motion.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoFloat"
    )
    val logoScale = motion.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.018f,
        animationSpec = infiniteRepeatable(
            animation = tween(3_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoBreath"
    )
    val logoRotation = motion.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(6_800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoDrift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF45283D),
                        0.25f to Color(0xFF302331),
                        0.56f to Color(0xFF17151E),
                        1.0f to Color(0xFF08080B)
                    )
                )
            )
    ) {
        // Soft moving light sources create depth without a continuously blurred
        // full-screen layer or expensive shader effects.
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = (-154).dp + glowX.value.dp, y = 42.dp + glowY.value.dp)
                .size(430.dp)
                .graphicsLayer(alpha = 0.88f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFB367A6).copy(alpha = 0.50f), Color(0xFF87528C).copy(alpha = 0.15f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 190.dp, y = 80.dp - glowY.value.dp)
                .size(390.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF5368A3).copy(alpha = 0.27f), Color(0xFF373C6A).copy(alpha = 0.10f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-215).dp, y = 110.dp)
                .size(430.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF8E466D).copy(alpha = 0.23f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "Xeno",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-24).dp)
                .size(88.dp)
                .graphicsLayer {
                    translationY = logoFloat.value
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                    rotationZ = logoRotation.value
                }
        )
    }
}
