package com.dude555afk.xeno.core.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CoreTab(
    val id: Long,
    val url: String = "",
    val title: String = "New tab",
    val pinned: Boolean = false
)

data class PageSnapshot(
    val url: String = "",
    val title: String = "",
    val progress: Float = 0f,
    val loading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val favicon: Bitmap? = null,
    val desktopSite: Boolean = false
)

/** Owns WebViews independently from Compose chrome and overlay state. */
@Stable
class WebViewPool(context: Context) {
    private var activityContext = context
    private val webContexts = HashMap<Long, MutableContextWrapper>()
    private val views = LinkedHashMap<Long, WebView>()
    private val zoomLevels = HashMap<Long, Int>()
    private val desktopSites = HashMap<Long, Boolean>()
    private val _snapshots = MutableStateFlow<Map<Long, PageSnapshot>>(emptyMap())
    val snapshots: StateFlow<Map<Long, PageSnapshot>> = _snapshots.asStateFlow()

    @SuppressLint("SetJavaScriptEnabled")
    fun obtain(tabId: Long): WebView {
        views[tabId]?.let { return it }
        val viewContext = MutableContextWrapper(activityContext)
        webContexts[tabId] = viewContext
        val view = WebView(viewContext).apply {
            settings.textZoom = zoomLevels[tabId] ?: 100
            if (desktopSites[tabId] == true) settings.userAgentString = DESKTOP_USER_AGENT
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.mediaPlaybackRequiresUserGesture = true
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = WebView.OVER_SCROLL_NEVER
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    publish(tabId, view) {
                        copy(url = url, title = view.title.orEmpty(), progress = 0f, loading = true, favicon = favicon)
                    }
                }

                override fun onPageFinished(view: WebView, url: String) {
                    publish(tabId, view) {
                        copy(url = url, title = view.title.orEmpty(), progress = 1f, loading = false)
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    publish(tabId, view) {
                        copy(
                            url = view.url.orEmpty(),
                            title = view.title.orEmpty(),
                            progress = newProgress / 100f,
                            loading = newProgress < 100
                        )
                    }
                }

                override fun onReceivedTitle(view: WebView, title: String) {
                    publish(tabId, view) { copy(title = title) }
                }

                override fun onReceivedIcon(view: WebView, icon: Bitmap?) {
                    publish(tabId, view) { copy(favicon = icon) }
                }
            }
        }
        views[tabId] = view
        publish(tabId, view) { copy(url = view.url.orEmpty(), title = view.title.orEmpty()) }
        return view
    }

    private fun publish(tabId: Long, view: WebView, transform: PageSnapshot.() -> PageSnapshot) {
        _snapshots.update { current ->
            val old = current[tabId] ?: PageSnapshot()
            current + (tabId to transform(old).copy(
                canGoBack = view.canGoBack(),
                canGoForward = view.canGoForward(),
                desktopSite = desktopSites[tabId] == true
            ))
        }
    }

    fun updateActivityContext(context: Context) {
        activityContext = context
        webContexts.values.forEach { it.baseContext = context }
    }

    fun contains(tabId: Long): Boolean = views.containsKey(tabId)

    fun navigate(tabId: Long, url: String) {
        obtain(tabId).loadUrl(url)
    }

    fun goBack(tabId: Long) { views[tabId]?.takeIf { it.canGoBack() }?.goBack() }
    fun goForward(tabId: Long) { views[tabId]?.takeIf { it.canGoForward() }?.goForward() }
    fun reload(tabId: Long) { views[tabId]?.reload() }
    fun find(tabId: Long, query: String) { views[tabId]?.findAllAsync(query) }
    fun clearFind(tabId: Long) { views[tabId]?.clearMatches() }
    fun findNext(tabId: Long, forward: Boolean) { views[tabId]?.findNext(forward) }
    fun zoomLevel(tabId: Long): Int = zoomLevels[tabId] ?: 100

    fun zoom(tabId: Long, percent: Int) {
        val view = views[tabId] ?: return
        val next = percent.coerceIn(50, 200)
        view.settings.textZoom = next
        zoomLevels[tabId] = next
    }

    fun setDesktopSite(tabId: Long, enabled: Boolean) {
        desktopSites[tabId] = enabled
        _snapshots.update { current -> current + (tabId to (current[tabId] ?: PageSnapshot()).copy(desktopSite = enabled)) }
        views[tabId]?.let { view ->
            view.settings.userAgentString = if (enabled) {
                DESKTOP_USER_AGENT
            } else {
                WebSettings.getDefaultUserAgent(webContexts[tabId] ?: activityContext)
            }
            view.settings.useWideViewPort = true
            view.settings.loadWithOverviewMode = true
            view.reload()
        }
    }

    fun destroy(tabId: Long) {
        views.remove(tabId)?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            runCatching {
                view.stopLoading()
                view.webChromeClient = null
                view.webViewClient = WebViewClient()
                view.loadUrl("about:blank")
                view.removeAllViews()
                view.destroy()
            }
        }
        zoomLevels.remove(tabId)
        desktopSites.remove(tabId)
        webContexts.remove(tabId)
        _snapshots.update { it - tabId }
    }

    fun destroyAll() {
        views.keys.toList().forEach(::destroy)
    }

    companion object {
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}

/**
 * Stable leaf node: it observes no chrome state and has no pointer interceptors.
 * The AndroidView update block is deliberately empty.
 */
@Composable
fun WebViewHost(
    pool: WebViewPool,
    tabId: Long,
    modifier: Modifier = Modifier
) {
    key(tabId) {
        AndroidView(
            factory = { pool.obtain(tabId) },
            modifier = modifier,
            update = { }
        )
    }
}
