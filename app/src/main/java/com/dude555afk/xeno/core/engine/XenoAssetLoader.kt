package com.dude555afk.xeno.core.engine

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader

/**
 * Serves our bundled assets on a virtual https origin.
 *
 * `appassets.androidplatform.net` is the documented host, which is what lets
 * the readability injector be loaded into a page as a same-origin-ish script
 * rather than an opaque `file://` URL that CSP and mixed-content rules hate.
 *
 * Kept in its own file rather than alongside [XenoInjector] — they were shipped
 * together once, which compiled fine but made both harder to find.
 */
object XenoAssetLoader {

    const val DOMAIN = "appassets.androidplatform.net"
    const val ORIGIN = "https://$DOMAIN"

    fun create(context: Context): WebViewAssetLoader =
        WebViewAssetLoader.Builder()
            .setDomain(DOMAIN)
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()

    /** Interceptor to hand to [WebViewClient.shouldInterceptRequest]. */
    fun interceptor(
        loader: WebViewAssetLoader
    ): (WebResourceRequest) -> WebResourceResponse? = { request ->
        loader.shouldInterceptRequest(request.url)
    }
}
