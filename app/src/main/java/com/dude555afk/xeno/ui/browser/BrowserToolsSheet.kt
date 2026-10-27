package com.dude555afk.xeno.ui.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dude555afk.xeno.core.ai.ProviderConfig
import com.dude555afk.xeno.core.ai.ModelInfo
import com.dude555afk.xeno.core.ai.ProviderPresets
import com.dude555afk.xeno.core.model.Bookmark
import com.dude555afk.xeno.core.model.Space
import com.dude555afk.xeno.core.model.SiteCapability
import com.dude555afk.xeno.core.model.SitePermissions
import com.dude555afk.xeno.core.storage.HistoryEntity
import com.dude555afk.xeno.core.storage.CollectedPage
import com.dude555afk.xeno.ui.theme.StrokeSubtle
import com.dude555afk.xeno.ui.theme.TextPrimary
import com.dude555afk.xeno.ui.theme.TextSecondary
import java.util.Locale

enum class BrowserPanel { MENU, HISTORY, BOOKMARKS, DOWNLOADS, ARCHIVE, PINNED, PRIVACY, SETTINGS, AI, TRANSLATE, SPACES, SITE_SETTINGS }

data class BrowserDownload(
    val id: Long,
    val title: String,
    val sourceUrl: String,
    val localUri: String,
    val mimeType: String,
    val status: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val reason: Int
)

