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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Close
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
import com.dude555afk.xeno.core.ai.ProviderConfig
import com.dude555afk.xeno.core.storage.HistoryEntity
import com.dude555afk.xeno.ui.theme.StrokeSubtle
import com.dude555afk.xeno.ui.theme.TextPrimary
import com.dude555afk.xeno.ui.theme.TextSecondary
import java.util.Locale

enum class BrowserPanel { MENU, HISTORY, DOWNLOADS, PRIVACY, SETTINGS }

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
    downloads: List<BrowserDownload>,
    blockingEnabled: Boolean,
    provider: ProviderConfig,
    onDismiss: () -> Unit,
    onOpenPanel: (BrowserPanel) -> Unit,
    onHistoryClick: (HistoryEntity) -> Unit,
    onClearHistory: () -> Unit,
    onClearBrowsingData: () -> Unit,
    onBlockingChange: (Boolean) -> Unit,
    onSaveProvider: (ProviderConfig) -> Unit,
    onRefreshDownloads: () -> Unit,
    onOpenDownload: (BrowserDownload) -> Unit,
    onRemoveDownload: (BrowserDownload) -> Unit
) {
    AnimatedVisibility(
        visible = panel != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.64f)).clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(
                    if (panel == BrowserPanel.MENU) 0.54f else 0.84f
                ).clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { },
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                color = Color(0xFF090909),
                border = BorderStroke(1.dp, StrokeSubtle),
                shadowElevation = 20.dp
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (panel != null && panel != BrowserPanel.MENU) {
                            IconButton(onClick = { onOpenPanel(BrowserPanel.MENU) }) {
                                Icon(Icons.Rounded.ArrowBack, "Back", tint = TextSecondary)
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                when (panel) {
                                    BrowserPanel.MENU -> "Xeno"
                                    BrowserPanel.HISTORY -> "History"
                                    BrowserPanel.DOWNLOADS -> "Downloads"
                                    BrowserPanel.PRIVACY -> "Privacy"
                                    BrowserPanel.SETTINGS -> "Settings"
                                    null -> "Xeno"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                when (panel) {
                                    BrowserPanel.MENU -> "Your browser, your rules."
                                    BrowserPanel.HISTORY -> "Stored on this device"
                                    BrowserPanel.DOWNLOADS -> "Files saved by Xeno"
                                    BrowserPanel.PRIVACY -> "Protection and local data"
                                    BrowserPanel.SETTINGS -> "AI provider and preferences"
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
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MenuAction("History", "Recently visited pages", Icons.Rounded.History) { onOpenPanel(BrowserPanel.HISTORY) }
                            MenuAction("Downloads", "Files and download status", Icons.Rounded.Download) {
                                onOpenPanel(BrowserPanel.DOWNLOADS)
                                onRefreshDownloads()
                            }
                            MenuAction("Privacy & blockers", "Tracking protection and clear data", Icons.Rounded.Shield) { onOpenPanel(BrowserPanel.PRIVACY) }
                            MenuAction("Settings", "AI provider and browser preferences", Icons.Rounded.Settings) { onOpenPanel(BrowserPanel.SETTINGS) }
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
                            if (downloads.isEmpty()) EmptyState("No downloads yet")
                            else LazyColumn(
                                Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(downloads, key = { it.id }) { item ->
                                    Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
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
                            InfoCard("Local-first", "History and preferences stay on this device. Xeno does not add telemetry.")
                            InfoCard("AI page summaries", "Page text is sent to your AI provider only after you confirm.")
                            Button(onClick = onClearBrowsingData, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Clear history, cookies and cache")
                            }
                        }
                        BrowserPanel.SETTINGS -> {
                            var baseUrl by remember(provider.baseUrl, panel) { mutableStateOf(provider.baseUrl) }
                            var model by remember(provider.model, panel) { mutableStateOf(provider.model) }
                            var apiKey by remember(provider.apiKey, panel) { mutableStateOf(provider.apiKey) }
                            Column(
                                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                InfoCard("Kilo AI Gateway", "Use a key for paid models or a supported model ending in :free for keyless access.")
                                OutlinedTextField(
                                    baseUrl, { baseUrl = it }, label = { Text("OpenAI-compatible base URL") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                    singleLine = true, modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    model, { model = it }, label = { Text("Model ID") },
                                    singleLine = true, modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    apiKey, { apiKey = it }, label = { Text("API key (optional for free models)") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true, modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = baseUrl.isNotBlank() && model.isNotBlank(),
                                    onClick = {
                                        val url = baseUrl.trim().trimEnd('/')
                                        if (url.startsWith("https://") || (provider.id == "local" && url.startsWith("http://"))) {
                                            onSaveProvider(provider.copy(baseUrl = url, model = model.trim(), apiKey = apiKey.trim()))
                                        }
                                    }
                                ) { Text("Save AI settings") }
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
private fun MenuAction(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.045f),
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
        color = Color.White.copy(alpha = 0.04f),
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
