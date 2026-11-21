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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.viewinterop.AndroidView
import com.dude555afk.xeno.ui.answersheet.AnswerSheet
import com.dude555afk.xeno.ui.arc.ArcFindInPageBar
import com.dude555afk.xeno.ui.arc.ArchiveSheet
import com.dude555afk.xeno.ui.arc.ArcPageAddress
import com.dude555afk.xeno.ui.arc.ArcAiWebTogglePill
import com.dude555afk.xeno.ui.arc.ArcSettingsScreen
import com.dude555afk.xeno.ui.arc.ArcSiteActionsSheet
import com.dude555afk.xeno.ui.arc.LocalArcMotionEnabled
import com.dude555afk.xeno.ui.arc.PixelArcHomeScreen
import com.dude555afk.xeno.ui.arc.PixelArcAmbientBackground
import com.dude555afk.xeno.ui.arc.PixelBottomDock
import com.dude555afk.xeno.ui.arc.VoiceSearchOverlay
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
    val desktopSiteEnabled = state.desktopSiteEnabled
    var archiveVisible by remember { mutableStateOf(false) }
    var voiceVisible by remember { mutableStateOf(false) }
    var voiceHoldReleased by remember { mutableStateOf(false) }
    var answerWebMode by remember { mutableStateOf(false) }
    var pinchScale by remember(state.activeTab?.id) { mutableFloatStateOf(1f) }
    val context = LocalContext.current
    val density = LocalDensity.current
    val coordinateScale = (LocalConfiguration.current.screenWidthDp / 462f).coerceIn(0.75f, 1.1f)
    val interactionScope = rememberCoroutineScope()
    val tabSwipeOffset = remember { Animatable(0f) }
    val scrubTabs: (Int) -> Unit = { direction ->
        interactionScope.launch {
            val width = with(density) { (478f * coordinateScale).dp.toPx() }
            tabSwipeOffset.animateTo(direction * width, animationSpec = tween(140))
            viewModel.selectAdjacentTab(direction)
            tabSwipeOffset.snapTo(-direction * width)
            tabSwipeOffset.animateTo(
                0f,
                animationSpec = spring(stiffness = 350f, dampingRatio = 0.9f)
            )
        }
    }
    val compactPage = state.paletteVisible || state.tabStackVisible
    val pinchProgress = ((1f - pinchScale) / 0.35f).coerceIn(0f, 1f)
    val pageScale by animateFloatAsState(
        targetValue = when {
            compactPage -> 0.6364f
            state.summaryVisible -> 0.62f
            pinchScale < 1f -> 1f - (0.38f * pinchProgress)
            else -> 1f
        },
        animationSpec = if (state.animationsEnabled) spring(stiffness = 380f, dampingRatio = 1f) else snap(),
        label = "arc-shared-page-scale"
    )
    val pageOffsetY by animateFloatAsState(
        targetValue = when {
            state.paletteVisible -> 0f
            state.summaryVisible -> -132f
            else -> 0f
        },
        animationSpec = if (state.animationsEnabled) spring(stiffness = 380f, dampingRatio = 1f) else snap(),
        label = "arc-shared-page-offset"
    )
    val chromeDockOffset by animateFloatAsState(
        targetValue = if (state.chromeVisible) 0f else 85f * coordinateScale,
        animationSpec = if (state.animationsEnabled) tween(220, easing = FastOutSlowInEasing) else snap(),
        label = "arc-dock-collapse-offset"
    )
    val pageFoldRotation by animateFloatAsState(
        targetValue = if (state.summaryVisible) -55f else -55f * pinchProgress,
        animationSpec = if (state.animationsEnabled) spring(stiffness = 320f, dampingRatio = 0.9f) else snap(),
        label = "arc-page-fold"
    )
    LaunchedEffect(state.answer) {
        if (state.answer == null) answerWebMode = false
    }

    LaunchedEffect(state.activeTab?.id, state.activeTab?.type, state.activeTab?.query) {
        if (state.activeTab?.type == "ai") viewModel.ensureActiveAiTabLoaded()
    }

    BackHandler(
        enabled = voiceVisible || homeSettingsVisible || findInPageVisible || controlsVisible ||
            state.summaryVisible || state.answer != null || state.paletteVisible ||
            state.tabStackVisible || state.canGoBack
    ) {
        when {
            voiceVisible -> voiceVisible = false
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
    val currentHost = remember(displayUrl) {
        runCatching { java.net.URI(displayUrl).host?.removePrefix("www.") }.getOrNull().orEmpty()
    }
    val isSearchResultsPage = currentHost.contains("duckduckgo.com", true) ||
        currentHost.contains("google.", true) || currentHost.contains("bing.com", true)
    val addressLabel = if (isSearchResultsPage) {
        state.activeTab?.query?.takeIf { it.isNotBlank() } ?: currentHost
    } else {
        currentHost.ifBlank { state.load.title.ifBlank { state.activeTab?.title.orEmpty() } }
    }
    val showingAiAnswer = state.activeTab?.type == "ai" && state.answer != null && !answerWebMode
    val isHome = !showingAiAnswer && (displayUrl.isBlank() || displayUrl == "about:blank")

    CompositionLocalProvider(LocalArcMotionEnabled provides state.animationsEnabled) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // The live WebView stays mounted; Compose only transforms its surface.
            if (state.summaryVisible || pinchScale < 1f) PixelArcAmbientBackground()
            AndroidView(
                modifier = Modifier.fillMaxSize()
                    .pointerInput(state.activeTab?.id, state.summaryVisible) {
                        awaitEachGesture {
                            var previousSpan = 0f
                            var gestureScale = 1f
                            do {
                                val event = awaitPointerEvent()
                                val pointers = event.changes.filter { it.pressed }
                                if (pointers.size >= 2) {
                                    val centerX = pointers.map { it.position.x }.average().toFloat()
                                    val centerY = pointers.map { it.position.y }.average().toFloat()
                                    val span = pointers.map {
                                        kotlin.math.hypot(it.position.x - centerX, it.position.y - centerY)
                                    }.average().toFloat()
                                    if (previousSpan > 0f && span < previousSpan) {
                                        gestureScale = (gestureScale * (span / previousSpan)).coerceIn(0.5f, 1f)
                                        pinchScale = gestureScale
                                    }
                                    previousSpan = span
                                    event.changes.forEach { it.consume() }
                                } else previousSpan = 0f
                            } while (event.changes.any { it.pressed })
                            val shouldSummarize = gestureScale <= 0.65f && !state.summaryVisible
                            pinchScale = 1f
                            if (shouldSummarize) viewModel.summarizeCurrentPage()
                        }
                    }
                    .graphicsLayer {
                    scaleX = pageScale
                    scaleY = pageScale
                    translationY = with(density) { pageOffsetY.dp.toPx() }
                    translationX = tabSwipeOffset.value
                    rotationX = pageFoldRotation
                    cameraDistance = if (state.summaryVisible || pinchScale < 1f) 12f * density.density else 900f * density.density
                    transformOrigin = if (state.summaryVisible) {
                        TransformOrigin(0.5f, 0.08f)
                    } else {
                        TransformOrigin(0.5f, 0.5f)
                    }
                    shape = RoundedCornerShape(
                        when {
                            state.summaryVisible || pinchScale < 1f -> 20.dp
                            compactPage -> 15.dp
                            else -> 0.dp
                        }
                    )
                    clip = compactPage || state.summaryVisible || pinchScale < 1f
                },
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

            if (showingAiAnswer) {
                // AnswerSheet owns its dock; do not draw the empty-home or
                // regular web dock underneath an active AI tab.
            } else if (isHome) {
                PixelArcHomeScreen(
                    onOpenSearch = viewModel::openPalette,
                    onOpenTabs = viewModel::toggleTabStack,
                    onOpenSettings = { homeSettingsVisible = true },
                    tabCount = state.tabs.size,
                    onVoiceStart = { voiceVisible = true; voiceHoldReleased = false },
                    onVoiceEnd = { voiceHoldReleased = true },
                    onSwipeTab = scrubTabs
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

                ArcPageAddress(
                    url = displayUrl,
                    title = state.load.title.ifBlank { state.activeTab?.title.orEmpty() },
                    displayLabel = addressLabel,
                    onClick = viewModel::openPalette,
                    modifier = Modifier.align(Alignment.TopCenter)
                        .graphicsLayer {
                            scaleX = pageScale
                            scaleY = pageScale
                            translationY = with(density) { pageOffsetY.dp.toPx() }
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                        .padding(top = 54.dp)
                )

                AnimatedVisibility(
                    visible = state.chromeVisible,
                    modifier = Modifier.fillMaxSize(),
                    enter = if (state.animationsEnabled) fadeIn(tween(140)) + slideInVertically(
                        initialOffsetY = { with(density) { (85f * coordinateScale).dp.roundToPx() } },
                        animationSpec = tween(220)
                    ) else EnterTransition.None,
                    exit = if (state.animationsEnabled) fadeOut(tween(110)) + slideOutVertically(
                        targetOffsetY = { with(density) { (85f * coordinateScale).dp.roundToPx() } },
                        animationSpec = tween(220)
                    ) else ExitTransition.None
                ) {
                    Box(Modifier.fillMaxSize()) {
                        PixelBottomDock(
                            tabCount = state.tabs.size,
                            homeMode = false,
                            onOpenTabs = viewModel::toggleTabStack,
                            onNewTab = viewModel::newTab,
                            onOpenControls = { controlsVisible = true },
                            onVoiceStart = { voiceVisible = true; voiceHoldReleased = false },
                            onVoiceEnd = { voiceHoldReleased = true },
                            onSwipeTab = scrubTabs,
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
                searchEngine = state.searchEngine,
                languageMode = state.languageMode,
                archiveAfter = state.archiveAfter,
                alwaysOpenAppLinks = state.alwaysOpenAppLinks,
                allowGoogleTranslate = state.allowGoogleTranslate,
                forceDark = state.forceDark,
                appIcon = state.appIcon,
                onSearchEngineChange = viewModel::setSearchEngine,
                onLanguageModeChange = viewModel::setLanguageMode,
                onArchiveAfterChange = viewModel::setArchiveAfter,
                onAlwaysOpenAppLinksChange = viewModel::setAlwaysOpenAppLinks,
                onAllowGoogleTranslateChange = viewModel::setAllowGoogleTranslate,
                onForceDarkChange = viewModel::setForceDark,
                onAppIconChange = viewModel::setAppIcon,
                onOpenArchive = { homeSettingsVisible = false; archiveVisible = true },
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
                    val urls = state.tabs
                        .asSequence()
                        .filter { !it.isIncognito && it.type != "ai" }
                        .map { it.url }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .toList()
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
                tabHints = state.tabs.filter { it.hasContent && it.isIncognito == state.incognitoMode },
                historyHints = if (state.incognitoMode) emptyList() else state.historyHints,
                loading = state.answerStreaming,
                isQuestion = state.isQuestion,
                accent = accent,
                onNavigate = viewModel::navigate,
                onAsk = viewModel::ask,
                onOpenTab = { tab ->
                    viewModel.selectTab(tab)
                    viewModel.closePalette()
                },
                onDismiss = viewModel::closePalette,
                incognitoMode = state.incognitoMode,
                onToggleIncognito = viewModel::toggleIncognitoMode
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
                onClose = viewModel::archiveTab,
                onNewTab = viewModel::newTab,
                onDismiss = viewModel::toggleTabStack,
                onOpenSettings = { homeSettingsVisible = true },
                onUndoClose = viewModel::undoCloseLastTab,
                onVoiceStart = { voiceVisible = true; voiceHoldReleased = false },
                onVoiceEnd = { voiceHoldReleased = true },
                onSwipeTab = scrubTabs
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
                blockedAdsCount = state.blockedAdsCount,
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
                    if (state.allowGoogleTranslate) {
                        val target = "https://translate.google.com/translate?sl=auto&tl=en&u=" +
                            android.net.Uri.encode(displayUrl)
                        viewModel.navigate(target)
                    }
                    controlsVisible = false
                },
                onDesktopSiteChange = viewModel::setDesktopSite,
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

            if (state.answer != null && !answerWebMode) AnswerSheet(
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
                    if (query.isNotBlank()) {
                        answerWebMode = true
                        viewModel.navigate(query)
                    }
                },
                onOpenTabs = viewModel::toggleTabStack,
                onNewTab = viewModel::newTab,
                onOpenControls = { controlsVisible = true },
                tabCount = state.tabs.size,
                onVoiceStart = { voiceVisible = true; voiceHoldReleased = false },
                onVoiceEnd = { voiceHoldReleased = true },
                onSwipeTab = scrubTabs
            )

            if (state.answer != null && answerWebMode) {
                ArcAiWebTogglePill(
                    webMode = true,
                    onToggle = {
                        answerWebMode = false
                        viewModel.returnToAnswerMode()
                    },
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .padding(end = (17f * coordinateScale).dp, bottom = (90f * coordinateScale).dp)
                        .graphicsLayer { translationY = with(density) { chromeDockOffset.dp.toPx() } }
                )
            }

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

            ArchiveSheet(
                visible = archiveVisible,
                tabs = state.archivedTabs,
                onRestore = { archived ->
                    archiveVisible = false
                    viewModel.restoreArchivedTab(archived)
                },
                onDelete = viewModel::deleteArchivedTab,
                onDismiss = { archiveVisible = false },
                pinnedPages = state.pinnedTabs,
                onOpenPinned = { page ->
                    archiveVisible = false
                    viewModel.navigate(page.url)
                },
                onUnpin = { page -> viewModel.unpinPage(page.url) }
            )

            VoiceSearchOverlay(
                visible = voiceVisible,
                holdReleased = voiceHoldReleased,
                onDismiss = { voiceVisible = false; voiceHoldReleased = false },
                onResult = viewModel::ask
            )
        }
    }
}
