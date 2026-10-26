package com.dude555afk.xeno.ui.spaces

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dude555afk.xeno.core.model.Space
import com.dude555afk.xeno.ui.theme.PillShape
import com.dude555afk.xeno.ui.theme.StrokeSubtle
import com.dude555afk.xeno.ui.theme.TextPrimary
import com.dude555afk.xeno.ui.theme.TextSecondary

/**
 * Compact, labeled workspace switcher. Color carries identity; the label keeps
 * the switcher understandable without asking users to memorize colored dots.
 */
@Composable
fun SpacesBar(
    spaces: List<Space>,
    activeSpaceId: Long,
    onSelect: (Space) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth(0.78f)
            .padding(start = 16.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(spaces, key = { it.id }) { space ->
            val active = space.id == activeSpaceId
            val accent by animateColorAsState(
                targetValue = space.accent,
                animationSpec = tween(220),
                label = "spaceAccent"
            )

            Surface(
                modifier = Modifier
                    .clip(PillShape)
                    .clickable { onSelect(space) },
                shape = PillShape,
                color = if (active) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.055f),
                border = BorderStroke(
                    1.dp,
                    if (active) accent.copy(alpha = 0.55f) else StrokeSubtle
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Text(
                        text = space.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) TextPrimary else TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
