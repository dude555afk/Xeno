package com.dude555afk.xeno.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable

/**
 * Arc Search Android's new-tab canvas is an atmospheric colour field rather
 * than a dashboard. Keep it visually quiet so the bottom search control owns
 * the interaction hierarchy.
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF3437E8),
                        0.28f to Color(0xFF7040E8),
                        0.58f to Color(0xFFC14CDA),
                        0.82f to Color(0xFFF16BAF),
                        1.0f to Color(0xFFFFA9C5)
                    )
                )
            )
    )
}
