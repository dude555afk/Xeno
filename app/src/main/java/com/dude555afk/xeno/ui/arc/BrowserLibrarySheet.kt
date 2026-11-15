package com.dude555afk.xeno.ui.arc

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dude555afk.xeno.core.model.ArchivedTab
import com.dude555afk.xeno.core.model.PinnedPage

@Composable
fun BrowserLibrarySheet(
    visible: Boolean,
    archivedTabs: List<ArchivedTab>,
    pinnedTabs: List<PinnedPage>,
    initialSection: String = "archive",
    onRestore: (ArchivedTab) -> Unit,
    onDeleteArchive: (ArchivedTab) -> Unit,
    onOpenPinned: (PinnedPage) -> Unit,
    onUnpin: (PinnedPage) -> Unit,
    onDismiss: () -> Unit
) {
    var section by remember(initialSection, visible) { mutableStateOf(initialSection) }
    BackHandler(enabled = visible, onBack = onDismiss)
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF1B1C20)) {
            Column(Modifier.fillMaxSize().padding(top = 36.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Library", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close library", tint = Color.White)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LibrarySegment("Archive", section == "archive", Modifier.weight(1f)) { section = "archive" }
                    LibrarySegment("Pinned", section == "pinned", Modifier.weight(1f)) { section = "pinned" }
                }
                if (section == "archive") {
                    if (archivedTabs.isEmpty()) EmptyLibrary("No archived tabs yet")
                    else LazyColumn(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(archivedTabs, key = { it.id }) { tab ->
                            LibraryRow(
                                icon = if (tab.type == "ai") Icons.Rounded.Archive else Icons.Rounded.Language,
                                title = tab.title.ifBlank { tab.query.ifBlank { tab.url } },
                                subtitle = tab.url,
                                actionIcon = Icons.Rounded.Restore,
                                actionDescription = "Restore tab",
                                onAction = { onRestore(tab) },
                                onClick = { onRestore(tab) }
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                IconButton(onClick = { onDeleteArchive(tab) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete archived tab", tint = Color(0xFF9B9DA5))
                                }
                            }
                        }
                    }
                } else {
                    if (pinnedTabs.isEmpty()) EmptyLibrary("No pinned pages yet")
                    else LazyColumn(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(pinnedTabs, key = { it.url }) { page ->
                            LibraryRow(
                                icon = Icons.Rounded.PushPin,
                                title = page.title.ifBlank { page.url },
                                subtitle = page.url,
                                actionIcon = Icons.Rounded.Close,
                                actionDescription = "Unpin page",
                                onAction = { onUnpin(page) },
                                onClick = { onOpenPinned(page) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySegment(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = CircleShape,
        color = if (selected) Color(0xFF4E5BCD) else Color(0xFF303137)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyLibrary(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = Color(0xFF9B9DA5), fontSize = 15.sp)
    }
}

@Composable
private fun LibraryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    actionDescription: String,
    onAction: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(72.dp).clickable(onClick = onClick).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(Modifier.size(38.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFF303137)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color(0xFFB9BAC3), modifier = Modifier.size(19.dp))
            }
        }
        Column(Modifier.padding(start = 13.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Color(0xFF9B9DA5), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onAction) {
            Icon(actionIcon, contentDescription = actionDescription, tint = Color(0xFFB9BAC3))
        }
    }
}
