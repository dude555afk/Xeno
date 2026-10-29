package com.dude555afk.xeno.ui.browser

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.dude555afk.xeno.core.model.Space
import com.dude555afk.xeno.core.model.Tab
import com.dude555afk.xeno.ui.answersheet.AnswerSheet
import com.dude555afk.xeno.ui.cmdbar.CommandPalette
import com.dude555afk.xeno.ui.summary.SummarySheet
import com.dude555afk.xeno.ui.theme.PillShape

private val XenoInk = Color(0xFF050507)
private val Hairline = Color.White.copy(alpha = 0.09f)

/**
 * Search-first browser chrome, using a lightweight Compose shell around Xeno's
 * existing WebView/session host. No browser engine or AI pipeline is replaced.
 */
@Composable
fun BrowserScreen(
    state: BrowserUiState,
    viewModel: BrowserViewModel,
    accent: Color
) {
    val isHome = state.activeTab?.hasContent != true

    Box(Modifier.fillMaxSize().background(XenoInk)) {
        // Keep the session host mounted. The landing page only covers an empty tab.
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val container = android.widget.FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                container.post { viewModel.attachHost(container) }
                container
            }
        )

        if (isHome) {
            HomeLanding(
                spaces = state.spaces,
                activeSpaceId = state.activeSpace?.id ?: 0L,
                tabs = state.tabs,
                accent = accent,
                onSelectSpace = viewModel::selectSpace,
                onOpenSearch = viewModel::openPalette,
                onOpenTabs = viewModel::toggleTabStack,
                onOpenTab = viewModel::selectTab,
                onAskIdea = { idea ->
                    viewModel.onQueryChange(idea)
                    viewModel.openPalette()
                }
            )
        } else {
            PageSpaceRail(
                spaces = state.spaces,
                activeSpaceId = state.activeSpace?.id ?: 0L,
                accent = accent,
                onSelectSpace = viewModel::selectSpace
            )
            FloatingDock(
                loadProgress = state.load.progress,
                loading = state.load.loading,
                accent = accent,
                canGoBack = state.canGoBack,
                displayUrl = state.load.url.ifBlank { state.activeTab?.url.orEmpty() },
                tabCount = state.tabs.size,
                onOpenPalette = viewModel::openPalette,
                onSummarize = { viewModel.summarizeCurrentPage() },
                onBack = viewModel::goBack,
                onOpenTabs = viewModel::toggleTabStack,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        CommandPalette(
            visible = state.paletteVisible,
            query = state.query,
            onQueryChange = viewModel::onQueryChange,
            tabHints = state.tabs.filter { it.hasContent },
            historyHints = state.historyHints,
            loading = state.answerStreaming,
            isQuestion = state.isQuestion,
            accent = accent,
            onNavigate = viewModel::navigate,
            onAsk = viewModel::ask,
            onOpenTab = { tab ->
                viewModel.selectTab(tab)
                viewModel.closePalette()
            },
            onDismiss = viewModel::closePalette
        )

        ArcTabOverview(
            visible = state.tabStackVisible,
            tabs = state.tabs,
            activeTabId = state.activeTab?.id,
            accent = accent,
            onSelect = { tab ->
                viewModel.selectTab(tab)
                viewModel.toggleTabStack()
            },
            onClose = viewModel::closeTab,
            onNewTab = viewModel::newTab,
            onDismiss = viewModel::toggleTabStack
        )

        AnswerSheet(
            result = state.answer,
            streaming = state.answerStreaming,
            accent = accent,
            onOpenSource = { source ->
                viewModel.navigate(source.url)
                viewModel.dismissAnswer()
            },
            onDismiss = viewModel::dismissAnswer
        )

        SummarySheet(
            visible = state.summaryVisible,
            pageTitle = state.activeTab?.displayTitle.orEmpty(),
            streamText = state.summaryText,
            tone = state.summaryTone,
            offline = state.summaryOffline,
            error = state.summaryError,
            accent = accent,
            onToneChange = viewModel::setSummaryTone,
            onCopy = { viewModel.copySummary() },
            onDismiss = viewModel::dismissSummary
        )
    }
}

