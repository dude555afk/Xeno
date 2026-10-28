package com.dude555afk.xeno.ui.tabstack

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dude555afk.xeno.core.model.Tab

/**
 * Full-screen, horizontally browsable tab carousel modelled after Arc Search
 * on Android. Previews come from small in-memory WebView snapshots where
 * available; no page screenshots are written to disk.
 */
@Composable
fun TabStackSheet(
    visible: Boolean,
    tabs: List<Tab>,
    previews: Map<Long, Bitmap>,
    activeTabId: Long?,
    onSelect: (Tab) -> Unit,
    onClose: (Tab) -> Unit,
    onNewTab: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(190)) + scaleIn(
            initialScale = 0.94f,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
        ) + slideInVertically(
            initialOffsetY = { it / 18 },
            animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
        ),
        exit = fadeOut(tween(130)) + scaleOut(
            targetScale = 0.97f,
            animationSpec = tween(150)
        ) + slideOutVertically(targetOffsetY = { it / 24 }, animationSpec = tween(150))
    ) {
        Surface(
            shape = RoundedCornerShape(0.dp),
            color = Color(0xFF191821),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* The carousel owns the full surface. Back closes it. */ }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Tabs",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFF7F4FC)
                        )
                        Text(
                            text = if (tabs.size == 1) "1 open page" else "${tabs.size} open pages",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB1ABBF)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close tab switcher",
                            tint = Color(0xFFA7A0B4)
                        )
                    }
                }

                if (tabs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No open pages",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFB1ABBF)
                        )
                    }
                } else {
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val listState = rememberLazyListState()
                        // Keep the focused card centered, including the first and last tabs.
                        val cardSidePadding = ((maxWidth - 260.dp) / 2).coerceAtLeast(30.dp)
                        // Reference card bounds are about 260:512; preserve that ratio
                        // while shrinking to 88% of the available height on compact screens.
                        val cardHeight = minOf(260.dp / 0.508f, maxHeight * 0.88f)
                        LaunchedEffect(activeTabId, tabs.map { it.id }) {
                            val selectedIndex = tabs.indexOfFirst { it.id == activeTabId }
                            if (selectedIndex >= 0) {
                                listState.animateScrollToItem(selectedIndex)
                            }
                        }
                        LazyRow(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = cardSidePadding, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(tabs, key = { it.id }) { tab ->
                                TabCard(
                                    tab = tab,
                                    preview = previews[tab.id],
                                    active = tab.id == activeTabId,
                                    cardHeight = cardHeight,
                                    onSelect = { onSelect(tab) },
                                    onClose = { onClose(tab) }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF25232D),
                        border = BorderStroke(1.dp, Color(0xFF3D3948)),
                        onClick = onOpenSettings,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Settings, "App settings", tint = Color(0xFFEAE6F4), modifier = Modifier.size(21.dp))
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF5D4BE8),
                        border = BorderStroke(1.dp, Color(0xFFA69AFF)),
                        onClick = onNewTab,
                        modifier = Modifier.size(width = 62.dp, height = 48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Add, "New tab", tint = Color.White, modifier = Modifier.size(25.dp))
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF25232D),
                        border = BorderStroke(1.dp, Color(0xFF3D3948)),
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, "Close tab switcher", tint = Color(0xFFEAE6F4), modifier = Modifier.size(21.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabCard(
    tab: Tab,
    preview: Bitmap?,
    active: Boolean,
    cardHeight: Dp,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1f else 0.96f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "tabScale"
    )
    val shadowElevation by animateDpAsState(
        targetValue = if (active) 14.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "tabDepth"
    )
    val host = runCatching {
        android.net.Uri.parse(tab.url).host?.removePrefix("www.").orEmpty()
    }.getOrDefault("")

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF25232D),
        shadowElevation = shadowElevation,
        border = BorderStroke(
            if (active) 1.5.dp else 1.dp,
            if (active) Color(0xFF8B7AFF) else Color(0xFF3D3948)
        ),
        onClick = onSelect,
        modifier = Modifier
            .width(260.dp)
            .height(cardHeight)
            .scale(scale)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(Color(0xFF302D39)),
                contentAlignment = Alignment.Center
            ) {
                if (preview != null && !preview.isRecycled) {
                    Image(
                        bitmap = preview.asImageBitmap(),
                        contentDescription = "Preview of ${tab.displayTitle}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = if (tab.url.isBlank()) {
                            "New tab"
                        } else {
                            "${tab.displayTitle}\n\n${host.ifBlank { tab.url }}"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFEAE6F4),
                        textAlign = TextAlign.Center,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
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
                        color = Color(0xFFF7F4FC),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = host.ifBlank { if (tab.url.isBlank()) "Search the web" else tab.url },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB1ABBF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close tab",
                        tint = Color(0xFFA7A0B4)
                    )
                }
            }
        }
    }
}
