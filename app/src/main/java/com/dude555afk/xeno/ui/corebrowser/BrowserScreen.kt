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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
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
    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            WebViewHost(
                pool = pool,
                tabId = tabId,
                modifier = Modifier.fillMaxSize()
            )

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