@Composable
private fun HomeLanding(
    spaces: List<Space>,
    activeSpaceId: Long,
    tabs: List<Tab>,
    accent: Color,
    onSelectSpace: (Space) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenTab: (Tab) -> Unit,
    onAskIdea: (String) -> Unit
) {
    val recentTabs = tabs.filter { it.hasContent }.take(4)

    Box(Modifier.fillMaxSize().background(XenoInk)) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 112.dp, y = 44.dp)
                .size(300.dp)
                .background(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.23f), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-170).dp, y = 28.dp)
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF286C75).copy(alpha = 0.13f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 12.dp, bottom = 22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(accent, accent.copy(alpha = 0.48f), Color(0xFF1A8F9A))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("x", color = Color.Black, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            "xeno",
                            color = Color.White,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.8).sp
                        )
                        Text(
                            "A little less noise.",
                            color = Color.White.copy(alpha = 0.48f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                TabCounter(count = tabs.size, accent = accent, onClick = onOpenTabs)
            }

            Spacer(Modifier.height(22.dp))
            SpaceRail(
                spaces = spaces,
                activeSpaceId = activeSpaceId,
                accent = accent,
                onSelectSpace = onSelectSpace,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(38.dp))
            Text(
                "Where to\nnext?",
                color = Color.White,
                fontSize = 48.sp,
                lineHeight = 48.sp,
                letterSpacing = (-2.2).sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Search the web. Ask a question. Follow the thought.",
                color = Color.White.copy(alpha = 0.58f),
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 23.sp
            )

            Spacer(Modifier.height(28.dp))
            Surface(
                onClick = onOpenSearch,
                shape = RoundedCornerShape(27.dp),
                color = Color.White.copy(alpha = 0.075f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.13f)),
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth().animateContentSize()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 17.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(42.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = accent, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Search or ask Xeno",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "A question or a URL. One bar.",
                            color = Color.White.copy(alpha = 0.48f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("↵", color = Color.White.copy(alpha = 0.56f), fontSize = 23.sp)
                }
            }

            Spacer(Modifier.height(25.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "TRY ASKING",
                    color = Color.White.copy(alpha = 0.49f),
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.4.sp
                )
            }
            Spacer(Modifier.height(12.dp))

            listOf(
                "Why does the Moon look bigger near the horizon?",
                "What should I know about quantum entanglement?"
            ).forEach { idea ->
                Surface(
                    onClick = { onAskIdea(idea) },
                    shape = RoundedCornerShape(17.dp),
                    color = Color.White.copy(alpha = 0.035f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(
                        idea,
                        color = Color.White.copy(alpha = 0.79f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp)
                    )
                }
            }

            Spacer(Modifier.height(23.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Pick up where you left off",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "RECENT",
                    color = Color.White.copy(alpha = 0.36f),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.1.sp
                )
            }
            Spacer(Modifier.height(12.dp))

            if (recentTabs.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.035f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(17.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Layers, null, tint = Color.White.copy(alpha = 0.45f))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Your next tab starts here",
                                color = Color.White.copy(alpha = 0.83f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Your open pages will show up here.",
                                color = Color.White.copy(alpha = 0.43f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else {
                recentTabs.forEach { tab ->
                    Surface(
                        onClick = { onOpenTab(tab) },
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.04f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.065f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                                    .background(accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Search, null, tint = accent, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    tab.displayTitle,
                                    color = Color.White.copy(alpha = 0.92f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    tab.url.removePrefix("https://").removePrefix("http://").substringBefore("/"),
                                    color = Color.White.copy(alpha = 0.43f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "Made for curiosity, not clutter.",
                color = Color.White.copy(alpha = 0.28f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun PageSpaceRail(
    spaces: List<Space>,
    activeSpaceId: Long,
    accent: Color,
    onSelectSpace: (Space) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 14.dp, end = 14.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpaceRail(
            spaces = spaces,
            activeSpaceId = activeSpaceId,
            accent = accent,
            onSelectSpace = onSelectSpace,
            modifier = Modifier.widthIn(max = 300.dp)
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun SpaceRail(
    spaces: List<Space>,
    activeSpaceId: Long,
    accent: Color,
    onSelectSpace: (Space) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        spaces.forEach { space ->
            val active = space.id == activeSpaceId
            Surface(
                onClick = { onSelectSpace(space) },
                shape = RoundedCornerShape(percent = 50),
                color = if (active) accent.copy(alpha = 0.17f) else Color.Black.copy(alpha = 0.42f),
                border = BorderStroke(
                    1.dp,
                    if (active) accent.copy(alpha = 0.5f) else Hairline
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        Modifier.size(if (active) 7.dp else 6.dp)
                            .clip(CircleShape).background(space.accent)
                    )
                    Text(
                        space.name,
                        color = if (active) Color.White else Color.White.copy(alpha = 0.58f),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TabCounter(count: Int, accent: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        color = Color.White.copy(alpha = 0.065f),
        border = BorderStroke(1.dp, Hairline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.Layers, null, tint = accent, modifier = Modifier.size(18.dp))
            Text(count.toString(), color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun FloatingDock(
    loadProgress: Float,
    loading: Boolean,
    accent: Color,
    canGoBack: Boolean,
    displayUrl: String,
    tabCount: Int,
    onOpenPalette: () -> Unit,
    onSummarize: () -> Unit,
    onBack: () -> Unit,
    onOpenTabs: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 12.dp).padding(bottom = 10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(29.dp),
            color = Color(0xF2121216),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 5.dp, end = 7.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, enabled = canGoBack) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            "Back",
                            tint = if (canGoBack) Color.White.copy(alpha = 0.84f)
                            else Color.White.copy(alpha = 0.24f)
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f).clip(PillShape)
                            .clickable(onClick = onOpenPalette).padding(horizontal = 9.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.48f), modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            displayUrl.ifBlank { "Search or ask Xeno" },
                            color = if (displayUrl.isBlank()) Color.White.copy(alpha = 0.48f)
                            else Color.White.copy(alpha = 0.92f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(onClick = onSummarize, shape = CircleShape, color = accent, modifier = Modifier.size(39.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.AutoAwesome, "Summarize page", tint = Color.Black, modifier = Modifier.size(19.dp))
                        }
                    }
                    Spacer(Modifier.width(5.dp))
                    Surface(
                        onClick = onOpenTabs,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.07f),
                        modifier = Modifier.size(39.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Layers, "Open tabs: $tabCount", tint = Color.White.copy(alpha = 0.86f), modifier = Modifier.size(19.dp))
                        }
                    }
                }
                AnimatedVisibility(visible = loading) {
                    LinearProgressIndicator(
                        progress = { loadProgress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(2.dp).clip(PillShape),
                        color = accent
                    )
                }
            }
        }
    }
}

@Composable
private fun ArcTabOverview(
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
        enter = fadeIn(tween(170)) + scaleIn(
            initialScale = 0.94f,
            animationSpec = spring(dampingRatio = 0.76f, stiffness = 380f)
        ),
        exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.97f, animationSpec = tween(150))
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.78f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = Color(0xFF101014),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                shadowElevation = 20.dp,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f).padding(horizontal = 14.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* consume backdrop taps */ }
            ) {
                Column(Modifier.fillMaxSize().padding(horizontal = 17.dp)) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier.align(Alignment.CenterHorizontally).size(width = 35.dp, height = 4.dp)
                            .clip(CircleShape).background(Color.White.copy(alpha = 0.2f))
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Your tabs", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(3.dp))
                            Text("${tabs.size} open", color = Color.White.copy(alpha = 0.48f), style = MaterialTheme.typography.bodySmall)
                        }
                        Surface(onClick = { onDismiss(); onNewTab() }, shape = CircleShape, color = accent, modifier = Modifier.size(46.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Add, "New tab", tint = Color.Black, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))

                    if (tabs.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.Layers, null, tint = accent, modifier = Modifier.size(38.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Nothing open yet", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(5.dp))
                            Text("Start a fresh tab and go exploring.", color = Color.White.copy(alpha = 0.48f), style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            items(tabs.chunked(2), key = { row -> row.first().id }) { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                                    row.forEach { tab ->
                                        TabPreviewCard(
                                            tab = tab,
                                            active = tab.id == activeTabId,
                                            accent = accent,
                                            onSelect = { onSelect(tab) },
                                            onClose = { onClose(tab) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Surface(
                        onClick = { onDismiss(); onNewTab() },
                        shape = RoundedCornerShape(percent = 50),
                        color = accent,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 17.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("New tab", color = Color.Black, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabPreviewCard(
    tab: Tab,
    active: Boolean,
    accent: Color,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(22.dp),
        color = if (active) accent.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.035f),
        border = BorderStroke(
            1.dp,
            if (active) accent.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.09f)
        ),
        modifier = modifier.height(178.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(15.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accent.copy(alpha = if (active) 0.27f else 0.15f),
                                Color.White.copy(alpha = 0.035f),
                                Color(0xFF17171D)
                            )
                        )
                    )
            ) {
                Row(Modifier.align(Alignment.TopStart).padding(9.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { index ->
                        Box(
                            Modifier.size(5.dp).clip(CircleShape).background(
                                if (index == 0) accent else Color.White.copy(alpha = 0.22f)
                            )
                        )
                    }
                }
                Column(
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp, end = 12.dp, top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.fillMaxWidth(0.68f).height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.28f)))
                    Box(Modifier.fillMaxWidth(0.92f).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.11f)))
                    Box(Modifier.fillMaxWidth(0.52f).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.11f)))
                }
                Surface(
                    onClick = onClose,
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.42f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(29.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Close, "Close tab", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(
                tab.displayTitle,
                color = Color.White.copy(alpha = 0.93f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                tab.url.removePrefix("https://").removePrefix("http://").substringBefore("/"),
                color = Color.White.copy(alpha = 0.42f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
