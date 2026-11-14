package com.dude555afk.xeno.ui.browser

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dude555afk.xeno.ui.answersheet.AnswerSheet
import com.dude555afk.xeno.ui.arc.ArcFindInPageBar
import com.dude555afk.xeno.ui.arc.ArcSettingsScreen
import com.dude555afk.xeno.ui.arc.ArcSiteActionsSheet
import com.dude555afk.xeno.ui.arc.LocalArcMotionEnabled
import com.dude555afk.xeno.ui.arc.PixelArcHomeScreen
import com.dude555afk.xeno.ui.arc.PixelBottomDock
import com.dude555afk.xeno.ui.cmdbar.CommandPalette
import com.dude555afk.xeno.ui.summary.SummarySheet
import com.dude555afk.xeno.ui.tabstack.TabStackSheet

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
    var desktopSiteEnabled by remember { mutableStateOf(false) }
    val context = LocalContext.current

    BackHandler(
        enabled = homeSettingsVisible || findInPageVisible || controlsVisible ||
            state.summaryVisible || state.answer != null || state.paletteVisible ||
            state.tabStackVisible || state.canGoBack
    ) {
        when {
            homeSettingsVisible -> homeSettingsVisible = false
            findInPageVisible -> {
                findInPageVisible = false
                findQuery = ""
                viewModel.clearFindInPage()
            }
            controlsVisible -> controlsVisible = false
            state.summaryVisible -> viewModel.dismissSummary()
            state.answer != null -> viewModel.dismissAnswer()
            state.paletteVisible -> viewModel.closePalette()
            state.tabStackVisible -> viewModel.toggleTabStack()
            state.canGoBack -> viewModel.goBack()
        }
    }

    LaunchedEffect(viewModel) { viewModel.showStartupSearchIfConfigured() }

    val displayUrl = state.load.url.ifBlank { state.activeTab?.url.orEmpty() }
    val isHome = displayUrl.isBlank() || displayUrl == "about:blank"

    CompositionLocalProvider(LocalArcMotionEnabled provides state.animationsEnabled) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Keep the live Chromium WebView mounted beneath the Compose chrome.
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val container = android.widget.FrameLayout(ctx).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    container.post { viewModel.attachHost(container) }
                    container
                }
            )

            if (isHome) {
                PixelArcHomeScreen(
                    onOpenSearch = viewModel::openPalette,
                    onOpenTabs = viewModel::toggleTabStack,
                    onOpenSettings = { homeSettingsVisible = true },
                    tabCount = state.tabs.size
                )
            } else {
                if (state.load.loading) {
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { state.load.progress },
                        modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(2.dp),
                        color = accent,
                        trackColor = Color.Transparent
                    )
                }

                AnimatedVisibility(
                    visible = state.chromeVisible,
                    modifier = Modifier.fillMaxSize(),
                    enter = if (state.animationsEnabled) fadeIn(tween(140)) + slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = tween(220)
                    ) else EnterTransition.None,
                    exit = if (state.animationsEnabled) fadeOut(tween(110)) + slideOutVertically(
                        targetOffsetY = { it / 3 },
                        animationSpec = tween(180)
                    ) else ExitTransition.None
                ) {
                    Box(Modifier.fillMaxSize()) {
                        PixelBottomDock(
                            tabCount = state.tabs.size,
                            homeMode = false,
                            onOpenTabs = viewModel::toggleTabStack,
                            onNewTab = viewModel::newTab,
                            onOpenControls = { controlsVisible = true },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }

            ArcSettingsScreen(
                visible = homeSettingsVisible,
                themeMode = state.themeMode,
                accentOverrideArgb = state.accentOverrideArgb,
                animationsEnabled = state.animationsEnabled,
                blockingEnabled = state.blockingEnabled,
                autoOpenKeyboard = state.autoOpenKeyboard,
                onThemeModeChange = viewModel::setThemeMode,
                onAccentChange = viewModel::setAccentOverride,
                onAnimationsChange = viewModel::setAnimationsEnabled,
                onBlockingChange = viewModel::setBlockingEnabled,
                onAutoOpenKeyboardChange = viewModel::setAutoOpenKeyboard,
                onDismiss = { homeSettingsVisible = false },
                onShare = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Try Xeno Browser.")
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Xeno"))
                },
                onSetDefaultBrowser = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                    }
                },
                onClearBrowsingData = {
                    viewModel.clearBrowsingData()
                    CookieManager.getInstance().removeAllCookies(null)
                    WebStorage.getInstance().deleteAllData()
                },
                onOpenDownloads = {
                    runCatching { context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) }
                },
                onExportData = {
                    val urls = state.tabs.map { it.url }.filter { it.isNotBlank() }.distinct()
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, urls.joinToString("\n"))
                    }
                    context.startActivity(Intent.createChooser(intent, "Export open tabs"))
                }
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
                onDismiss = viewModel::toggleTabStack,
                onOpenSettings = { homeSettingsVisible = true }
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
                desktopSiteEnabled = desktopSiteEnabled,
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
                onTranslate = {
                    val target = "https://translate.google.com/translate?sl=auto&tl=en&u=" +
                        android.net.Uri.encode(displayUrl)
                    viewModel.navigate(target)
                    controlsVisible = false
                },
                onDesktopSiteChange = { enabled ->
                    desktopSiteEnabled = enabled
                    viewModel.setDesktopSite(enabled)
                },
                onBlockingChange = viewModel::setBlockingEnabled,
                onZoomChange = viewModel::changePageTextZoom,
                onOpenAppSettings = {
                    controlsVisible = false
                    homeSettingsVisible = true
                }
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
                onDismiss = viewModel::dismissAnswer,
                onOpenWeb = {
                    val query = state.answer?.query.orEmpty()
                    viewModel.dismissAnswer()
                    if (query.isNotBlank()) viewModel.navigate(query)
                },
                onOpenTabs = viewModel::toggleTabStack,
                onNewTab = viewModel::newTab,
                onOpenControls = { controlsVisible = true },
                tabCount = state.tabs.size
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
}
