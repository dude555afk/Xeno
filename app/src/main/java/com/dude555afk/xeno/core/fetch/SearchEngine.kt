package com.dude555afk.xeno.core.fetch

import android.content.Context
import android.webkit.WebView
import com.dude555afk.xeno.core.model.SearchHit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.resume

/**
 * Scrapes search results by loading a search engine in a real WebView.
 *
 * This is the trick that makes anonymous search work on Android. A plain
 * `http.get()` gets a 403 or a JS challenge; a real WebView has real headers,
 * real JS execution and a real TLS fingerprint, so it sails through. We already
 * ship a WebView, so using it as the scraper costs us roughly nothing.
 *
 * The JS targets several selectors at once because engines change their DOM
 * without warning. If every pattern misses, the caller moves to the next
 * engine in [engines].
 */
class SearchEngine(private val context: Context) {

    private val json: Json = Json { ignoreUnknownKeys = true }

    data class Engine(
        val name: String,
        val urlFor: (String) -> String,
        val extractScript: String
    )

    /** Engines in priority order. More entries is cheap insurance. */
    val engines = listOf(
        Engine(
            name = "DuckDuckGo",
            urlFor = { q -> "https://duckduckgo.com/?q=" + q.urlEncoded() + "&ia=web" },
            extractScript = SCRAPE_DDG
        ),
        Engine(
            name = "DuckDuckGo Lite",
            urlFor = { q -> "https://lite.duckduckgo.com/lite/?q=" + q.urlEncoded() },
            extractScript = SCRAPE_DDG_LITE
        ),
        Engine(
            name = "Mojeek",
            urlFor = { q -> "https://www.mojeek.com/search?q=" + q.urlEncoded() },
            extractScript = SCRAPE_GENERIC
        ),
        Engine(
            name = "SearXNG",
            urlFor = { q -> "https://searx.be/search?q=" + q.urlEncoded() },
            extractScript = SCRAPE_GENERIC
        )
    )

    /** Try each engine in turn until one yields usable hits. */
    suspend fun search(query: String, limit: Int = 8): List<SearchHit> =
        withContext(Dispatchers.Main) {
            for (engine in engines) {
                val view = createWebView()
                val hits = try {
                    scrape(engine, view, query, limit)
                } finally {
                    runCatching { view.destroy() }
                }
                val clean = hits.filter { it.url.startsWith("http") }
                if (clean.isNotEmpty()) return@withContext clean
            }
            emptyList()
        }

    private fun createWebView(): WebView = WebView(context).apply {
 settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.blockNetworkImage = true
        settings.loadsImagesAutomatically = false
    }

    private suspend fun scrape(
        engine: Engine,
        webView: WebView,
        query: String,
        limit: Int
    ): List<SearchHit> {
        val loaded = withTimeoutOrNull(ENGINE_TIMEOUT_MS) {
            suspendCancellableCoroutine<Unit> { cont ->
                var resumed = false
                fun once() { if (!resumed) { resumed = true; if (cont.isActive) cont.resume(Unit) } }
                webView.webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) = once()
                    override fun onReceivedError(
                        view: WebView?, request: android.webkit.WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) { if (request?.isForMainFrame == true) once() }
                }
                webView.loadUrl(engine.urlFor(query))
            }
        }
        if (loaded == null) return emptyList()

        // Results render client-side, so scrape twice and keep the bigger set.
        var best: List<SearchHit> = emptyList()
        delay(700)
        parseHits(eval(webView, engine.extractScript), limit).let {
            if (it.size > best.size) best = it
        }
        delay(1_200)
        parseHits(eval(webView, engine.extractScript), limit).let {
            if (it.size > best.size) best = it
        }
        return best
    }

    private suspend fun eval(webView: WebView, script: String): String? =
        suspendCancellableCoroutine { cont ->
            webView.evaluateJavascript(script) { value ->
                if (cont.isActive) cont.resume(value)
            }
        }

    private fun parseHits(raw: String?, limit: Int): List<SearchHit> {
        if (raw.isNullOrBlank() || raw == "null") return emptyList()
        return runCatching {
            val literal = json.parseToJsonElement(raw).jsonPrimitive.content
            val array = json.parseToJsonElement(literal) as? JsonArray ?: return emptyList()

            array.mapNotNull { element ->
                val obj = element as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
                val url = obj["u"]?.jsonPrimitive?.contentOrNull().orEmpty()
                val title = obj["t"]?.jsonPrimitive?.contentOrNull().orEmpty()
                val snippet = obj["s"]?.jsonPrimitive?.contentOrNull().orEmpty()
                if (url.isBlank()) null else SearchHit(url, title.ifBlank { url }, snippet)
            }.distinctBy { it.url }.take(limit)
        }.getOrDefault(emptyList())
    }

    private fun String.urlEncoded(): String =
        java.net.URLEncoder.encode(this, "UTF-8")

    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNull(): String? =
        if (isString) content else null

    companion object {
        private const val ENGINE_TIMEOUT_MS = 15_000L

        /** DDG's result containers. `data-testid` is the stable one. */
        private val SCRAPE_DDG = """
            (function(){
              var out = [];
              var nodes = document.querySelectorAll(
                '[data-testid="result"], article[data-testid="result"], .result__body, .result');
              for (var i = 0; i < nodes.length && out.length < 12; i++) {
                var n = nodes[i];
                var a = n.querySelector('a[href]');
                if (!a) continue;
                var href = a.href || '';
                if (href.indexOf('http') !== 0) continue;
                if (href.indexOf('duckduckgo.com') !== -1) continue;
                var s = n.querySelector('.result__snippet, [data-result="snippet"]');
                out.push({u: href, t: (a.innerText || '').trim().slice(0,120), s: s ? (s.innerText||'').trim().slice(0,220) : ''});
              }
              return JSON.stringify(out);
            })()
        """.trimIndent()

        private val SCRAPE_DDG_LITE = """
            (function(){
              var out = [];
              var links = document.querySelectorAll('a.result-link');
              var snips = document.querySelectorAll('.result-snippet');
              for (var i = 0; i < links.length && out.length < 12; i++) {
                var a = links[i];
                if (!a.href || a.href.indexOf('http') !== 0) continue;
                out.push({u: a.href, t: (a.innerText||'').trim().slice(0,120), s: snips[i] ? (snips[i].innerText||'').trim().slice(0,220) : ''});
              }
              return JSON.stringify(out);
            })()
        """.trimIndent()

        /** Generic fallback for anything with a plausible result list. */
        private val SCRAPE_GENERIC = """
            (function(){
              var out = [];
              var seen = {};
              var as = document.querySelectorAll('main a[href], #results a[href], .results a[href], ol a[href], ul li a[href]');
              for (var i = 0; i < as.length && out.length < 12; i++) {
                var a = as[i];
                var h = a.href || '';
                if (h.indexOf('http') !== 0) continue;
                if (seen[h]) continue;
                var host = h.split('/')[2] || '';
                if (host.indexOf('mojeek') !== -1 || host.indexOf('searx') !== -1) continue;
                seen[h] = 1;
                out.push({u: h, t: (a.innerText||'').trim().slice(0,120), s: ''});
              }
              return JSON.stringify(out);
            })()
        """.trimIndent()
    }
}
