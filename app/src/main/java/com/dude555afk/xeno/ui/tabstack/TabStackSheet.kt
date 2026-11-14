package com.dude555afk.xeno.ui.tabstack

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dude555afk.xeno.core.model.Tab
import com.dude555afk.xeno.ui.arc.LocalArcMotionEnabled
import com.dude555afk.xeno.ui.arc.PixelArcAmbientBackground
import com.dude555afk.xeno.ui.arc.PixelBottomDock

private val HeaderTint = Color(0xFF998795)
private val CardStroke = Color(0xFF3E3F43)

/** Full-screen Arc-style tab carousel with live captured WebView previews. */
@Composable
fun TabStackSheet(
    visible: Boolean,
    tabs: List<Tab>,
    activeTabId: Long?,
    previews: Map<Long, android.graphics.Bitmap>,
    accent: Color,
    onSelect: (Tab) -> Unit,
    onClose: (Tab) -> Unit,
    onNewTab: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit = onDismiss
) {
    val motion = LocalArcMotionEnabled.current
    val activeIndex = tabs.indexOfFirst { it.id == activeTabId }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = activeIndex)
    val activeTab = tabs.getOrNull(activeIndex)

    BackHandler(enabled = visible, onBack = onDismiss)
    LaunchedEffect(visible, activeTabId, tabs.size) {
        if (visible && tabs.isNotEmpty()) {
            runCatching {
                if (motion) listState.animateScrollToItem(activeIndex)
                else listState.scrollToItem(activeIndex)
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = if (motion) fadeIn() + scaleIn(initialScale = 0.985f) else EnterTransition.None,
        exit = if (motion) fadeOut() + scaleOut(targetScale = 0.985f) else ExitTransition.None
    ) {
        Box(Modifier.fillMaxSize()) {
            PixelArcAmbientBackground()

            if (activeTab != null) {
                val title = remember(activeTab.url, activeTab.displayTitle) {
                    runCatching { java.net.URI(activeTab.url).host?.removePrefix("www.") }
                        .getOrNull()
                        .orEmpty()
                        .ifBlank { activeTab.displayTitle }
                }
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 145.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAi = activeTab.url.startsWith("xeno://answer", ignoreCase = true)
                    Icon(
                        if (isAi) Icons.Rounded.AutoAwesome else Icons.Rounded.Language,
                        contentDescription = null,
                        tint = HeaderTint,
                        modifier = Modifier.width(25.dp).height(24.dp)
                    )
                    Text(
                        title,
                        color = HeaderTint,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 10.dp).widthInCompat(196.dp)
                    )
                }
            }

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().align(Alignment.Center)
            ) {
                val cardWidth = minOf(250.dp, maxWidth * 0.64f)
                val cardHeight = minOf(471.dp, maxHeight * 0.56f)
                val sideInset = ((maxWidth - cardWidth) / 2).coerceAtLeast(12.dp)
                LazyRow(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().height(cardHeight),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = sideInset),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(17.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(tabs, key = { it.id }) { tab ->
                        val preview = previews[tab.id]
                        Surface(
                            onClick = { onSelect(tab) },
                            modifier = Modifier.width(cardWidth).height(cardHeight),
                            shape = RoundedCornerShape(13.dp),
                            color = Color(0xFF202126),
                            border = BorderStroke(1.dp, CardStroke)
                        ) {
                            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(13.dp))) {
                                if (preview != null && !preview.isRecycled) {
                                    Image(
                                        bitmap = preview.asImageBitmap(),
                                        contentDescription = tab.displayTitle,
                                        contentScale = ContentScale.Crop,
                                        alignment = Alignment.TopCenter,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        Modifier.fillMaxSize().background(Color(0xFF202126)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            tab.displayTitle.ifBlank { "New tab" },
                                            color = Color.White.copy(alpha = 0.82f),
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            PixelBottomDock(
                tabCount = tabs.size,
                homeMode = true,
                onOpenTabs = onDismiss,
                onNewTab = onNewTab,
                onOpenControls = onOpenSettings,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/** Keeps the measured single-line tab header from expanding with long page titles. */
private fun Modifier.widthInCompat(max: androidx.compose.ui.unit.Dp): Modifier =
    this.then(Modifier.width(max))
