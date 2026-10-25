package com.dude555afk.xeno.ui.browser

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.FindInPage
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.dude555afk.xeno.PrivateBrowserActivity
import com.dude555afk.xeno.ui.answersheet.AnswerSheet
import com.dude555afk.xeno.core.model.SiteCapability
import com.dude555afk.xeno.ui.cmdbar.CommandPalette
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
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) viewModel.setSitePermission(SiteCapability.CAMERA, false)
    }
    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) viewModel.setSitePermission(SiteCapability.MICROPHONE, false)
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.none { it }) viewModel.setSitePermission(SiteCapability.LOCATION, false)
    }

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

        // The new-tab surface owns its own bottom search control, like Arc Search.
        val isHome = state.activeTab?.url.isNullOrBlank() && state.load.url.isBlank()
        if (isHome) {
            HomeScreen()
        }

        // Keep private browsing visibly distinct without a permanent top toolbar.
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
        }

        // Arc Search's defining control layout: tabs left, new search center,
        // and conventional browser controls behind the upward chevron.
        FloatingBar(
            loadProgress = state.load.progress,
            loading = state.load.loading,
            accent = accent,
            canGoBack = state.canGoBack,
            canGoForward = state.canGoForward,
            displayUrl = state.load.url.ifBlank { state.activeTab?.url.orEmpty() },
            onOpenPalette = viewModel::openPalette,
            onNewTab = viewModel::newTab,
            onOpenMenu = { viewModel.openPanel(BrowserPanel.MENU) },
            onOpenTabs = viewModel::toggleTabStack,
            onOpenReader = viewModel::openReaderMode,
            onOpenFind = viewModel::openFindInPage,
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
            currentSiteHost = state.currentSiteHost,
            sitePermissions = state.sitePermissions,
            currentTitle = state.load.title.ifBlank { state.activeTab?.displayTitle.orEmpty() },
            currentUrl = state.load.url.ifBlank { state.activeTab?.url.orEmpty() },
            desktopSiteEnabled = state.desktopSiteEnabled,
            downloads = state.downloads,
            blockingEnabled = state.blockingEnabled,
            thirdPartyCookiesEnabled = state.thirdPartyCookiesEnabled,
            fingerprintingProtectionEnabled = state.fingerprintingProtectionEnabled,
            provider = viewModel.providerConfig(),
            onProviderSelected = viewModel::providerConfigFor,
            translationLanguage = state.translationLanguage,
            settingsError = state.settingsError,
            onDismiss = viewModel::closePanel,
            onOpenPanel = viewModel::openPanel,
            onOpenReader = viewModel::openReaderMode,
            onOpenFind = viewModel::openFindInPage,
            onCopyCurrentUrl = viewModel::copyCurrentUrl,
            onShareCurrentUrl = viewModel::shareCurrentUrl,
            onToggleDesktopSite = viewModel::toggleDesktopSite,
            onOpenSiteSettings = viewModel::openSiteSettings,
            onSitePermissionChange = { capability, enabled ->
                viewModel.setSitePermission(capability, enabled)
                if (enabled) {
                    when (capability) {
                        SiteCapability.CAMERA -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        SiteCapability.MICROPHONE -> microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        SiteCapability.LOCATION -> locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        SiteCapability.JAVASCRIPT, SiteCapability.THIRD_PARTY_COOKIES -> Unit
                    }
                }
            },
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
            onFingerprintingProtectionChange = viewModel::setFingerprintingProtectionEnabled,
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
 * Arc Search-style bottom controls. The persistent row stays deliberately tiny;
 * address/navigation controls are revealed only when the chevron is opened.
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
    onNewTab: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenReader: () -> Unit,
    onOpenFind: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReloadOrStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        if (expanded) {
            Surface(
                shape = PillShape,
                color = Color(0xF2161616),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.13f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 5.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            enabled = canGoBack,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White.copy(alpha = if (canGoBack) 0.9f else 0.28f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(
                            onClick = onForward,
                            enabled = canGoForward,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.ArrowForward,
                                contentDescription = "Forward",
                                tint = Color.White.copy(alpha = if (canGoForward) 0.9f else 0.28f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(PillShape)
                                .clickable(onClick = onOpenPalette)
                                .padding(horizontal = 8.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.66f),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = displayUrl.ifBlank { "Search or enter address" },
                                color = if (displayUrl.isBlank()) Color.White.copy(alpha = 0.48f) else Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        IconButton(onClick = onReloadOrStop, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = if (loading) Icons.Rounded.Close else Icons.Rounded.Refresh,
                                contentDescription = if (loading) "Stop loading" else "Reload page",
                                tint = Color.White.copy(alpha = 0.78f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(onClick = onOpenMenu, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = "Browser menu",
                                tint = Color.White.copy(alpha = 0.78f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (loading) {
                        LinearProgressIndicator(
                            progress = { loadProgress },
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = accent,
                            trackColor = Color.Transparent
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 7.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExpandedTool(Icons.Rounded.MenuBook, "Reader", onOpenReader)
                        ExpandedTool(Icons.Rounded.FindInPage, "Find", onOpenFind)
                        ExpandedTool(Icons.Rounded.MoreHoriz, "More", onOpenMenu)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xF2232323),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.13f)),
                onClick = onOpenTabs,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Layers,
                        contentDescription = "Open tabs",
                        tint = Color.White.copy(alpha = 0.91f),
                        modifier = Modifier.size(23.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = Color(0xFF303030),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                onClick = onNewTab,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "New search",
                        tint = Color.White,
                        modifier = Modifier.size(27.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = Color(0xF2232323),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.13f)),
                onClick = { expanded = !expanded },
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                        contentDescription = if (expanded) "Collapse browser controls" else "Show browser controls",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedTool(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Color.White.copy(alpha = 0.82f), modifier = Modifier.size(19.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
    }
}
