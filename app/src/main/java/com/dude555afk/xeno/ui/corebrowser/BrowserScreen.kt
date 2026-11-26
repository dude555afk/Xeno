package com.dude555afk.xeno.ui.corebrowser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dude555afk.xeno.core.browser.PageSnapshot
import com.dude555afk.xeno.core.browser.WebViewHost
import com.dude555afk.xeno.core.browser.WebViewPool

private val BrowserSheet = Color(0xFF2D2E32)
private val BrowserPill = Color(0xFF4F5052)
private val BrowserTiles = Color(0xFF3D3E42)

@Composable
fun BrowserScreen(
    pool: WebViewPool,
    tabId: Long,
    snapshot: PageSnapshot,
    s: Float,
    onOpenTabs: () -> Unit,
    onNewSearch: () -> Unit,
    isPinned: Boolean,
    onTogglePin: (Boolean) -> Unit
) {
    var actionsVisible by remember { mutableStateOf(false) }
    var findVisible by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            WebViewHost(pool = pool, tabId = tabId, modifier = Modifier.fillMaxSize())
            if (snapshot.loading) {
                LinearProgressIndicator(
                    progress = { snapshot.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(2.dp),
                    color = Color(0xFF5988FC),
                    trackColor = Color.Transparent
                )
            }
            if (findVisible) {
                Row(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .background(BrowserSheet, RoundedCornerShape(18.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = findQuery,
                        onValueChange = {
                            findQuery = it
                            if (it.isNotBlank()) pool.find(tabId, it) else pool.clearFind(tabId)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        decorationBox = { inner ->
                            Box {
                                if (findQuery.isEmpty()) Text("Find in page", color = Color.LightGray)
                                inner()
                            }
                        }
                    )
                    IconButton(onClick = { pool.findNext(tabId, false) }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Previous match", tint = Color.White)
                    }
                    IconButton(onClick = { pool.findNext(tabId, true) }) {
                        Icon(Icons.Rounded.ArrowForward, contentDescription = "Next match", tint = Color.White)
                    }
                    IconButton(onClick = {
                        pool.clearFind(tabId)
                        findQuery = ""
                        findVisible = false
                    }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close find", tint = Color.White)
                    }
                }
            }
        }
        CoreBottomDock(
            s = s,
            browserMode = true,
            onOpenTabs = onOpenTabs,
            onNewSearch = onNewSearch,
            onRightAction = { actionsVisible = true }
        )
    }

    if (actionsVisible) {
        PageActionSheet(
            pool = pool,
            tabId = tabId,
            snapshot = snapshot,
            s = s,
            isPinned = isPinned,
            onTogglePin = onTogglePin,
            onFind = { actionsVisible = false; findVisible = true },
            onDismiss = { actionsVisible = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageActionSheet(
    pool: WebViewPool,
    tabId: Long,
    snapshot: PageSnapshot,
    s: Float,
    isPinned: Boolean,
    onTogglePin: (Boolean) -> Unit,
    onFind: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var zoom by remember(tabId) { mutableIntStateOf(pool.zoomLevel(tabId)) }
    val host = runCatching { android.net.Uri.parse(snapshot.url).host }.getOrNull()
        ?.removePrefix("www.") ?: "New tab"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BrowserSheet,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = (44f * s).dp, topEnd = (44f * s).dp),
        dragHandle = {}
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().height((660f * s).dp)
                .padding(horizontal = (18f * s).dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = (18f * s).dp)
                    .height((58f * s).dp)
                    .background(BrowserPill, RoundedCornerShape((29f * s).dp))
                    .padding(horizontal = (7f * s).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { pool.goBack(tabId) }, enabled = snapshot.canGoBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                IconButton(onClick = { pool.goForward(tabId) }, enabled = snapshot.canGoForward) {
                    Icon(Icons.Rounded.ArrowForward, contentDescription = "Forward", tint = Color.White)
                }
                Text(
                    host,
                    color = Color.White,
                    fontSize = (15f * s).sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Page URL", snapshot.url))
                }) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy URL", tint = Color.White)
                }
                IconButton(onClick = { pool.reload(tabId) }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Reload", tint = Color.White)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = (22f * s).dp)
                    .height((97f * s).dp)
                    .background(BrowserTiles, RoundedCornerShape((15f * s).dp))
                    .padding((2f * s).dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ActionTile(
                    modifier = Modifier.weight(1f),
                    label = if (isPinned) "Pinned" else "Pin",
                    selected = isPinned,
                    icon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
                    onClick = { onTogglePin(!isPinned) }
                )
                ActionTile(
                    modifier = Modifier.weight(1f),
                    label = "Find",
                    icon = { Icon(Icons.Rounded.FindInPage, contentDescription = null) },
                    onClick = onFind
                )
                ActionTile(
                    modifier = Modifier.weight(1f),
                    label = "Share",
                    icon = { Icon(Icons.Rounded.IosShare, contentDescription = null) },
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, snapshot.url)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share page"))
                    }
                )
                ActionTile(
                    modifier = Modifier.weight(1f),
                    label = if (snapshot.desktopSite) "Desktop on" else "Desktop",
                    selected = snapshot.desktopSite,
                    icon = { Icon(Icons.Rounded.DesktopWindows, contentDescription = null) },
                    onClick = { pool.setDesktopSite(tabId, !snapshot.desktopSite) }
                )
            }
            Spacer(Modifier.height((34f * s).dp))
            Row(
                modifier = Modifier.fillMaxWidth().height((56f * s).dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = { zoom = (zoom - 10).coerceAtLeast(50); pool.zoom(tabId, zoom) }) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Zoom out", tint = Color.White)
                }
                Text(
                    zoom.toString() + "%",
                    color = Color.White,
                    fontSize = (16f * s).sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = (22f * s).dp)
                )
                IconButton(onClick = { zoom = (zoom + 10).coerceAtMost(200); pool.zoom(tabId, zoom) }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Zoom in", tint = Color.White)
                }
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height((18f * s).dp))
        }
    }
}

@Composable
private fun ActionTile(
    modifier: Modifier,
    label: String,
    selected: Boolean = false,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize()
            .background(if (selected) Color(0xFF555B68) else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides Color.White
        ) { icon() }
        Text(label, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
    }
}