@Composable
fun BrowserToolsSheet(
    panel: BrowserPanel?,
    history: List<HistoryEntity>,
    bookmarks: List<Bookmark>,
    spaces: List<Space>,
    activeSpaceId: Long?,
    isPrivateMode: Boolean,
    autoOpenKeyboard: Boolean,
    searchEngineId: String,
    currentSiteHost: String,
    sitePermissions: SitePermissions,
    currentTitle: String,
    currentUrl: String,
    desktopSiteEnabled: Boolean,
    downloads: List<BrowserDownload>,
    archiveEntries: List<CollectedPage>,
    pinnedPages: List<CollectedPage>,
    archiveRetentionHours: Int,
    blockingEnabled: Boolean,
    thirdPartyCookiesEnabled: Boolean,
    fingerprintingProtectionEnabled: Boolean,
    provider: ProviderConfig,
    availableModels: List<ModelInfo>,
    availableModelsProviderId: String?,
    modelsLoading: Boolean,
    onFetchModels: (ProviderConfig) -> Unit,
    onProviderSelected: (String) -> ProviderConfig,
    translationLanguage: String,
    settingsError: String?,
    onDismiss: () -> Unit,
    onOpenPanel: (BrowserPanel) -> Unit,
    onAutoOpenKeyboardChange: (Boolean) -> Unit,
    onSearchEngineChange: (String) -> Unit,
    onOpenDefaultAppsSettings: () -> Unit,
    onPinCurrentPage: () -> Unit,
    onArchiveCurrentTab: () -> Unit,
    onUnpinPage: (CollectedPage) -> Unit,
    onOpenPinnedPage: (CollectedPage) -> Unit,
    onOpenArchivedPage: (CollectedPage) -> Unit,
    onRemoveArchivedPage: (CollectedPage) -> Unit,
    onArchiveRetentionChange: (Int) -> Unit,
    onOpenReader: () -> Unit,
    onOpenFind: () -> Unit,
    onCopyCurrentUrl: () -> Unit,
    onShareCurrentUrl: () -> Unit,
    onToggleDesktopSite: () -> Unit,
    onOpenSiteSettings: () -> Unit,
    onSitePermissionChange: (SiteCapability, Boolean) -> Unit,
    onTogglePrivateMode: () -> Unit,
    onHistoryClick: (HistoryEntity) -> Unit,
    onBookmarkClick: (Bookmark) -> Unit,
    onRemoveBookmark: (Bookmark) -> Unit,
    onSelectSpace: (Space) -> Unit,
    onCreateSpace: (String) -> Unit,
    onRenameSpace: (Space, String) -> Unit,
    onDeleteSpace: (Space) -> Unit,
    onAddBookmark: () -> Unit,
    onClearHistory: () -> Unit,
    onClearBrowsingData: () -> Unit,
    onBlockingChange: (Boolean) -> Unit,
    onThirdPartyCookiesChange: (Boolean) -> Unit,
    onFingerprintingProtectionChange: (Boolean) -> Unit,
    onSaveProvider: (ProviderConfig) -> Unit,
    onTranslationLanguageChange: (String) -> Unit,
    onTranslate: () -> Unit,
    onRefreshDownloads: () -> Unit,
    onOpenDownload: (BrowserDownload) -> Unit,
    onRemoveDownload: (BrowserDownload) -> Unit
) {
    AnimatedVisibility(
        visible = panel != null,
        enter = fadeIn(androidx.compose.animation.core.tween(210)) +
            androidx.compose.animation.slideInVertically(
                initialOffsetY = { it / 6 },
                animationSpec = androidx.compose.animation.core.tween(250)
            ) +
            androidx.compose.animation.scaleIn(
                initialScale = 0.985f,
                animationSpec = androidx.compose.animation.core.tween(220)
            ),
        exit = fadeOut(androidx.compose.animation.core.tween(140)) +
            androidx.compose.animation.slideOutVertically(
                targetOffsetY = { it / 10 },
                animationSpec = androidx.compose.animation.core.tween(160)
            ) +
            androidx.compose.animation.scaleOut(
                targetScale = 0.99f,
                animationSpec = androidx.compose.animation.core.tween(150)
            )
    ) {
        val isBottomSheet = panel == BrowserPanel.MENU || panel == BrowserPanel.SITE_SETTINGS
        Box(
            Modifier.fillMaxSize()
                .background(if (isBottomSheet) Color.Black.copy(alpha = 0.22f) else Color(0xFF191821))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = if (isBottomSheet) Alignment.BottomCenter else Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .fillMaxHeight(if (isBottomSheet) 0.84f else 1f)
                    .then(
                        if (isBottomSheet) Modifier
                        else Modifier
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { },
                shape = if (isBottomSheet) RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp) else RoundedCornerShape(0.dp),
                color = Color(0xFF191821),
                border = BorderStroke(1.dp, StrokeSubtle),
                shadowElevation = if (isBottomSheet) 18.dp else 0.dp
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (panel != null && panel != BrowserPanel.MENU) {
                            IconButton(
                                onClick = {
                                    when (panel) {
                                        BrowserPanel.AI -> onOpenPanel(BrowserPanel.SETTINGS)
                                        BrowserPanel.SETTINGS -> onDismiss()
                                        else -> onOpenPanel(BrowserPanel.MENU)
                                    }
                                }
                            ) {
                                Icon(Icons.Rounded.ArrowBack, "Back", tint = TextSecondary)
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                when (panel) {
                                    BrowserPanel.MENU -> "Browser controls"
                                    BrowserPanel.HISTORY -> "History"
                                    BrowserPanel.BOOKMARKS -> "Bookmarks"
                                    BrowserPanel.DOWNLOADS -> "Downloads"
                                    BrowserPanel.ARCHIVE -> "Archive"
                                    BrowserPanel.PINNED -> "Pinned pages"
                                    BrowserPanel.PRIVACY -> "Privacy"
                                    BrowserPanel.SETTINGS -> "Settings"
                                    BrowserPanel.AI -> "AI and Browse for Me"
                                    BrowserPanel.TRANSLATE -> "Translate page"
                                    BrowserPanel.SPACES -> "Spaces"
                                    BrowserPanel.SITE_SETTINGS -> "Site settings"
                                    null -> "Xeno"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                when (panel) {
                                    BrowserPanel.MENU -> "Actions for this page"
                                    BrowserPanel.HISTORY -> if (isPrivateMode) "Temporary private history" else "Stored on this device"
                                    BrowserPanel.BOOKMARKS -> "Your saved pages"
                                    BrowserPanel.DOWNLOADS -> "Files saved by Xeno"
                                    BrowserPanel.ARCHIVE -> "Inactive tabs saved on this device"
                                    BrowserPanel.PINNED -> "Pages you keep close"
                                    BrowserPanel.PRIVACY -> "Protection and local data"
                                    BrowserPanel.SETTINGS -> "Search, privacy and app preferences"
                                    BrowserPanel.AI -> "Provider, model and request settings"
                                    BrowserPanel.TRANSLATE -> "Choose a target language"
                                    BrowserPanel.SPACES -> "Organize your workspaces"
                                    BrowserPanel.SITE_SETTINGS -> currentSiteHost
                                    null -> ""
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, "Close", tint = TextSecondary)
                        }
                    }
                    when (panel) {
                        BrowserPanel.MENU -> Column(
                            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (currentUrl.startsWith("http://") || currentUrl.startsWith("https://")) {
                                MenuAction("Copy link", "Copy the current address", Icons.Rounded.ContentCopy, onCopyCurrentUrl)
                                MenuAction("Share page", "Send this link to another app", Icons.Rounded.Share, onShareCurrentUrl)
                                MenuAction(
                                    if (desktopSiteEnabled) "Desktop site enabled" else "Request desktop site",
                                    "Reload this tab using a desktop user agent",
                                    Icons.Rounded.Computer,
                                    onToggleDesktopSite
                                )
                                if (!isPrivateMode) {
                                    MenuAction(
                                        "Bookmark current page",
                                        currentTitle.ifBlank { currentUrl },
                                        Icons.Rounded.BookmarkAdd,
                                        onAddBookmark
                                    )
                                    val pinnedPage = pinnedPages.firstOrNull { it.url == currentUrl }
                                    MenuAction(
                                        if (pinnedPage == null) "Pin current page" else "Unpin current page",
                                        currentTitle.ifBlank { currentUrl },
                                        Icons.Rounded.Bookmarks
                                    ) {
                                        if (pinnedPage == null) onPinCurrentPage() else onUnpinPage(pinnedPage)
                                    }
                                    MenuAction(
                                        "Archive this tab",
                                        "Save the page and close this tab",
                                        Icons.Rounded.History,
                                        onArchiveCurrentTab
                                    )
                                }
                            }
                            if (!isPrivateMode) {
                                MenuAction("Bookmarks", "${bookmarks.size} saved pages", Icons.Rounded.Bookmarks) { onOpenPanel(BrowserPanel.BOOKMARKS) }
                            }
                            MenuAction("Spaces", "${spaces.size} workspaces", Icons.Rounded.Workspaces) { onOpenPanel(BrowserPanel.SPACES) }
                            MenuAction(
                                if (isPrivateMode) "Close private session" else "New private session",
                                if (isPrivateMode) "Exit and clear temporary browsing data" else "Separate WebView profile, no saved history",
                                Icons.Rounded.Shield,
                                onTogglePrivateMode
                            )
                            MenuAction("History", "Recently visited pages", Icons.Rounded.History) { onOpenPanel(BrowserPanel.HISTORY) }
                            MenuAction("Archive", "${archiveEntries.size} archived pages", Icons.Rounded.History) { onOpenPanel(BrowserPanel.ARCHIVE) }
                            if (!isPrivateMode) {
                                MenuAction("Pinned pages", "${pinnedPages.size} pinned pages", Icons.Rounded.Bookmarks) { onOpenPanel(BrowserPanel.PINNED) }
                            }
                            MenuAction("Downloads", "Files and download status", Icons.Rounded.Download) {
                                onOpenPanel(BrowserPanel.DOWNLOADS)
                                onRefreshDownloads()
                            }
                            MenuAction("Reader mode", "A quiet, readable article view", Icons.Rounded.Article, onOpenReader)
                            MenuAction("Find in page", "Search text on the current page", Icons.Rounded.Search, onOpenFind)
                            if (currentUrl.startsWith("http://") || currentUrl.startsWith("https://")) {
                                MenuAction("Site settings", "Permissions for " + currentSiteHost, Icons.Rounded.Settings, onOpenSiteSettings)
                            }
                            MenuAction("Translate page", "Translate article text with AI", Icons.Rounded.Article) { onOpenPanel(BrowserPanel.TRANSLATE) }
                            MenuAction("Privacy & blockers", "Tracking protection and clear data", Icons.Rounded.Shield) { onOpenPanel(BrowserPanel.PRIVACY) }
                            MenuAction("Settings", "Search, privacy and app preferences", Icons.Rounded.Settings) { onOpenPanel(BrowserPanel.SETTINGS) }
                        }
                        BrowserPanel.ARCHIVE -> Column(
                            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "ARCHIVE INACTIVE TABS AFTER",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                            listOf(
                                12 to "12 hours",
                                24 to "1 day",
                                72 to "3 days",
                                168 to "7 days",
                                720 to "30 days"
                            ).chunked(3).forEach { rowOptions ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowOptions.forEach { (hours, label) ->
                                        FilterChip(
                                            selected = archiveRetentionHours == hours,
                                            onClick = { onArchiveRetentionChange(hours) },
                                            enabled = !isPrivateMode,
                                            label = { Text(label, maxLines = 1) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            if (archiveEntries.isEmpty()) {
                                EmptyState("Your Archive is empty")
                            } else {
                                LazyColumn(
                                    Modifier.weight(1f),
                                    contentPadding = PaddingValues(bottom = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(archiveEntries, key = { it.url }) { page ->
                                        Row(
                                            Modifier.fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable { onOpenArchivedPage(page) }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.History, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                                Text(page.title.ifBlank { page.url }, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(page.url, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            IconButton(onClick = { onOpenArchivedPage(page) }) {
                                                Icon(Icons.Rounded.OpenInNew, "Restore page", tint = TextSecondary)
                                            }
                                            IconButton(onClick = { onRemoveArchivedPage(page) }) {
                                                Icon(Icons.Rounded.DeleteOutline, "Remove from Archive", tint = TextSecondary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        BrowserPanel.PINNED -> {
                            if (pinnedPages.isEmpty()) EmptyState("No pinned pages yet")
                            else LazyColumn(
                                Modifier.weight(1f).fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(pinnedPages, key = { it.url }) { page ->
                                    Row(
                                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                            .clickable { onOpenPinnedPage(page) }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.Bookmarks, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                            Text(page.title.ifBlank { page.url }, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(page.url, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        IconButton(onClick = { onUnpinPage(page) }) {
                                            Icon(Icons.Rounded.DeleteOutline, "Unpin page", tint = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                        BrowserPanel.BOOKMARKS -> {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${bookmarks.size} saved pages", color = TextSecondary, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            }
                            if (bookmarks.isEmpty()) EmptyState("No bookmarks yet")
                            else LazyColumn(
                                Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(bookmarks, key = { it.url }) { bookmark ->
                                    Row(
                                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onBookmarkClick(bookmark) }
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.Bookmarks, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                            Text(bookmark.title.ifBlank { bookmark.url }, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(bookmark.url, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        IconButton(onClick = { onRemoveBookmark(bookmark) }) {
                                            Icon(Icons.Rounded.DeleteOutline, "Remove bookmark", tint = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                        BrowserPanel.HISTORY -> {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${history.size} recent pages", color = TextSecondary, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = onClearHistory) { Text("Clear history") }
                            }
                            if (history.isEmpty()) EmptyState("Nothing in your history yet")
                            else LazyColumn(
                                Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(history, key = { it.id }) { item ->
                                    Row(
                                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onHistoryClick(item) }
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.History, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                            Text(item.title.ifBlank { item.url }, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(item.url, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Icon(Icons.Rounded.OpenInNew, "Open", tint = TextSecondary, modifier = Modifier.size(17.dp))
                                    }
                                }
                            }
                        }
                        BrowserPanel.DOWNLOADS -> {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${downloads.size} downloads", color = TextSecondary, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = onRefreshDownloads) { Text("Refresh") }
                            }
                            if (isPrivateMode) EmptyState("Downloads are disabled in private mode to avoid leaving a system download record.")
                            else if (downloads.isEmpty()) EmptyState("No downloads yet")
                            else LazyColumn(
                                Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(downloads, key = { it.id }) { item ->
                                    Row(Modifier.fillMaxWidth().background(Color(0xFF302D39), RoundedCornerShape(16.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Download, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                            Text(item.title, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(
                                                if (item.totalBytes > 0L) "${item.status} · ${formatBytes(item.downloadedBytes)} / ${formatBytes(item.totalBytes)}" else item.status,
                                                color = TextSecondary, style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        if (item.status == "Completed") IconButton(onClick = { onOpenDownload(item) }) {
                                            Icon(Icons.Rounded.OpenInNew, "Open download", tint = TextSecondary)
                                        }
                                        IconButton(onClick = { onRemoveDownload(item) }) {
                                            Icon(Icons.Rounded.DeleteOutline, "Remove download", tint = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                        BrowserPanel.PRIVACY -> Column(
                            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Block ads and trackers", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                                    Text("Filter lists run locally. Disable blocking if a site breaks.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(checked = blockingEnabled, onCheckedChange = onBlockingChange)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Third-party cookies", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                                    Text("Off by default; exact-site overrides are available in Site settings.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(checked = thirdPartyCookiesEnabled, onCheckedChange = onThirdPartyCookiesChange)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Reduce fingerprinting", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                                    Text("Best-effort normalization of hardware and WebGL signals. WebView can still expose other signals.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(checked = fingerprintingProtectionEnabled, onCheckedChange = onFingerprintingProtectionChange)
                            }
                            InfoCard("Site permissions", "Location, camera, and microphone require an explicit site allow-list and Android permission. JavaScript and third-party cookies are configurable per site.")
                            InfoCard("Local-first", "History and preferences stay on this device. Xeno does not add telemetry.")
                            InfoCard("AI page summaries", "Page text is sent to your AI provider only after you confirm.")
                            Button(onClick = onClearBrowsingData, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Clear history, cookies and cache")
                            }
                        }
                        BrowserPanel.SPACES -> {
                            var newSpaceName by remember(panel) { mutableStateOf("") }
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newSpaceName,
                                    onValueChange = { newSpaceName = it.take(32) },
                                    label = { Text("New space name") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        onCreateSpace(newSpaceName)
                                        newSpaceName = ""
                                    },
                                    enabled = newSpaceName.isNotBlank()
                                ) { Text("Add") }
                            }
                            if (spaces.isEmpty()) EmptyState("No spaces available")
                            else LazyColumn(
                                Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(spaces, key = { it.id }) { space ->
                                    SpaceManagementRow(
                                        space = space,
                                        active = space.id == activeSpaceId,
                                        canDelete = spaces.size > 1,
                                        onSelect = { onSelectSpace(space) },
                                        onRename = { name -> onRenameSpace(space, name) },
                                        onDelete = { onDeleteSpace(space) }
                                    )
                                }
                            }
                        }
                        BrowserPanel.SITE_SETTINGS -> Column(
                            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InfoCard(
                                "Permissions for " + currentSiteHost,
                                "Sensitive permissions are denied by default. Enabling one also requires Android permission. This preference applies only to this exact host."
                            )
                            SitePermissionToggle(
                                title = "JavaScript",
                                subtitle = "Reloads the page when changed",
                                checked = sitePermissions.javaScriptEnabled,
                                onChange = { onSitePermissionChange(SiteCapability.JAVASCRIPT, it) }
                            )
                            SitePermissionToggle(
                                title = "Third-party cookies",
                                subtitle = "Override the global cookie default for this site",
                                checked = sitePermissions.thirdPartyCookiesEnabled,
                                onChange = { onSitePermissionChange(SiteCapability.THIRD_PARTY_COOKIES, it) }
                            )
                            SitePermissionToggle(
                                title = "Location",
                                subtitle = "Requires Android location permission",
                                checked = sitePermissions.allowLocation,
                                onChange = { onSitePermissionChange(SiteCapability.LOCATION, it) }
                            )
                            SitePermissionToggle(
                                title = "Camera",
                                subtitle = "Requires Android camera permission",
                                checked = sitePermissions.allowCamera,
                                onChange = { onSitePermissionChange(SiteCapability.CAMERA, it) }
                            )
                            SitePermissionToggle(
                                title = "Microphone",
                                subtitle = "Requires Android microphone permission",
                                checked = sitePermissions.allowMicrophone,
                                onChange = { onSitePermissionChange(SiteCapability.MICROPHONE, it) }
                            )
                        }
                        BrowserPanel.TRANSLATE -> Column(
                            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("Translate the readable article text into:", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                            listOf("English", "Hindi", "Bengali", "Spanish", "French", "Japanese", "Chinese", "German", "Portuguese").chunked(3).forEach { rowLanguages ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    rowLanguages.forEach { language ->
                                        FilterChip(
                                            selected = translationLanguage == language,
                                            onClick = { onTranslationLanguageChange(language) },
                                            label = { Text(language) }
                                        )
                                    }
                                }
                            }
                            InfoCard("Privacy", "Xeno extracts article text locally first. It asks before sending that text to your AI provider.")
                            Button(onClick = onTranslate, modifier = Modifier.fillMaxWidth()) { Text("Translate page") }
                        }
                        BrowserPanel.SETTINGS -> Column(
                            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            InfoCard("General", "Search, browser behaviour and local data controls.")
                            SitePermissionToggle(
                                title = "Auto-open keyboard",
                                subtitle = "Open search ready to type when Xeno starts",
                                checked = autoOpenKeyboard,
                                onChange = onAutoOpenKeyboardChange
                            )
                            Text(
                                "SEARCH ENGINE",
                                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                            listOf(
                                "brave" to "Brave Search",
                                "duckduckgo" to "DuckDuckGo",
                                "google" to "Google",
                                "bing" to "Bing",
                                "startpage" to "Startpage"
                            ).chunked(3).forEach { engineRow ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    engineRow.forEach { (id, label) ->
                                        FilterChip(
                                            selected = searchEngineId == id,
                                            onClick = { onSearchEngineChange(id) },
                                            label = { Text(label, maxLines = 1) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            Text(
                                "BROWSER",
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                            MenuAction("Default browser", "Choose Xeno in Android system settings", Icons.Rounded.Settings, onOpenDefaultAppsSettings)
                            MenuAction("Privacy and blockers", "Ads, trackers, cookies and site permissions", Icons.Rounded.Shield) { onOpenPanel(BrowserPanel.PRIVACY) }
                            MenuAction("Downloads", "${downloads.size} saved files", Icons.Rounded.Download) {
                                onOpenPanel(BrowserPanel.DOWNLOADS)
                                onRefreshDownloads()
                            }
                            MenuAction("History", "${history.size} recently visited pages", Icons.Rounded.History) { onOpenPanel(BrowserPanel.HISTORY) }
                            MenuAction("Archive", "${archiveEntries.size} archived pages", Icons.Rounded.History) { onOpenPanel(BrowserPanel.ARCHIVE) }
                            if (!isPrivateMode) {
                                MenuAction("Bookmarks", "${bookmarks.size} saved pages", Icons.Rounded.Bookmarks) { onOpenPanel(BrowserPanel.BOOKMARKS) }
                                MenuAction("Pinned pages", "${pinnedPages.size} pinned pages", Icons.Rounded.Bookmarks) { onOpenPanel(BrowserPanel.PINNED) }
                            }
                            MenuAction("Spaces", "${spaces.size} browsing spaces", Icons.Rounded.Workspaces) { onOpenPanel(BrowserPanel.SPACES) }
                            MenuAction("AI and Browse for Me", "${provider.displayName} · ${provider.model}", Icons.Rounded.AutoAwesome) { onOpenPanel(BrowserPanel.AI) }
                            MenuAction("Clear browsing data", "Clear history, cookies and cache on this device", Icons.Rounded.DeleteOutline, onClearBrowsingData)
                        }
                        BrowserPanel.AI -> {
                            var selectedProviderId by remember(provider.id, panel) { mutableStateOf(provider.id) }
                            var baseUrl by remember(provider.baseUrl, panel) { mutableStateOf(provider.baseUrl) }
                            var model by remember(provider.model, panel) { mutableStateOf(provider.model) }
                            var apiKey by remember(provider.apiKey, panel) { mutableStateOf(provider.apiKey) }
                            val selectedPreset = ProviderPresets.byId(selectedProviderId)
                            val catalogueForSelection = remember(
                                availableModels, availableModelsProviderId, selectedProviderId, apiKey
                            ) {
                                val source = availableModels
                                    .takeIf { availableModelsProviderId == selectedProviderId }
                                    .orEmpty()
                                if (selectedProviderId == "kilo" && apiKey.isBlank()) {
                                    source.filter { it.isFree }
                                } else source
                            }
                            var modelSearch by remember(selectedProviderId, panel) { mutableStateOf("") }
                            val filteredModels = remember(catalogueForSelection, modelSearch) {
                                catalogueForSelection.filter { option ->
                                    listOfNotNull(
                                        option.name,
                                        option.id,
                                        if (option.isFree) "free" else null
                                    ).joinToString(" ").contains(modelSearch.trim(), ignoreCase = true)
                                }
                            }
                            Column(
                                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "AI PROVIDER",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 1.dp)
                                )
                                InfoCard(
                                    selectedPreset.displayName,
                                    when {
                                        isPrivateMode -> "AI provider settings are read-only in private mode."
                                        selectedProviderId == "kilo" -> "Fetch the live Kilo catalogue below. Free models and Kilo Auto Free work without a key; anonymous requests are rate-limited and may reach upstream model providers, so avoid confidential data."
                                        selectedProviderId == "local" -> "Use a local OpenAI-compatible server. On a phone, localhost means the phone itself; enter a reachable LAN address when needed."
                                        else -> "This provider generally requires your own API key. Xeno sends only queries or page text you explicitly approve."
                                    }
                                )
                                ProviderPresets.all.chunked(2).forEach { presetRow ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        presetRow.forEach { preset ->
                                            FilterChip(
                                                selected = selectedProviderId == preset.id,
                                                onClick = {
                                                    val selectedConfig = onProviderSelected(preset.id)
                                                    selectedProviderId = selectedConfig.id
                                                    baseUrl = selectedConfig.baseUrl
                                                    model = selectedConfig.model
                                                    apiKey = selectedConfig.apiKey
                                                    modelSearch = ""
                                                },
                                                enabled = !isPrivateMode,
                                                label = {
                                                    Text(
                                                        when (preset.id) {
                                                            "openrouter" -> "OpenRouter"
                                                            "local" -> "Local"
                                                            else -> preset.displayName
                                                        },
                                                        maxLines = 1
                                                    )
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    baseUrl, { baseUrl = it }, label = { Text("OpenAI-compatible base URL") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                    readOnly = isPrivateMode,
                                    singleLine = true, modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    model, { model = it }, label = { Text("Model ID") },
                                    readOnly = isPrivateMode,
                                    singleLine = true, modifier = Modifier.fillMaxWidth()
                                )
                                var modelMenuExpanded by remember { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            onFetchModels(provider.copy(
                                                id = selectedProviderId,
                                                displayName = selectedPreset.displayName,
                                                baseUrl = baseUrl.trim().trimEnd('/'),
                                                model = model.trim(),
                                                apiKey = apiKey.trim()
                                            ))
                                        },
                                        enabled = !isPrivateMode && !modelsLoading && baseUrl.isNotBlank(),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (modelsLoading) "Fetching models…" else "Fetch models")
                                    }
                                    Box {
                                        OutlinedButton(
                                            onClick = { modelMenuExpanded = true },
                                            enabled = filteredModels.isNotEmpty()
                                        ) { Text("Choose") }
                                        DropdownMenu(
                                            expanded = modelMenuExpanded,
                                            onDismissRequest = { modelMenuExpanded = false }
                                        ) {
                                            filteredModels.take(100).forEach { option ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(option.name ?: option.id)
                                                            if (option.name != null || option.isFree) {
                                                                Text(
                                                                     if (option.isFree) "FREE · ${option.id}" else option.id,
                                                                     style = MaterialTheme.typography.labelSmall,
                                                                     color = if (option.isFree) Color(0xFFB6F36A) else TextSecondary
                                                                 )
                                                            }
                                                        }
                                                    },
                                                    onClick = {
                                                        model = option.id
                                                        modelMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                if (catalogueForSelection.isNotEmpty()) {
                                    OutlinedTextField(
                                        value = modelSearch,
                                        onValueChange = { modelSearch = it },
                                        label = {
                                            Text(
                                                if (selectedProviderId == "kilo" && apiKey.isBlank())
                                                    "Search free Kilo models"
                                                else "Search fetched models"
                                            )
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        "${filteredModels.size} matching models",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                OutlinedTextField(
                                    apiKey, { apiKey = it }, label = { Text("API key (if required)") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    readOnly = isPrivateMode,
                                    singleLine = true, modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isPrivateMode && baseUrl.isNotBlank() && model.isNotBlank(),
                                    onClick = {
                                        val url = baseUrl.trim().trimEnd('/')
                                        val validScheme = url.startsWith("https://") ||
                                            (selectedProviderId == "local" && url.startsWith("http://"))
                                        if (validScheme) {
                                            onSaveProvider(
                                                provider.copy(
                                                    id = selectedProviderId,
                                                    displayName = selectedPreset.displayName,
                                                    baseUrl = url,
                                                    model = model.trim(),
                                                    apiKey = apiKey.trim()
                                                )
                                            )
                                        }
                                    }
                                ) { Text("Save AI settings") }
                                settingsError?.let { message ->
                                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        null -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun SitePermissionToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
@Composable
private fun SpaceManagementRow(
    space: Space,
    active: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var editing by remember(space.id, space.name) { mutableStateOf(false) }
    var draft by remember(space.id, space.name) { mutableStateOf(space.name) }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (active) space.accent.copy(alpha = 0.11f) else Color(0xFF25232D),
        border = BorderStroke(1.dp, if (active) space.accent.copy(alpha = 0.45f) else StrokeSubtle)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(9.dp).clip(androidx.compose.foundation.shape.CircleShape)
                    .background(space.accent)
            )
            if (editing) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(32) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                IconButton(
                    onClick = {
                        if (draft.isNotBlank()) onRename(draft.trim())
                        editing = false
                    }
                ) {
                    Icon(Icons.Rounded.Check, "Save space name", tint = space.accent)
                }
                IconButton(onClick = { draft = space.name; editing = false }) {
                    Icon(Icons.Rounded.Close, "Cancel rename", tint = TextSecondary)
                }
            } else {
                Column(
                    Modifier.weight(1f).clickable(onClick = onSelect).padding(start = 10.dp, top = 5.dp, bottom = 5.dp)
                ) {
                    Text(space.name, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(if (active) "Current space" else "Switch to space", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { draft = space.name; editing = true }) {
                    Icon(Icons.Rounded.Edit, "Rename space", tint = TextSecondary)
                }
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.DeleteOutline, "Delete space", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuAction(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(17.dp),
        color = Color(0xFF25232D),
        border = BorderStroke(1.dp, StrokeSubtle)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF302D39),
        border = BorderStroke(1.dp, StrokeSubtle)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(body, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Text(message, color = TextSecondary)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "${bytes.coerceAtLeast(0)} B"
    bytes < 1024L * 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
}
