package com.dude555afk.xeno.ui.tabstack

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dude555afk.xeno.core.model.Tab
import com.dude555afk.xeno.ui.arc.LocalArcMotionEnabled

/**
 * Android-style Arc tab switcher: a centered horizontal carousel of live page
 * previews. The active tab is centered on open and neighboring tabs remain
 * visible at the edges to communicate the horizontal browsing gesture.
 */
@Composable
fun TabStackSheet(
    visible: Boolean,
    tabs: List<Tab>,
    activeTabId: Long?,
    previews: Map<Long, android.graphics.Bitmap> = emptyMap(),
    accent: Color,
    onSelect: (Tab) -> Unit,
    onClose: (Tab) -> Unit,
    onNewTab: () -> Unit,
    onDismiss: () -> Unit
) {
    val motionEnabled = LocalArcMotionEnabled.current
    val activeIndex = tabs.indexOfFirst { it.id == activeTabId }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = activeIndex)
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backdrop = if (dark) {
        listOf(Color(0xFF17151F), Color(0xFF2B2337), Color(0xFF382B42))
    } else {
        listOf(Color(0xFFE9E1F1), Color(0xFFF5E0EA), Color(0xFFE2DDF4))
    }

    LaunchedEffect(visible, activeTabId, tabs.size) {
        if (visible && tabs.isNotEmpty()) {
            runCatching {
                if (motionEnabled) listState.animateScrollToItem(activeIndex)
                else listState.scrollToItem(activeIndex)
            }
        }
    }

    BackHandler(enabled = visible, onBack = onDismiss)

    AnimatedVisibility(
        visible = visible,
        enter = if (motionEnabled) fadeIn(tween(160)) + scaleIn(
            initialScale = 0.985f,
            animationSpec = tween(190)
        ) else EnterTransition.None,
        exit = if (motionEnabled) fadeOut(tween(120)) + scaleOut(
            targetScale = 0.985f,
            animationSpec = tween(150)
        ) else ExitTransition.None
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(backdrop))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .padding(horizontal = 14.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close tabs",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                if (tabs.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No open tabs",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val cardWidth = minOf(270.dp, maxWidth - 36.dp)
                        val sideInset = ((maxWidth - cardWidth) / 2).coerceAtLeast(18.dp)

                        LazyRow(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = sideInset,
                                vertical = 12.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(tabs, key = { it.id }) { tab ->
                                ArcTabCard(
                                    tab = tab,
                                    active = tab.id == activeTabId,
                                    preview = previews[tab.id],
                                    accent = accent,
                                    cardWidth = cardWidth,
                                    onSelect = { onSelect(tab) },
                                    onClose = { onClose(tab) }
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(66.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        onClick = onNewTab,
                        shape = RoundedCornerShape(19.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 10.dp,
                        modifier = Modifier.width(64.dp).height(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "New tab",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArcTabCard(
    tab: Tab,
    active: Boolean,
    preview: android.graphics.Bitmap?,
    accent: Color,
    cardWidth: Dp,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val motionEnabled = LocalArcMotionEnabled.current
    val animatedScale by animateFloatAsState(
        targetValue = if (active) 1f else 0.965f,
        animationSpec = tween(180),
        label = "arc-tab-card-scale"
    )
    val scale = if (motionEnabled) animatedScale else if (active) 1f else 0.965f
    val fallback = listOf(Color(0xFF5140BC), Color(0xFFE18DAD))
    val host = remember(tab.url, tab.displayTitle) {
        runCatching { java.net.URI(tab.url).host?.removePrefix("www.") }
            .getOrNull()
            .orEmpty()
            .ifBlank { tab.displayTitle.ifBlank { "New tab" } }
    }

    Surface(
        onClick = onSelect,
        modifier = Modifier
            .width(cardWidth)
            .fillMaxHeight(0.86f)
            .scale(scale),
        shape = RoundedCornerShape(25.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (active) 2.dp else 1.dp,
            if (active) accent.copy(alpha = 0.82f) else MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = if (active) 18.dp else 8.dp
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(start = 15.dp, end = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    host,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close tab",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(Brush.linearGradient(fallback)),
                contentAlignment = Alignment.Center
            ) {
                if (preview != null && !preview.isRecycled) {
                    Image(
                        bitmap = preview.asImageBitmap(),
                        contentDescription = tab.displayTitle,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        tab.displayTitle.ifBlank { "New tab" },
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }
    }
}
