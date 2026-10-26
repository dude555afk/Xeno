package com.dude555afk.xeno.core.reader

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.dude555afk.xeno.core.engine.XenoAssetLoader
import com.dude555afk.xeno.core.model.ExtractedContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.resume

/**
 * Runs the Readability extractor inside a real page and hands back clean text.
 *
 * Three details that matter more than they look:
 *
 * 1. **`onPageFinished` is not enough.** SPAs fire it before any content is
 *    rendered, so after load completes we poll a JS readiness probe and keep
 *    going until the DOM text length stops changing.
 * 2. **The extractor works on a clone.** It never mutates the live page, so
 *    this is safe to point at the visible tab as well as an offscreen one.
 * 3. **`wordCount` arrives as a JSON number, not a string.** The original
 *    parser read it with a string-only accessor, so it silently became 0,
 *    which made [ExtractedContent.isJunk] true for *every* page in the app.
 *    See [parse] for why that one line broke both flagship features.
 *
 * The injector owns a [androidx.webkit.WebViewAssetLoader] and installs it
 * itself. Without that, offscreen WebViews used by the answer engine have no
 * asset loader, so `readability.js` resolves to a 404 and every extracted page
 * comes back junk — invisible on the visible tab, fatal everywhere else.
 */
class ReadabilityInjector(
    private val context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    private val assetLoader = XenoAssetLoader.create(context)

    companion object {
        private const val EXTRACT_CALL = "JSON.stringify(window.__xenoReadability.extract())"
        private const val READER_BOOTSTRAP_WAIT_MS = 3_000L
        private const val JS_CALLBACK_TIMEOUT_MS = 2_500L

        private const val PROBE_CALL =
            "(function(){try{var b=document.body;var t=(b&&b.innerText)||'';" +
                "return document.readyState+':'+t.length+':'+(window.__xenoReadability?'ready':'pending');" +
                "}catch(e){return 'err:0:pending';}})()"

        // Readability can be blocked by a site's CSP or fail on unusual markup.
        // Fall back to visible DOM text only; never attempt to bypass a paywall.
        private val FALLBACK_CALL = """
            (function(){
              try {
                var b = document.body;
                var t = (b && b.innerText) || '';
                var author = document.querySelector('meta[name="author"]');
                var description = document.querySelector('meta[name="description"]');
                return JSON.stringify({
                  url: location.href,
                  title: document.title || '',
                  byline: author ? (author.content || '') : '',
                  text: t,
                  excerpt: description ? (description.content || '') : '',
                  wordCount: t.trim() ? t.trim().split(/\s+/).length : 0
                });
              } catch(e) {
                return JSON.stringify({url: location.href, title: document.title || '', text: '', excerpt: '', wordCount: 0});
              }
            })()
        """.trimIndent()

        /** Injected once per navigation so [PROBE_CALL]/[EXTRACT_CALL] exist. */
        private const val BOOTSTRAP =
            "(function(){if(window.__xenoReadability)return;" +
                "var s=document.createElement('script');" +
                "s.src='https://appassets.androidplatform.net/assets/readability.js';" +
                "s.async=false;document.documentElement.appendChild(s);})();"
    }

    /**
     * Loads [url] into [webView] and extracts once the page settles.
     *
     * Returns a junk [ExtractedContent] rather than throwing on timeout — the
     * AI layer needs to be able to handle "couldn't read this" gracefully.
     *
     * Restores [WebView.getWebViewClient] when finished so the caller's client
     * is not silently replaced.
     */
    suspend fun extract(
        webView: WebView,
        url: String,
        timeoutMs: Long = 25_000L,
        quietForMs: Long = 900L
    ): ExtractedContent = withContext(Dispatchers.Main) {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        // We only want text. Images are the single largest bandwidth and memory
        // cost on a page and Readability never looks at them.
        webView.settings.loadsImagesAutomatically = false
        webView.settings.blockNetworkImage = true

        val previousClient = webView.webViewClient

        try {
            val loaded = withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine<Unit> { cont ->
                    var resumed = false
                    fun once(block: () -> Unit) { if (!resumed) { resumed = true; block() } }

                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, startedUrl: String?, favicon: android.graphics.Bitmap?) {
                            runCatching { view?.evaluateJavascript(BOOTSTRAP, null) }
                        }

                        override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                            runCatching { view?.evaluateJavascript(BOOTSTRAP, null) }
                            once { if (cont.isActive) cont.resume(Unit) }
                        }

                        override fun onReceivedError(
                            view: WebView?, request: WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                once { if (cont.isActive) cont.resume(Unit) }
                            }
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest
                        ): android.webkit.WebResourceResponse? =
                            assetLoader.shouldInterceptRequest(request.url)
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

                val readerReady = snapshot.contains(":ready")
                if (snapshot == last && snapshot.isNotBlank() && readerReady) {
                    if (quietSince == 0L) quietSince = now
                    if (now - quietSince >= quietForMs) break
                } else {
                    last = snapshot
                    quietSince = 0L
                    // The script asset can load later than the document on a
                    // slow phone. Wait briefly for it, then use DOM-text fallback
                    // instead of burning the entire page timeout.
                    if (!readerReady && now - start >= minOf(timeoutMs, READER_BOOTSTRAP_WAIT_MS)) break
                }
                delay(220)
            }

            val primary = parse(evalToString(webView, EXTRACT_CALL), url)
            if (!primary.isJunk) primary else {
                // The fallback reads only text currently exposed by the page.
                // It does not unlock subscriber-only content or bypass access controls.
                val fallback = parse(evalToString(webView, FALLBACK_CALL), url)
                if (!fallback.isJunk) fallback else primary
            }
        } finally {
            // Restore even on cancellation/JS bridge failure: this class may be
            // used with a retained WebView, so leaking the temporary client breaks navigation.
            webView.webViewClient = previousClient
        }
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
        withTimeoutOrNull(JS_CALLBACK_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                webView.evaluateJavascript(script) { value ->
                    if (cont.isActive) cont.resume(value)
                }
            }
        }

    /**
     * Parse the JS object handed back by `__xenoReadability.extract()`.
     *
     * `evaluateJavascript` returns a JS *expression*, which for an object is
     * the JSON text wrapped in quotes. So the raw value is unwrapped once with
     * [JsonPrimitive.content] to get the JSON string, then parsed again as the
     * object. Skipping the first unwrap is the classic mistake here — it yields
     * a JsonPrimitive instead of a JsonObject and every field reads as absent.
     *
     * Numbers are read through [num] rather than [str] because the extractor
     * emits `wordCount` as a JSON number. `takeIf { it.isString }` skips it,
     * `toIntOrNull` then returns null, and the field silently became 0 — which
     * made [ExtractedContent.isJunk] true for every page, so the summarizer
     * always said "couldn't be read" and the answer engine always reported zero
     * readable sources. Neither symptom points at this line.
     */
    private fun parse(raw: String?, fallbackUrl: String): ExtractedContent {
        if (raw.isNullOrBlank() || raw == "null") return ExtractedContent.junk(fallbackUrl)
        return runCatching {
            // WebView implementations may return a JSON object directly or a
            // JSON-encoded string when the JS expression uses JSON.stringify.
            // Accept both forms instead of treating the object form as junk.
            val first = json.parseToJsonElement(raw)
            val obj = when (first) {
                is JsonObject -> first
                is JsonPrimitive -> if (first.isString) {
                    json.parseToJsonElement(first.content) as? JsonObject
                } else null
                else -> null
            } ?: return ExtractedContent.junk(fallbackUrl)

            fun str(key: String): String =
                (obj[key] as? JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty()

            fun num(key: String): Int =
                (obj[key] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0

            val text = str("text")
            val explicitCount = num("wordCount")
            ExtractedContent(
                url = str("url").ifBlank { fallbackUrl },
                title = str("title"),
                byline = str("byline").ifBlank { null },
                text = text,
                excerpt = str("excerpt"),
                wordCount = explicitCount.takeIf { it > 0 }
                    ?: text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
            )
        }.getOrElse { ExtractedContent.junk(fallbackUrl) }
    }
}
