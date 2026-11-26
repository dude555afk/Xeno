package com.dude555afk.xeno.ui.corebrowser

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dude555afk.xeno.core.browser.CoreTab
import kotlinx.coroutines.launch
import kotlin.math.abs

private val TabHeaderTint = Color(0xFF998795)

@Composable
fun TabSwitcherScreen(
    tabs: List<CoreTab>,
    activeTabId: Long,
    s: Float,
    onSelect: (CoreTab) -> Unit,
    onClose: (CoreTab) -> Unit,
    onUndoClose: () -> Unit,
    onNewSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    val activeIndex = tabs.indexOfFirst { it.id == activeTabId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = activeIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0)),
        pageCount = { tabs.size.coerceAtLeast(1) }
    )
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val headerTab = tabs.getOrNull(pagerState.currentPage)

    fun closeWithUndo(tab: CoreTab) {
        onClose(tab)
        scope.launch {
            val result = snackbar.showSnackbar(
                message = "Tab closed",
                actionLabel = "Undo",
                withDismissAction = true
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) onUndoClose()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AmbientMeshBackdrop()
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height((145f * s).dp))
            if (headerTab != null) {
                val header = runCatching { android.net.Uri.parse(headerTab.url).host }
                    .getOrNull()?.removePrefix("www.")?.takeIf { it.isNotBlank() } ?: "New tab"
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.Language,
                        contentDescription = null,
                        tint = TabHeaderTint,
                        modifier = Modifier.width((24f * s).dp).height((24f * s).dp)
                    )
                    Text(
                        header,
                        color = TabHeaderTint,
                        fontSize = (17f * s).sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = (9f * s).dp)
                    )
                }
            }
            Spacer(Modifier.height((28f * s).dp))
            HorizontalPager(
                state = pagerState,
                key = { page -> tabs.getOrNull(page)?.id ?: page },
                modifier = Modifier.fillMaxWidth().weight(1f),
                pageSize = PageSize.Fixed((294f * s).dp),
                pageSpacing = (20f * s).dp,
                contentPadding = PaddingValues(horizontal = (84f * s).dp),
                verticalAlignment = Alignment.CenterVertically
            ) { page ->
                val tab = tabs.getOrNull(page)
                if (tab != null) {
                    TabPreviewCard(
                        tab = tab,
                        s = s,
                        onSelect = { onSelect(tab) },
                        onClose = { closeWithUndo(tab) }
                    )
                }
            }
            CoreBottomDock(
                s = s,
                browserMode = false,
                onOpenTabs = onDismiss,
                onNewSearch = onNewSearch,
                onRightAction = onDismiss
            )
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = (92f * s).dp)
        )
    }
}

@Composable
private fun TabPreviewCard(
    tab: CoreTab,
    s: Float,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val offset = remember(tab.id) { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val closeThresholdPx = with(density) { (120f * s).dp.toPx() }
    val fadeDistancePx = with(density) { (520f * s).dp.toPx() }
    val maxSwipePx = with(density) { (700f * s).dp.toPx() }
    val domain = runCatching { android.net.Uri.parse(tab.url).host }
        .getOrNull()?.removePrefix("www.")?.takeIf { it.isNotBlank() } ?: "New tab"
    Surface(
        onClick = onSelect,
        modifier = Modifier
            .width((294f * s).dp)
            .height((490f * s).dp)
            .graphicsLayer {
                translationY = offset.value
                alpha = (1f - abs(offset.value) / fadeDistancePx).coerceIn(0f, 1f)
            }
            .pointerInput(tab.id) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch { offset.snapTo((offset.value + dragAmount).coerceIn(-maxSwipePx, 0f)) }
                    },
                    onDragEnd = {
                        if (offset.value < -closeThresholdPx) {
                            onClose()
                        } else {
                            scope.launch {
                                offset.animateTo(
                                    0f,
                                    animationSpec = androidx.compose.animation.core.spring(
                                        stiffness = 350f,
                                        dampingRatio = 0.86f
                                    )
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch { offset.animateTo(0f) }
                    }
                )
            },
        shape = RoundedCornerShape((18f * s).dp),
        color = Color(0xFF202126),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF45464D))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF202126)).padding((20f * s).dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                domain,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = (16f * s).sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Column {
                Text(
                    tab.title.ifBlank { "New tab" },
                    color = Color.White,
                    fontSize = (20f * s).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    tab.url,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = (12f * s).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = (8f * s).dp)
                )
            }
        }
    }
}
