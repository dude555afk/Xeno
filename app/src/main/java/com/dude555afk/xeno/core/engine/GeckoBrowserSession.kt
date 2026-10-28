package com.dude555afk.xeno.core.engine

import android.content.Context
import android.graphics.Canvas
import android.net.Uri
import android.view.ViewGroup
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Engine adapter for a single Xeno tab. Browser UI code uses this explicit
 * contract instead of treating GeckoSession as if it were an Android WebView.
 */
class GeckoBrowserSession(
    context: Context,
    runtime: GeckoRuntime,
    privateMode: Boolean
) {
    val view: GeckoView = GeckoView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        // Arc-style chrome overlays the live page. TextureView supports Compose
        // overlays and animated surfaces; SurfaceView would punch through them.
        setViewBackend(GeckoView.BACKEND_TEXTURE_VIEW)
    }

    val session: GeckoSession = GeckoSession(
        GeckoSessionSettings.Builder()
            .usePrivateMode(privateMode)
            .useTrackingProtection(true)
            .allowJavascript(true)
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
            .build()
    )

    val width: Int get() = view.width
    val height: Int get() = view.height

    var url: String = ""
        private set
    var title: String = ""
        private set
    var progress: Int = 0
        private set
    var loading: Boolean = false
        private set
    var canGoBack: Boolean = false
        private set
    var canGoForward: Boolean = false
        private set

    private var listener: SessionListener? = null
    private var scrollListener: ((Int) -> Unit)? = null
    private var findListener: ((Int, Int, Boolean) -> Unit)? = null
    private var lastFindQuery = ""
    private val closed = AtomicBoolean(false)

    init {
        session.open(runtime)
        view.setSession(session)

        session.setProgressDelegate(object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                this@GeckoBrowserSession.url = url
                loading = true
                progress = 0
                listener?.onPageStarted(url)
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                this@GeckoBrowserSession.progress = progress.coerceIn(0, 100)
                listener?.onPageProgress(url, this@GeckoBrowserSession.progress)
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                loading = false
                progress = 100
                if (url.isNotBlank() && url != "about:blank") {
                    if (success) listener?.onPageFinished(url, title)
                    else listener?.onPageError(url, "The page could not finish loading.")
                }
            }
        })

        session.setNavigationDelegate(object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: List<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean?
            ) {
                this@GeckoBrowserSession.url = url.orEmpty()
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                this@GeckoBrowserSession.canGoBack = canGoBack
                listener?.onHistoryChanged(this@GeckoBrowserSession.canGoBack, this@GeckoBrowserSession.canGoForward)
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                this@GeckoBrowserSession.canGoForward = canGoForward
                listener?.onHistoryChanged(this@GeckoBrowserSession.canGoBack, this@GeckoBrowserSession.canGoForward)
            }

            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny>? {
                val target = request.uri
                val scheme = Uri.parse(target).scheme?.lowercase().orEmpty()
                if (scheme in setOf("http", "https", "about", "data", "blob")) return null
                listener?.onExternalNavigation(target)
                return GeckoResult.fromValue(AllowOrDeny.DENY)
            }

            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                // Xeno's own tab model owns all sessions and enforces the live-session cap.
                listener?.onPopupRequested(uri)
                return null
            }
        })

        session.setContentDelegate(object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                this@GeckoBrowserSession.title = title.orEmpty()
            }

            override fun onCrash(session: GeckoSession) {
                loading = false
                listener?.onEngineCrashed(this@GeckoBrowserSession, true)
            }

            override fun onKill(session: GeckoSession) {
                loading = false
                listener?.onEngineCrashed(this@GeckoBrowserSession, false)
            }

            override fun onCloseRequest(session: GeckoSession) {
                listener?.onCloseRequested()
            }

            override fun onExternalResponse(
                session: GeckoSession,
                response: GeckoSession.WebResponseInfo
            ) {
                listener?.onDownloadRequested(response.uri, response.contentType)
            }
        })

        session.setScrollDelegate(object : GeckoSession.ScrollDelegate {
            override fun onScrollChanged(session: GeckoSession, scrollX: Int, scrollY: Int) {
                scrollListener?.invoke(scrollY)
            }
        })

        session.getFinder().setDisplayFlags(GeckoSession.FINDER_DISPLAY_HIGHLIGHT_ALL)
    }

    fun setListener(value: SessionListener?) {
        listener = value
    }

    fun setScrollListener(value: ((Int) -> Unit)?) {
        scrollListener = value
    }

    fun loadUrl(target: String) {
        if (!closed.get()) session.load(GeckoSession.Loader.createLoadUri(target))
    }

    fun reload() {
        if (!closed.get()) session.reload()
    }

    fun stopLoading() {
        if (!closed.get()) session.stop()
    }

    fun goBack() {
        if (!closed.get() && canGoBack) session.goBack()
    }

    fun goForward() {
        if (!closed.get() && canGoForward) session.goForward()
    }

    fun canGoBack(): Boolean = canGoBack
    fun canGoForward(): Boolean = canGoForward

    fun onPause() {
        session.setActive(false)
        session.setFocused(false)
    }

    fun onResume() {
        if (!closed.get()) {
            session.setActive(true)
            session.setFocused(true)
        }
    }

    fun setJavaScriptEnabled(enabled: Boolean) {
        session.settings.setAllowJavascript(enabled)
    }

    fun setDesktopMode(enabled: Boolean) {
        session.settings.setUserAgentMode(
            if (enabled) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
            else GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        )
        session.settings.setViewportMode(
            if (enabled) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
            else GeckoSessionSettings.VIEWPORT_MODE_MOBILE
        )
    }

    fun clearMatches() {
        session.getFinder().clear()
        findListener?.invoke(0, 0, true)
    }

    fun setFindListener(value: ((Int, Int, Boolean) -> Unit)?) {
        findListener = value
    }

    fun findAllAsync(query: String) {
        lastFindQuery = query
        if (query.isBlank()) {
            clearMatches()
            return
        }
        session.getFinder().find(query, GeckoSession.FINDER_FIND_FORWARD).accept { result ->
            findListener?.invoke(
                (result.current - 1).coerceAtLeast(0),
                result.total.coerceAtLeast(0),
                true
            )
        }
    }

    fun findNext(forward: Boolean) {
        if (lastFindQuery.isBlank()) return
        session.getFinder().findNext(!forward).accept { result ->
            findListener?.invoke(
                (result.current - 1).coerceAtLeast(0),
                result.total.coerceAtLeast(0),
                true
            )
        }
    }

    fun clearHistory() {
        session.purgeHistory()
    }

    fun draw(canvas: Canvas) {
        view.draw(canvas)
    }

    fun clearAndClose() {
        if (!closed.compareAndSet(false, true)) return
        runCatching { view.releaseSession() }
        runCatching { session.close() }
        listener = null
        scrollListener = null
        findListener = null
    }
}
