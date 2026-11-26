package com.dude555afk.xeno.ui.corebrowser

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import android.widget.Toast
import com.dude555afk.xeno.core.browser.CoreTab
import com.dude555afk.xeno.core.browser.PageSnapshot
import com.dude555afk.xeno.core.browser.WebViewPool

private enum class CoreScreen { HOME, BROWSER, SEARCH, TABS }
private data class ClosedTab(val tab: CoreTab, val index: Int)

@Composable
fun CoreBrowserRoot(initialUrl: String? = null) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val s = configuration.screenWidthDp / 462f
    val pool = remember(context) { WebViewPool(context) }
    pool.updateActivityContext(context)
    val tabs = remember {
        mutableStateListOf(CoreTab(id = 1L))
    }
    var nextId by remember { mutableLongStateOf(2L) }
    var activeTabId by remember { mutableLongStateOf(1L) }
    var screen by remember {
        mutableStateOf(if (initialUrl.isNullOrBlank()) CoreScreen.HOME else CoreScreen.BROWSER)
    }
    var lastClosed by remember { mutableStateOf<ClosedTab?>(null) }
    val snapshots by pool.snapshots.collectAsState()
    val activeTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()
    val snapshot = snapshots[activeTabId] ?: PageSnapshot(
        url = activeTab.url,
        title = activeTab.title
    )

    LaunchedEffect(activeTabId, snapshot.url, snapshot.title) {
        val index = tabs.indexOfFirst { it.id == activeTabId }
        if (index >= 0 && snapshot.url.isNotBlank()) {
            val current = tabs[index]
            val nextTitle = snapshot.title.ifBlank { current.title }
            if (current.url != snapshot.url || current.title != nextTitle) {
                tabs[index] = current.copy(url = snapshot.url, title = nextTitle)
            }
        }
    }

    DisposableEffect(pool) {
        onDispose { pool.destroyAll() }
    }

    fun newTab(openSearch: Boolean) {
        val tab = CoreTab(id = nextId++)
        tabs.add(tab)
        activeTabId = tab.id
        screen = if (openSearch) CoreScreen.SEARCH else CoreScreen.HOME
    }

    fun closeTab(tab: CoreTab) {
        val index = tabs.indexOfFirst { it.id == tab.id }
        if (index < 0) return
        lastClosed = ClosedTab(tab, index)
        pool.destroy(tab.id)
        if (tabs.size == 1) {
            tabs.clear()
            val replacement = CoreTab(id = nextId++)
            tabs.add(replacement)
            activeTabId = replacement.id
            screen = CoreScreen.TABS
        } else {
            tabs.removeAt(index)
            if (activeTabId == tab.id) {
                val replacement = tabs[index.coerceAtMost(tabs.lastIndex)]
                activeTabId = replacement.id
            }
            screen = CoreScreen.TABS
        }
    }

    fun undoClose() {
        val closed = lastClosed ?: return
        if (tabs.none { it.id == closed.tab.id }) {
            tabs.add(closed.index.coerceIn(0, tabs.size), closed.tab)
            if (closed.tab.url.isNotBlank()) pool.navigate(closed.tab.id, closed.tab.url)
            activeTabId = closed.tab.id
            screen = CoreScreen.TABS
        }
        lastClosed = null
    }

    fun submitSearch(target: String) {
        if (target.isBlank()) return
        val currentIndex = tabs.indexOfFirst { it.id == activeTabId }
        if (currentIndex >= 0) {
            val uri = android.net.Uri.parse(target)
            tabs[currentIndex] = tabs[currentIndex].copy(
                url = target,
                title = uri.host?.removePrefix("www.") ?: "Loading…"
            )
        }
        pool.navigate(activeTabId, target)
        screen = CoreScreen.BROWSER
    }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            submitSearch(resolveSearchTarget(initialUrl))
        }
    }

    BackHandler(enabled = screen != CoreScreen.HOME || snapshot.canGoBack) {
        when (screen) {
            CoreScreen.SEARCH -> screen = if (activeTab.url.isBlank()) CoreScreen.HOME else CoreScreen.BROWSER
            CoreScreen.TABS -> screen = if (activeTab.url.isBlank()) CoreScreen.HOME else CoreScreen.BROWSER
            CoreScreen.BROWSER -> if (snapshot.canGoBack) pool.goBack(activeTabId) else screen = CoreScreen.HOME
            CoreScreen.HOME -> Unit
        }
    }

    when (screen) {
        CoreScreen.HOME -> HomeScreen(
            s = s,
            onOpenTabs = { screen = CoreScreen.TABS },
            onNewSearch = { screen = CoreScreen.SEARCH },
            onSettings = {
                Toast.makeText(context, "Core browser settings are not part of Layer 1.", Toast.LENGTH_SHORT).show()
            }
        )
        CoreScreen.BROWSER -> BrowserScreen(
            pool = pool,
            tabId = activeTabId,
            snapshot = snapshot,
            s = s,
            onOpenTabs = { screen = CoreScreen.TABS },
            onNewSearch = { screen = CoreScreen.SEARCH },
            isPinned = activeTab.pinned,
            onTogglePin = { pinned ->
                val index = tabs.indexOfFirst { it.id == activeTabId }
                if (index >= 0) tabs[index] = tabs[index].copy(pinned = pinned)
            }
        )
        CoreScreen.SEARCH -> {
            if (activeTab.url.isBlank()) {
                HomeScreen(
                    s = s,
                    onOpenTabs = { screen = CoreScreen.TABS },
                    onNewSearch = { screen = CoreScreen.SEARCH },
                    onSettings = {
                        Toast.makeText(context, "Core browser settings are not part of Layer 1.", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                BrowserScreen(
                    pool = pool,
                    tabId = activeTabId,
                    snapshot = snapshot,
                    s = s,
                    onOpenTabs = { screen = CoreScreen.TABS },
                    onNewSearch = { screen = CoreScreen.SEARCH },
                    isPinned = activeTab.pinned,
                    onTogglePin = { pinned ->
                        val index = tabs.indexOfFirst { it.id == activeTabId }
                        if (index >= 0) tabs[index] = tabs[index].copy(pinned = pinned)
                    }
                )
            }
            SearchBottomSheet(
                s = s,
                onDismiss = {
                    screen = if (activeTab.url.isBlank()) CoreScreen.HOME else CoreScreen.BROWSER
                },
                onSubmit = ::submitSearch
            )
        }
        CoreScreen.TABS -> TabSwitcherScreen(
            tabs = tabs.toList(),
            activeTabId = activeTabId,
            s = s,
            onSelect = { selected ->
                if (selected.url.isNotBlank() && !pool.contains(selected.id)) pool.navigate(selected.id, selected.url)
                activeTabId = selected.id
                screen = if (selected.url.isBlank()) CoreScreen.HOME else CoreScreen.BROWSER
            },
            onClose = ::closeTab,
            onUndoClose = ::undoClose,
            onNewSearch = { newTab(openSearch = true) },
            onDismiss = {
                screen = if (activeTab.url.isBlank()) CoreScreen.HOME else CoreScreen.BROWSER
            }
        )
    }
}
