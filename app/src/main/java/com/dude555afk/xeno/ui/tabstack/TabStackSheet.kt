package com.dude555afk.xeno.ui.tabstack

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dude555afk.xeno.core.model.Tab
import com.dude555afk.xeno.ui.theme.Dimens

/**
 * The tab stack — Arc's vertical sidebar, translated for touch.
 *
 * Compact pill-cards, swipe-to-dismiss, generous negative space. On mobile the
 * horizontal tab strip is a lost cause; this is the honest equivalent.
 */
@Composable
fun TabStackSheet(
    visible: Boolean,
    tabs: List<Tab>,
    activeTabId: Long?,
    accent: Color,
    onSelect: (Tab) -> Unit,
    onClose: (Tab) -> Unit,
    onNewTab: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) +
            slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(240)
            ),
        exit = fadeOut(tween(140)) +
            slideOutVertically(tween(180))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = Dimens.cardRadius, topEnd = Dimens.cardRadius),
                color = Color(0xFF0A0A0A),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.72f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* swallow */ }
            ) {
                Column(Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = Dimens.spaceM)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Color.White.copy(alpha = 0.2f))
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            horizontal = Dimens.spaceL,
                            vertical = Dimens.spaceS
                        ),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spaceS)
                    ) {
                        items(tabs, key = { it.id }) { tab ->
                            TabCard(
                                tab = tab,
                                active = tab.id == activeTabId,
                                accent = accent,
                                onSelect = { onSelect(tab) },
                                onClose = { onClose(tab) }
                            )
                        }
                    }

                    // New tab — a pill, exactly like everything else.
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = accent,
                        onClick = onNewTab,
                        modifier = Modifier
                            .padding(Dimens.spaceL)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "New tab",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = Dimens.spaceXl, vertical = Dimens.spaceM)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabCard(
    tab: Tab,
    active: Boolean,
    accent: Color,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1f else 0.98f,
        animationSpec = tween(160), label = "tabScale"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (active) accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(
            1.dp,
            if (active) accent.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.06f)
        ),
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth().scale(scale)
    ) {
        Row(
            modifier = Modifier.padding(Dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = tab.displayTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (tab.url.isNotBlank()) {
                    Text(
                        text = tab.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close tab",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
