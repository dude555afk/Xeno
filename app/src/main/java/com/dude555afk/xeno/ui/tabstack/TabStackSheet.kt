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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
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
 * Touch-first tab switcher inspired by Arc Search's page-preview carousel.
 * Tall cards make each tab feel like a page, not a row in a settings list.
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = Dimens.spaceL, end = Dimens.spaceS, bottom = Dimens.spaceM),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Your tabs",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${tabs.size} open",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close tab switcher",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (tabs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(Dimens.spaceXl),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tabs open yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                horizontal = Dimens.spaceL,
                                vertical = Dimens.spaceM
                            ),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
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
        targetValue = if (active) 1f else 0.96f,
        animationSpec = tween(180), label = "tabScale"
    )
    val host = runCatching {
        android.net.Uri.parse(tab.url).host?.removePrefix("www.").orEmpty()
    }.getOrDefault("")

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF111111),
        border = BorderStroke(
            if (active) 2.dp else 1.dp,
            if (active) accent.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.09f)
        ),
        onClick = onSelect,
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .scale(scale)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Abstract page preview until the browser exposes real per-tab thumbnails.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(Color(0xFFF2F0EB))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.42f)
                            .height(7.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF252525).copy(alpha = 0.75f))
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(0.82f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF252525).copy(alpha = 0.88f))
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(0.66f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF252525).copy(alpha = 0.88f))
                    )
                    Spacer(Modifier.height(4.dp))
                    repeat(4) { index ->
                        Box(
                            Modifier
                                .fillMaxWidth(if (index == 3) 0.62f else 1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF666666).copy(alpha = 0.20f))
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFDBD8D0))
                    )
                }
                if (tab.url.isBlank()) {
                    Text(
                        "New tab",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF252525),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = tab.displayTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = host.ifBlank { if (tab.url.isBlank()) "Search the web" else tab.url },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.54f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close tab",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
