package com.dude555afk.xeno.core.engine

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream

/**
 * Callbacks from a browsing session, decoupled from the WebView itself so the
 * ViewModel can observe load state without holding a live view reference.
 */
interface SessionListener {
    fun onPageStarted(url: String)
    fun onPageProgress(url: String, progress: Int)
    fun onPageFinished(url: String, title: String)
    fun onPageError(url: String, description: String) = Unit
    fun onFavicon(url: String, icon: Bitmap?)
    fun onExternalNavigation(requestedUrl: String): Boolean = false
    fun onRenderProcessGone(view: WebView, didCrash: Boolean) = Unit
    fun onEngineCrashed(session: GeckoBrowserSession, didCrash: Boolean) = Unit
    fun onHistoryChanged(canGoBack: Boolean, canGoForward: Boolean) = Unit
    fun onPopupRequested(url: String) = Unit
    fun onCloseRequested() = Unit
    fun onDownloadRequested(url: String, contentType: String?) = Unit
}

/**
 * A [WebViewClient] that reports load state and honours a request blocker.
 *
 * Blocking happens in [shouldInterceptRequest] rather than by mutating the page
 * afterwards — the request never leaves the process, which is both faster and
 * the only approach that reliably kills ad iframes.
 *
 * @param cosmeticSelectors selectors to strip from whatever page loads next.
 *   Passed per load rather than read from a field so the caller controls the
 *   timing; cosmetic rules depend on the document URL, which only exists once
 *   a navigation starts.
 */
class XenoWebViewClient(
    private val listener: SessionListener,
    /** Return true to block the request outright. */
    private val blocker: (WebResourceRequest) -> Boolean = { false },
    private val assetLoader: androidx.webkit.WebViewAssetLoader,
    private val cosmeticSelectors: () -> List<String> = { emptyList() },
    private val fingerprintingProtection: () -> Boolean = { false }
) : WebViewClient() {

    // WebView commonly calls onPageFinished after a main-frame network error
    // because it has finished loading its internal error document. Remember
    // the failed URL so the ViewModel cannot accidentally mark that navigation
    // as successful and clear the error banner.
    private var failedMainFrameUrl: String? = null

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        val target = url.orEmpty()
        failedMainFrameUrl = null
        listener.onPageStarted(target)

        // Do not inject here: evaluateJavascript can still target the previous
        // document while the new one is navigating. Injection happens at
        // onPageCommitVisible below, when the new document actually exists.
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        // This is the first safe point to target the newly committed document.
        // Running evaluateJavascript from onPageStarted can hit the previous
        // document and silently lose cosmetic/fingerprinting protections.
        if (view != null) {
            val target = url.orEmpty()
            val script = XenoInjector.script(
                documentUrl = target,
                cosmeticSelectors = cosmeticSelectors(),
                // Preserve native window.open so sign-in, checkout and other user-gesture
                // popups reach WebChromeClient.onCreateWindow and become tabs.
                redirectPopups = false,
                fingerprintingProtection = fingerprintingProtection()
            )
            if (script != null) runCatching { view.evaluateJavascript(script, null) }
        }

        val title = view?.title.orEmpty()
        if (title.isNotBlank()) listener.onPageProgress(url.orEmpty(), 100)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        val finishedUrl = url.orEmpty()
        if (failedMainFrameUrl == finishedUrl) return
        listener.onPageFinished(finishedUrl, view?.title.orEmpty())
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: android.webkit.WebResourceError?
    ) {
        if (request?.isForMainFrame == true) {
            val failedUrl = request.url.toString()
            failedMainFrameUrl = failedUrl
            listener.onPageError(
                failedUrl,
                error?.description?.toString().orEmpty()
            )
        }
    }

    override fun onRenderProcessGone(
        view: WebView,
        detail: android.webkit.RenderProcessGoneDetail
    ): Boolean {
        // Android requires the host to remove references to a dead WebView and
        // return true; returning false can terminate the entire app.
        listener.onRenderProcessGone(view, detail.didCrash())
        return true
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest
    ): WebResourceResponse? {
        // Serve our bundled JS/CSS through WebViewAssetLoader. Returning null
        // here would let WebView try the public network for this synthetic host,
        // silently breaking Xeno's own reader/fingerprinting assets.
        if (request.url.host == XenoAssetLoader.DOMAIN) {
            return assetLoader.shouldInterceptRequest(request.url)
        }

        // Never let a content filter cancel a top-level navigation. Some filter lists
        // contain broad document rules; applying those here can make ordinary sites
        // appear completely unreachable. Keep aggressive filtering for subresources.
        if (!request.isForMainFrame && blocker(request)) return emptyResponse()

        return assetLoader.shouldInterceptRequest(request.url)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest
    ): Boolean {
        val url = request.url.toString()
        val scheme = request.url.scheme

        // Non-http schemes go to the OS, never into the WebView. Returning
        // true means "handled"; http/https falls through to false, which lets
        // the WebView navigate normally.
        if (scheme != "http" && scheme != "https" && scheme != "about" && scheme != "data") {
            // Consume unsupported schemes even when the OS has no handler.
            // Returning false would send tel:, mailto:, intent:, etc. back into
            // WebView, where they become confusing blank/error navigations.
            listener.onExternalNavigation(url)
            return true
        }

        return false
    }

    /**
     * A transparent, empty, immediately-complete response.
     *
     * Built fresh per call, never shared: a [WebResourceResponse] wraps a
     * one-shot InputStream, so reusing a single instance hands the first
     * blocked request an empty body and every later one a closed stream —
     * which shows up in the page console as a load error and can trip error
     * handling that a 200 never would.
     *
     * Better than a 403 in any case — the page's own JS sees a successful load
     * and moves on instead of retrying.
     */
    private fun emptyResponse(): WebResourceResponse =
        WebResourceResponse(
            "text/plain",
            "utf-8",
            ByteArrayInputStream(ByteArray(0))
        ).apply {
            setStatusCodeAndReasonPhrase(200, "OK")
        }
}
