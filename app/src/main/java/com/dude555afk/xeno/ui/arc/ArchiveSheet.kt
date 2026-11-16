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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dude555afk.xeno.core.model.ArchivedTab

@Composable
fun ArchiveSheet(
    visible: Boolean,
    tabs: List<ArchivedTab>,
    onRestore: (ArchivedTab) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(enabled = visible, onBack = onDismiss)
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickable(onClick = onDismiss)) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxSize(0.78f)
                    .clickable { },
                color = Color(0xFF202126),
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
            ) {
                Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    Row(
                        Modifier.fillMaxWidth().height(64.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Archive, contentDescription = null, tint = Color.White)
                        Text("Archive", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f).padding(start = 13.dp))
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close archive", tint = Color.White)
                        }
                    }
                    if (tabs.isEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text("Archived tabs will appear here", color = Color(0xFFAAAAB0), fontSize = 14.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(tabs, key = { it.id }) { tab ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(tab.title.ifBlank { tab.url.ifBlank { "New tab" } },
                                            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(tab.url, color = Color(0xFF9B9CA3), fontSize = 11.sp,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(onClick = { onRestore(tab) }, modifier = Modifier.size(42.dp)) {
                                        Icon(Icons.Rounded.Restore, contentDescription = "Restore tab", tint = Color(0xFFB8BFFF))
                                    }
                                    IconButton(onClick = { onDelete(tab.id) }, modifier = Modifier.size(42.dp)) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete archived tab", tint = Color(0xFFB0B0B7))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
