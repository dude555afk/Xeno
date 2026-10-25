package com.dude555afk.xeno.ui.browser

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
 * Chrome hides on scroll — rule 3 from docs/DESIGN.md, and it's what makes the
 * content feel like the app rather than the page feeling like a document
 * inside a shell.
 */
@Composable
fun BrowserScreen(
    state: BrowserUiState,
    viewModel: BrowserViewModel,
    accent: Color
) {
    val context = LocalContext.current

    // Android back should unwind browser surfaces before leaving the app.
    BackHandler(
        enabled = state.paletteVisible || state.tabStackVisible ||
            state.panel != null || state.findVisible || state.readerVisible || state.translationConsentVisible || state.translationVisible || state.summaryConsentVisible || state.summaryVisible || state.answer != null || state.canGoBack
    ) {
        when {
            state.paletteVisible -> viewModel.closePalette()
            state.tabStackVisible -> viewModel.toggleTabStack()
            state.findVisible -> viewModel.closeFindInPage()
            state.readerVisible -> viewModel.closeReaderMode()
            state.translationVisible -> viewModel.closeTranslation()
            state.translationConsentVisible -> viewModel.cancelTranslationConsent()
            state.panel != null -> viewModel.closePanel()
            state.summaryConsentVisible -> viewModel.cancelSummaryConsent()
            state.summaryVisible -> viewModel.dismissSummary()
            state.answer != null -> viewModel.dismissAnswer()
            state.canGoBack -> viewModel.goBack()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        // The page. The factory returns a container so the session pool can do
        // the actual WebView ownership — reusing a view across recompositions
        // is the whole point.
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

        // First-run and empty tabs get a designed landing surface instead of a dead black canvas.
        if (state.activeTab?.url.isNullOrBlank() && state.load.url.isBlank()) {
            HomeScreen(
                accent = accent,
                onSearch = viewModel::openPalette
            )
        }

        // Spaces live in the regular browser. A private session instead carries
        // a visible privacy marker so it cannot be confused with normal tabs.
        if (state.isPrivateMode) {
            Surface(
                modifier = Modifier.align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = Dimens.spaceL, top = Dimens.spaceM),
                shape = PillShape,
                color = Color(0xFF151B10),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC9F63E).copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = Color(0xFFC9F63E), modifier = Modifier.size(16.dp))
                    Text("PRIVATE", modifier = Modifier.padding(start = 8.dp), color = Color(0xFFC9F63E), style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp)
                }
            }
        } else {
            SpacesBar(
                spaces = state.spaces,
                activeSpaceId = state.activeSpace?.id ?: 0L,
                onSelect = viewModel::selectSpace,
                modifier = Modifier.align(Alignment.TopStart).windowInsetsPadding(WindowInsets.statusBars).padding(top = Dimens.spaceM)
            )
        }

        // Tab stack — top right.
        IconButton(
            onClick = viewModel::toggleTabStack,
            modifier = Modifier.align(Alignment.TopEnd).windowInsetsPadding(WindowInsets.statusBars).padding(top = Dimens.spaceS, end = 56.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Layers,
                contentDescription = "Tabs",
                tint = Color.White.copy(alpha = 0.7f)
            )
        }

        IconButton(
            onClick = { viewModel.openPanel(BrowserPanel.MENU) },
            modifier = Modifier.align(Alignment.TopEnd).windowInsetsPadding(WindowInsets.statusBars).padding(top = Dimens.spaceS, end = Dimens.spaceM)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = "Browser menu",
                tint = Color.White.copy(alpha = 0.82f)
            )
        }

        // The floating bar.
        FloatingBar(
            loadProgress = state.load.progress,
            loading = state.load.loading,
            accent = accent,
            canGoBack = state.canGoBack,
            canGoForward = state.canGoForward,
            displayUrl = state.load.url.ifBlank { state.activeTab?.url.orEmpty() },
            onOpenPalette = viewModel::openPalette,
            onSummarize = { viewModel.summarizeCurrentPage() },
            onBack = viewModel::goBack,
            onForward = viewModel::goForward,
            onReloadOrStop = viewModel::refreshOrStop,
            modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars)
        )

        if (state.pageError != null) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = 112.dp, start = 24.dp, end = 24.dp)
                    .clickable(onClick = viewModel::refreshOrStop),
                shape = PillShape,
                color = Color(0xFF1C1C1C),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
            ) {
                Text(
                    text = "Page couldn't load · Tap to retry",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (state.findVisible) {
            FindInPageBar(
                query = state.findQuery,
                matches = state.findMatches,
                activeMatch = state.findActiveMatch,
                onQueryChange = viewModel::updateFindQuery,
                onPrevious = { viewModel.findNext(false) },
                onNext = { viewModel.findNext(true) },
                onDismiss = viewModel::closeFindInPage,
                modifier = Modifier.align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 60.dp, start = 16.dp, end = 16.dp)
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
            onCopy = { viewModel.copySummary() },
            onDismiss = viewModel::dismissSummary
        )

        BrowserToolsSheet(
            panel = state.panel,
            history = state.historyEntries,
            bookmarks = state.bookmarks,
            spaces = state.spaces,
            activeSpaceId = state.activeSpace?.id,
            isPrivateMode = state.isPrivateMode,
            currentTitle = state.load.title.ifBlank { state.activeTab?.displayTitle.orEmpty() },
            currentUrl = state.load.url.ifBlank { state.activeTab?.url.orEmpty() },
            downloads = state.downloads,
            blockingEnabled = state.blockingEnabled,
            thirdPartyCookiesEnabled = state.thirdPartyCookiesEnabled,
            provider = viewModel.providerConfig(),
            translationLanguage = state.translationLanguage,
            settingsError = state.settingsError,
            onDismiss = viewModel::closePanel,
            onOpenPanel = viewModel::openPanel,
            onOpenReader = viewModel::openReaderMode,
            onOpenFind = viewModel::openFindInPage,
            onTogglePrivateMode = {
                if (state.isPrivateMode) {
                    (context as? Activity)?.finish()
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    viewModel.closePanel()
                    context.startActivity(Intent(context, PrivateBrowserActivity::class.java))
                } else {
                    Toast.makeText(context, "Private browsing requires Android 9 or newer.", Toast.LENGTH_LONG).show()
                }
            },
            onHistoryClick = viewModel::openHistoryEntry,
            onBookmarkClick = viewModel::openBookmark,
            onRemoveBookmark = viewModel::removeBookmark,
            onSelectSpace = { space -> viewModel.selectSpace(space); viewModel.closePanel() },
            onCreateSpace = viewModel::createSpace,
            onRenameSpace = viewModel::renameSpace,
            onDeleteSpace = viewModel::deleteSpace,
            onAddBookmark = viewModel::addCurrentBookmark,
            onClearHistory = viewModel::clearHistory,
            onClearBrowsingData = viewModel::clearBrowsingData,
            onBlockingChange = viewModel::setBlockingEnabled,
            onThirdPartyCookiesChange = viewModel::setThirdPartyCookiesEnabled,
            onSaveProvider = viewModel::saveProviderConfig,
            onTranslationLanguageChange = viewModel::setTranslationLanguage,
            onTranslate = viewModel::requestTranslation,
            onRefreshDownloads = viewModel::refreshDownloads,
            onOpenDownload = viewModel::openDownloadedFile,
            onRemoveDownload = viewModel::removeDownload
        )

        ReaderSheet(
            visible = state.readerVisible,
            loading = state.readerLoading,
            content = state.readerContent,
            error = state.readerError,
            onDismiss = viewModel::closeReaderMode
        )

        TranslationSheet(
            visible = state.translationVisible,
            loading = state.translationLoading,
            language = state.translationLanguage,
            title = state.translationConsentTitle.ifBlank { state.activeTab?.displayTitle.orEmpty() },
            text = state.translationText,
            error = state.translationError,
            notice = state.translationNotice,
            onDismiss = viewModel::closeTranslation
        )

        if (state.translationConsentVisible) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = viewModel::cancelTranslationConsent,
                title = { Text("Translate with AI?") },
                text = {
                    Text(
                        "Xeno will extract readable text from \"${state.translationConsentTitle}\" and send it to \"${viewModel.providerConfig().displayName}\" to translate it into \"${state.translationLanguage}\". Cookies, passwords, browsing history, and unrelated tabs are not included. The provider's own data policies apply."
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = viewModel::confirmTranslationConsent) {
                        Text("Translate")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = viewModel::cancelTranslationConsent) {
                        Text("Cancel")
                    }
                },
                containerColor = Color(0xFF171717),
                titleContentColor = Color.White,
                textContentColor = Color.White.copy(alpha = 0.8f)
            )
        }

        if (state.summaryConsentVisible) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = viewModel::cancelSummaryConsent,
                title = { Text("Send this page to AI?") },
                text = {
                    Text(
                        "Xeno will extract readable text from \"${state.summaryConsentTitle}\" and send it to \"${viewModel.providerConfig().displayName}\" to create a summary. Cookies, passwords, browsing history, and unrelated tabs are not included. The provider's own data policies apply."
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = viewModel::confirmSummaryConsent) {
                        Text("Continue")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = viewModel::cancelSummaryConsent) {
                        Text("Cancel")
                    }
                },
                containerColor = Color(0xFF171717),
                titleContentColor = Color.White,
                textContentColor = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * The floating bar — back, address field, summarize. A pill, like everything.
 *
 * The address field shows the live page URL, not the palette's in-progress
 * query. Showing `state.query` here meant the bar kept whatever the user last
 * typed long after they had dismissed the palette and moved on.
 */
@Composable
private fun FloatingBar(
    loadProgress: Float,
    loading: Boolean,
    accent: Color,
    canGoBack: Boolean,
    canGoForward: Boolean,
    displayUrl: String,
    onOpenPalette: () -> Unit,
    onSummarize: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReloadOrStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = Dimens.spaceL, vertical = Dimens.spaceL)) {
        Surface(
            shape = PillShape,
            color = Color(0xF20A0A0A),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.spaceS, vertical = Dimens.spaceS),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    // Only shown when there is somewhere to go back to. A
                    // permanently-visible back arrow that does nothing is worse
                    // than no arrow at all.
                    if (canGoBack) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    if (canGoForward) {
                        IconButton(onClick = onForward) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowForward,
                                contentDescription = "Forward",
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(Dimens.spaceS))
                        Text(
                            text = displayUrl.ifBlank { "Search or ask anything" },
                            color = if (displayUrl.isBlank()) Color.White.copy(alpha = 0.4f)
                            else Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = onReloadOrStop) {
                        Icon(
                            imageVector = if (loading) Icons.Rounded.Close else Icons.Rounded.Refresh,
                            contentDescription = if (loading) "Stop loading" else "Reload page",
                            tint = Color.White.copy(alpha = 0.78f)
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
