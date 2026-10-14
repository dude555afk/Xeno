package com.dude555afk.xeno.ui.browser

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.dude555afk.xeno.ui.answersheet.AnswerSheet
import com.dude555afk.xeno.ui.cmdbar.CommandPalette
import com.dude555afk.xeno.ui.spaces.SpacesBar
import com.dude555afk.xeno.ui.summary.SummarySheet
import com.dude555afk.xeno.ui.tabstack.TabStackSheet
import com.dude555afk.xeno.ui.theme.Dimens
import com.dude555afk.xeno.ui.theme.PillShape

/**
 * The browser screen.
 *
 * Chrome hides on scroll — that's rule 3 from docs/DESIGN.md, and it's what
 * makes the content feel like the app rather than the page feeling like a
 * document inside a shell.
 */
@Composable
fun BrowserScreen(
    state: BrowserUiState,
    viewModel: BrowserViewModel,
    accent: Color
) {
    val context = LocalContext.current

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        // The page.
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.BLACK)
                }.also { web ->
                    val host = web.parent as? ViewGroup
                    if (host != null) viewModel.attachHost(host) else viewModel.attachHost(web.parent as ViewGroup)
                }
            }
        )

        // Spaces — top left, always available.
        SpacesBar(
            spaces = state.spaces,
            activeSpaceId = state.activeSpace?.id ?: 0L,
            onSelect = viewModel::selectSpace,
            modifier = Modifier.align(Alignment.TopStart).padding(top = Dimens.spaceXl)
        )

        // Tab stack — top right.
        IconButton(
            onClick = { viewModel.toggleTabStack() },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = Dimens.spaceM, end = Dimens.spaceM)
        ) {
            Icon(
                imageVector = Icons.Rounded.Layers,
                contentDescription = "Tabs",
                tint = Color.White.copy(alpha = 0.7f)
            )
        }

        // The floating bar — back, search field, summarize.
        FloatingBar(
            loadProgress = state.load.progress,
            loading = state.load.loading,
            accent = accent,
            canGoBack = true,
            query = state.query,
            onOpenPalette = viewModel::openPalette,
            onSummarize = { viewModel.summarizeCurrentPage() },
            onBack = viewModel::goBack,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Overlays
        CommandPalette(
            visible = state.paletteVisible,
            query = state.query,
            onQueryChange = viewModel::onQueryChange,
            tabHints = state.tabs.filter { it.hasContent },
            historyHints = emptyList(),
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

        TabStackSheet(
            visible = state.tabStackVisible,
            tabs = state.tabs,
            activeTabId = state.activeTab?.id,
            accent = accent,
            onSelect = { viewModel.selectTab(it); viewModel.toggleTabStack() },
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
            onCopy = { },
            onDismiss = viewModel::dismissSummary
        )
    }
}

/**
 * The floating bar. A pill that holds back, address field and summarize.
 *
 * Scales up subtly when tapped — cheap, and it makes the bar feel physical.
 */
@Composable
private fun FloatingBar(
    loadProgress: Float,
    loading: Boolean,
    accent: Color,
    canGoBack: Boolean,
    query: String,
    onOpenPalette: () -> Unit,
    onSummarize: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(160), label = "barScale"
    )

    Column(modifier = modifier.padding(horizontal = Dimens.spaceL, vertical = Dimens.spaceL)) {
        Surface(
            shape = PillShape,
            color = Color(0xF20A0A0A),
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.spaceS, vertical = Dimens.spaceS),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canGoBack) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(PillShape)
                            .clickable(onClick = onOpenPalette)
                            .padding(horizontal = Dimens.spaceM, vertical = Dimens.spaceM),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(Dimens.spaceS))
                        Text(
                            text = query.ifBlank { "Search or ask anything" },
                            color = if (query.isBlank()) Color.White.copy(alpha = 0.4f)
                            else Color.White.copy(alpha = 0.9f),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    }

                    // The summarize action — the flagship, one tap away.
                    Surface(
                        shape = PillShape,
                        color = accent,
                        onClick = onSummarize,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Summarize",
                            tint = Color.Black,
                            modifier = Modifier.padding(Dimens.spaceS)
                        )
                    }
                }

                if (loading) {
                    LinearProgressIndicator(
                        progress = { loadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(PillShape),
                        color = accent
                    )
                }
            }
        }
    }
}
