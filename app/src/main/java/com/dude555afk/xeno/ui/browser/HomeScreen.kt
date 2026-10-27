package com.dude555afk.xeno.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme

/**
 * Arc Search-inspired new-tab canvas.
 *
 * Keep the canvas deliberately quiet: the compact bottom dock is the primary
 * action surface, and opening a new tab moves straight into search. A large
 * brand mark or decorative gradient would compete with that interaction.
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    )
}
