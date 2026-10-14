package com.dude555afk.xeno.core.reader

import android.webkit.WebView
import com.dude555afk.xeno.core.model.ExtractedContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.resume

/**
 * Runs the Readability extractor inside a real page and hands back clean text.
 *
 * Two details that matter more than they look:
 *
 * 1. **`onPageFinished` is not enough.** SPAs fire it before any content is
 *    rendered, so after load completes we poll a JS readiness probe and keep
 *    going until the DOM text length stops changing.
 * 2. **The extractor works on a clone.** It never mutates the live page, so
 *    this is safe to point at the visible tab as well as an offscreen one.
 */
class ReadabilityInjector(
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    companion object {
        private const val EXTRACT_CALL = "window.__xenoReadability.extract()"
        private const val PROBE_CALL =
            "(function(){try{var b=document.body;var t=(b&&b.innerText)||'';" +
                "return document.readyState+':'+t.length;}catch(e){return 'err:0';}})()"

        /** Injected once per WebView so [PROBE_CALL]/[EXTRACT_CALL] exist. */
        private const val BOOTSTRAP =
            "(function(){var s=document.createElement('script');" +
                "s.src='https://appassets.androidplatform.net/assets/readability.js';" +
                "s.async=false;document.documentElement.appendChild(s);})();"
    }

    /**
     * Loads [url] into [webView] and extracts once the page settles.
     *
     * Returns a junk [ExtractedContent] rather than throwing on timeout — the
     * AI layer needs to be able to handle "couldn't read this" gracefully.
     */
    suspend fun extract(
        webView: WebView,
        url: String,
        timeoutMs: Long = 25_000L,
        quietForMs: Long = 900L
    ): ExtractedContent = withContext(Dispatchers.Main) {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadsImagesAutomatically = false // we only want text

        val loaded = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Unit> { cont ->
                var resumed = false
                fun once(block: () -> Unit) { if (!resumed) { resumed = true; block() } }

                webView.webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                        once { if (cont.isActive) cont.resume(Unit) }
                    }

                    override fun onReceivedError(
                        view: WebView?, request: android.webkit.WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            once { if (cont.isActive) cont.resume(Unit) }
                        }
                    }
                }
                webView.loadUrl(url)
            }
        }
        if (loaded == null) return@withContext ExtractedContent.junk(url)

        // Wait for the DOM to go quiet, then extract.
        var last = ""
        var quietSince = 0L
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < timeoutMs) {
            val snapshot = evalToString(webView, PROBE_CALL) ?: ""
            val now = System.currentTimeMillis()

            if (snapshot == last && snapshot.isNotBlank() && !snapshot.startsWith("loading")) {
                if (quietSince == 0L) quietSince = now
                if (now - quietSince >= quietForMs) break
            } else {
                last = snapshot
                quietSince = 0L
            }
            delay(220)
        }

        val raw = evalToString(webView, EXTRACT_CALL)
        parse(raw, url)
    }

    /**
     * Bootstraps the extractor into an already-loaded page.
     *
     * Loaded via the WebView asset loader ([com.dude555afk.xeno.core.engine.XenoAssetLoader])
     * so it respects a page's own CSP where possible — matching origin where the
     * page allows it. A page that blocks it simply yields junk and the caller
     * degrades to TextRank.
     */
    fun bootstrapScript(): String = BOOTSTRAP

    private suspend fun evalToString(webView: WebView, script: String): String? =
        suspendCancellableCoroutine { cont ->
            webView.evaluateJavascript(script) { value ->
                if (cont.isActive) cont.resume(value)
            }
        }

    private fun parse(raw: String?, fallbackUrl: String): ExtractedContent {
        if (raw.isNullOrBlank() || raw == "null") return ExtractedContent.junk(fallbackUrl)
        return runCatching {
            // evaluateJavascript hands back a JS string literal, so unwrap once.
            val literal = json.parseToJsonElement(raw).jsonPrimitive.content
            val obj = json.parseToJsonElement(literal) as? JsonObject
                ?: return ExtractedContent.junk(fallbackUrl)

            fun str(key: String) = obj[key]?.jsonPrimitive?.takeIf { it.isString }?.content.orEmpty()

            ExtractedContent(
                url = str("url").ifBlank { fallbackUrl },
                title = str("title"),
                byline = str("byline").ifBlank { null },
                text = str("text"),
                excerpt = str("excerpt"),
                wordCount = str("wordCount").toIntOrNull() ?: 0
            )
        }.getOrElse { ExtractedContent.junk(fallbackUrl) }
    }
}
