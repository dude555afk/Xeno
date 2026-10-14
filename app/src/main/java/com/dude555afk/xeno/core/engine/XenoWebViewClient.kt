package com.dude555afk.xeno.core.engine

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Callbacks from a browsing session, decoupled from the WebView itself so the
 * ViewModel can observe load state without holding a live view reference.
 */
interface SessionListener {
    fun onPageStarted(url: String)
    fun onPageProgress(url: String, progress: Int)
    fun onPageFinished(url: String, title: String)
    fun onFavicon(url: String, icon: Bitmap?)
    fun onExternalNavigation(requestedUrl: String): Boolean = false
}

/**
 * A [WebViewClient] that reports load state and honours a request blocker.
 *
 * Blocking happens in [shouldInterceptRequest] rather than by mutating the page
 * afterwards — the request never leaves the process, which is both faster and
 * the only approach that reliably kills ad iframes.
 */
class XenoWebViewClient(
    private val listener: SessionListener,
    /** Return true to block the request outright. */
    private val blocker: (WebResourceRequest) -> Boolean = { false },
    private val assetLoader: WebViewAssetLoader
) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        listener.onPageStarted(url.orEmpty())
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        view?.title?.let { listener.onPageFinished(url.orEmpty(), it) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        listener.onPageFinished(url.orEmpty(), view?.title.orEmpty())
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        listener.onPageProgress(view?.url.orEmpty(), newProgress)
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest
    ): android.webkit.WebResourceResponse? {
        // Our own injector script always goes through, whatever the page tries.
        if (request.url.host == XenoAssetLoader.DOMAIN) return null

        if (blocker(request)) return EMPTY_RESPONSE

        return assetLoader.shouldInterceptRequest(request.url)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest
    ): Boolean {
        val url = request.url.toString()
        val scheme = request.url.scheme

        // Non-http schemes go to the OS, never into the WebView.
        if (scheme != "http" && scheme != "https" && scheme != "about" && scheme != "data") {
            return listener.onExternalNavigation(url)
        }

        // A _blank target is a popup — route it to a new tab instead of
        // silently swallowing it.
        if (request.isRedirect || request.hasGesture()) return false

        return false
    }

    private companion object {
        /**
         * A transparent, empty, immediately-complete response. Better than a
         * 403 — the page's own JS sees a successful load and moves on.
         */
        val EMPTY_RESPONSE = android.webkit.WebResourceResponse(
            "text/plain",
            "utf-8",
            java.io.ByteArrayInputStream(ByteArray(0))
        ).apply {
            setStatusCodeAndReasonPhrase(200, "OK")
        }
    }
}
