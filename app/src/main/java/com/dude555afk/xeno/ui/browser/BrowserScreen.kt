package com.dude555afk.xeno.ui.browser

import android.view.ViewGroup
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dude555afk.xeno.ui.answersheet.AnswerSheet
import com.dude555afk.xeno.ui.arc.ArcBottomDock
import com.dude555afk.xeno.ui.arc.ArcFindInPageBar
import com.dude555afk.xeno.ui.arc.ArcHomeScreen
import com.dude555afk.xeno.ui.arc.ArcHomeSettingsSheet
import com.dude555afk.xeno.ui.arc.ArcPageAddress
import com.dude555afk.xeno.ui.arc.ArcSiteActionsSheet
import com.dude555afk.xeno.ui.cmdbar.CommandPalette
import com.dude555afk.xeno.ui.summary.SummarySheet
import com.dude555afk.xeno.ui.tabstack.TabStackSheet

/**
 * Arc Search-inspired browser shell.
 *
 * The WebView remains the page renderer. Compose owns the browser chrome:
 * start page, search surface, bottom dock, tab carousel, and site actions.
 */
@Composable
fun BrowserScreen(
    state: BrowserUiState,
    viewModel: BrowserViewModel,
    accent: Color
) {
    var controlsVisible by remember { mutableStateOf(false) }
    var homeSettingsVisible by remember { mutableStateOf(false) }
    var findInPageVisible by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.showStartupSearchIfConfigured()
    }
    val displayUrl = state.load.url.ifBlank { state.activeTab?.url.orEmpty() }
    val isHome = displayUrl.isBlank() || displayUrl == "about:blank"

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Keep the live Chromium WebView mounted beneath the Compose chrome.
        // SessionPool owns the WebView instance and reuses it across recompositions.
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
            ArcHomeScreen(
                onOpenSearch = viewModel::openPalette,
                onBrowseForMe = viewModel::ask,
                onOpenTabs = viewModel::toggleTabStack,
                onNewTab = viewModel::newTab,
                onOpenSettings = { homeSettingsVisible = true },
                tabCount = state.tabs.size
            )
        } else {
            if (state.load.loading) {
                LinearProgressIndicator(
                    progress = { state.load.progress },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(2.dp),
                    color = accent,
                    trackColor = Color.Transparent
                )
            }

            ArcPageAddress(
                url = displayUrl,
                title = state.load.title.ifBlank { state.activeTab?.displayTitle.orEmpty() },
                onClick = viewModel::openPalette,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 84.dp)
            )

            ArcBottomDock(
                tabCount = state.tabs.size,
                onOpenTabs = viewModel::toggleTabStack,
                onNewTab = viewModel::newTab,
                onOpenControls = { controlsVisible = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 2.dp)
            )
        }

        ArcHomeSettingsSheet(
            visible = homeSettingsVisible,
            blockingEnabled = state.blockingEnabled,
            autoOpenKeyboard = state.autoOpenKeyboard,
            onBlockingChange = viewModel::setBlockingEnabled,
            onAutoOpenKeyboardChange = viewModel::setAutoOpenKeyboard,
            onDismiss = { homeSettingsVisible = false }
        )

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

        TabStackSheet(
            visible = state.tabStackVisible,
            tabs = state.tabs,
            activeTabId = state.activeTab?.id,
            previews = state.tabPreviews,
            accent = accent,
            onSelect = { tab ->
                viewModel.selectTab(tab)
                viewModel.toggleTabStack()
            },
            onClose = viewModel::closeTab,
            onNewTab = viewModel::newTab,
            onDismiss = viewModel::toggleTabStack
        )

        ArcSiteActionsSheet(
            visible = controlsVisible,
            title = state.load.title.ifBlank { state.activeTab?.displayTitle.orEmpty() },
            url = displayUrl,
            canGoBack = state.canGoBack,
            canGoForward = state.canGoForward,
            isPinned = state.isCurrentPagePinned,
            blockingEnabled = state.blockingEnabled,
            pageTextZoom = state.pageTextZoom,
            onDismiss = { controlsVisible = false },
            onBack = viewModel::goBack,
            onForward = viewModel::goForward,
            onReload = viewModel::reloadPage,
            onCopyUrl = {
                val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                manager?.setPrimaryClip(ClipData.newPlainText("Xeno address", displayUrl))
            },
            onFindInPage = {
                controlsVisible = false
                findQuery = ""
                findInPageVisible = true
            },
            onSummarize = {
                controlsVisible = false
                viewModel.summarizeCurrentPage()
            },
            onTogglePin = viewModel::togglePinCurrentPage,
            onShare = {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, state.load.title.ifBlank { state.activeTab?.displayTitle.orEmpty() })
                    putExtra(Intent.EXTRA_TEXT, displayUrl)
                }
                context.startActivity(Intent.createChooser(share, "Share page"))
            },
            onBlockingChange = viewModel::setBlockingEnabled,
            onZoomChange = viewModel::changePageTextZoom
        )

        if (findInPageVisible) {
            ArcFindInPageBar(
                query = findQuery,
                onQueryChange = { query ->
                    findQuery = query
                    viewModel.findInPage(query)
                },
                onPrevious = { viewModel.findNext(false) },
                onNext = { viewModel.findNext(true) },
                onClose = {
                    findInPageVisible = false
                    findQuery = ""
                    viewModel.clearFindInPage()
                },
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding()
            )
        }

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
            onCopy = viewModel::copySummary,
            onDismiss = viewModel::dismissSummary
        )
    }
}
