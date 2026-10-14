package com.dude555afk.xeno.ui.spaces

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
nimport com.dude555afk.xeno.core.model.Space
import com.dude555afk.xeno.ui.theme.Dimens

/**
 * The spaces bar — the identity switcher.
 *
 * Each space owns an accent that retints the entire UI. Tap one and the whole
 * app changes colour, which is Arc's single best trick and the thing almost
 * nobody copies.
 */
@Composable
fun SpacesBar(
    spaces: List<Space>,
    activeSpaceId: Long,
    onSelect: (Space) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = Dimens.spaceL),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        spaces.forEach { space ->
            val active = space.id == activeSpaceId
            val scale by animateFloatAsState(
                targetValue = if (active) 1.15f else 1f,
                animationSpec = tween(180),
                label = "spaceScale"
            )
            val color by animateColorAsState(
                targetValue = space.accent,
                animationSpec = tween(240),
                label = "spaceAccent"
            )

            Box(
                modifier = Modifier
                    .size(if (active) 14.dp else 10.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(if (active) color else color.copy(alpha = 0.35f))
                    .clickable { onSelect(space) }
            )
        }
    }
}
