package com.dude555afk.xeno.core.fetch

import com.dude555afk.xeno.core.model.SearchHit
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Small, keyless HTML search client adapted from the multi-engine approach in
 * AndroidHarness. It uses the OkHttp dependency Xeno already ships; no second
 * browser engine, SDK, native library, or API key is added.
 *
 * Search pages are fetched as bounded HTML and parsed as data. Their scripts
 * are never executed. A failed provider falls through to the next provider.
 */
class SearchEngine(
    private val preferredEngine: () -> String = { "brave" },
    private val client: OkHttpClient = defaultClient()
) {
    private data class Engine(val id: String, val searchUrl: (String) -> String)

    private val engines = listOf(
        Engine("duckduckgo") { "https://html.duckduckgo.com/html/?q=" + encode(it) },
        Engine("bing") { "https://www.bing.com/search?q=" + encode(it) },
        Engine("brave") { "https://search.brave.com/search?q=" + encode(it) },
        Engine("google") { "https://www.google.com/search?q=" + encode(it) + "&num=10" }
    )

    suspend fun search(query: String, limit: Int = 8): List<SearchHit> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().replace(Regex("\\s+"), " ").take(240)
        if (cleanQuery.isBlank() || limit <= 0) return@withContext emptyList()

        val preferred = normalizeEngine(preferredEngine())
        val cacheKey = "$preferred|$cleanQuery".lowercase()
        cached(cacheKey)?.let { return@withContext it.take(limit) }

        withTimeoutOrNull(SEARCH_TOTAL_TIMEOUT_MS) {
            for (engine in orderedEngines(preferred)) {
                val html = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                    fetchHtml(engine.searchUrl(cleanQuery))
                }.orEmpty()
                if (html.isBlank()) continue

                val results = parseHtml(engine.id, html)
                    .mapNotNull { hit ->
                        val url = safeSearchResultUrl(hit.url) ?: return@mapNotNull null
                        hit.copy(url = url)
                    }
                    .distinctBy { canonicalResearchUrl(it.url) }
                    .take(limit)

                if (results.isNotEmpty()) {
                    cache(cacheKey, results)
                    return@withTimeoutOrNull results
                }
            }
            emptyList()
        } ?: emptyList()
    }

    private fun orderedEngines(preferred: String): List<Engine> {
        val first = engines.firstOrNull { it.id == preferred }
        return if (first == null) engines else listOf(first) + engines.filterNot { it.id == preferred }
    }

    private suspend fun fetchHtml(url: String): String? = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-IN,en;q=0.9")
            .build()
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: java.io.IOException) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!continuation.isActive) return
                    try {
                        if (!response.isSuccessful) {
                            continuation.resume(null)
                            return
                        }
                        val body = response.body
                        if (body == null || (body.contentLength() > MAX_HTML_BYTES)) {
                            continuation.resume(null)
                            return
                        }
                        val output = ByteArrayOutputStream()
                        body.byteStream().use { input ->
                            val buffer = ByteArray(8 * 1024)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                if (output.size() + count > MAX_HTML_BYTES) {
                                    continuation.resume(null)
                                    return
                                }
                                output.write(buffer, 0, count)
                            }
                        }
                        if (continuation.isActive) continuation.resume(output.toString(Charsets.UTF_8.name()))
                    } catch (error: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                }
            }
        })
    }

    internal fun parseHtml(engine: String, html: String): List<SearchHit> = when (normalizeEngine(engine)) {
        "duckduckgo" -> parseDuckDuckGo(html)
        "bing" -> parseBing(html)
        "brave" -> parseBrave(html)
        "google" -> parseGoogle(html)
        else -> emptyList()
    }

    private fun parseDuckDuckGo(html: String): List<SearchHit> {
        val links = Regex(
            """(?is)<a\b(?=[^>]*\bclass\s*=\s*["'][^"']*result__a[^"']*["'])[^>]*\bhref\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>"""
        ).findAll(html).toList()
        val snippets = Regex(
            """(?is)<(?:a|div|span)\b[^>]*class\s*=\s*["'][^"']*result__snippet[^"']*["'][^>]*>(.*?)</(?:a|div|span)>"""
        ).findAll(html).map { cleanHtml(it.groupValues[1]) }.toList()

        return links.mapIndexedNotNull { index, match ->
            val href = unwrapDuckDuckGo(decodeEntities(match.groupValues[1]))
            val title = cleanHtml(match.groupValues[2])
            if (title.isBlank()) null
            else SearchHit(href, title, snippets.getOrElse(index) { "" }.take(MAX_SNIPPET_CHARS))
        }
    }

    private fun parseBing(html: String): List<SearchHit> {
        val blocks = Regex(
            """(?is)<li\b[^>]*class\s*=\s*["'][^"']*b_algo[^"']*["'][^>]*>(.*?)(?:</li>|$)"""
        ).findAll(html)
        return blocks.mapNotNull { block ->
            val link = Regex(
                """(?is)<h2[^>]*>\s*<a[^>]*href\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>"""
            ).find(block.groupValues[1]) ?: return@mapNotNull null
            val snippet = Regex("""(?is)<p[^>]*>(.*?)</p>""")
                .find(block.groupValues[1])?.groupValues?.get(1).orEmpty()
            val title = cleanHtml(link.groupValues[2])
            if (title.isBlank()) null else SearchHit(
                decodeEntities(link.groupValues[1]),
                title,
                cleanHtml(snippet).take(MAX_SNIPPET_CHARS)
            )
        }.toList()
    }

    private fun parseBrave(html: String): List<SearchHit> {
        val results = Regex("""(?is)<div\b[^>]*id\s*=\s*["']results["'][^>]*>(.*)""")
            .find(html)?.groupValues?.get(1) ?: html
        val blocks = Regex(
            """(?is)<div\b[^>]*class\s*=\s*["'][^"']*snippet[^"']*["'][^>]*>(.*?)(?:</div>\s*</div>|$)"""
        ).findAll(results).toList()
        val found = blocks.mapNotNull { block ->
            val link = Regex("""(?is)<a[^>]*href\s*=\s*["'](https?://[^"']+)["'][^>]*>(.*?)</a>""")
                .find(block.groupValues[1]) ?: return@mapNotNull null
            val title = cleanHtml(link.groupValues[2])
            val snippet = Regex("""(?is)class\s*=\s*["'][^"']*snippet-description[^"']*["'][^>]*>(.*?)</""")
                .find(block.groupValues[1])?.groupValues?.get(1).orEmpty()
            if (title.isBlank()) null else SearchHit(
                decodeEntities(link.groupValues[1]), title, cleanHtml(snippet).take(MAX_SNIPPET_CHARS)
            )
        }
        if (found.isNotEmpty()) return found

        return parseExternalAnchors(results)
    }

    private fun parseGoogle(html: String): List<SearchHit> {
        val links = Regex(
            """(?is)<a\b[^>]*href\s*=\s*["']([^"']+)["'][^>]*>.*?<h3[^>]*>(.*?)</h3>.*?</a>"""
        ).findAll(html)
        return links.mapNotNull { match ->
            val raw = decodeEntities(match.groupValues[1])
            val url = if (raw.startsWith("/url?")) {
                val q = Regex("""(?:\?|&)q=([^&]+)""").find(raw)?.groupValues?.get(1)
                q?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() }.orEmpty()
            } else raw
            val title = cleanHtml(match.groupValues[2])
            if (title.isBlank() || url.isBlank()) null else SearchHit(url, title)
        }.toList()
    }

    private fun parseExternalAnchors(html: String): List<SearchHit> {
        val anchors = Regex("""(?is)<a\b[^>]*href\s*=\s*["'](https?://[^"']+)["'][^>]*>(.*?)</a>""")
        return anchors.findAll(html).mapNotNull { match ->
            val title = cleanHtml(match.groupValues[2])
            if (title.length < 3) null else SearchHit(
                decodeEntities(match.groupValues[1]), title.take(180)
            )
        }.toList()
    }

    private fun unwrapDuckDuckGo(raw: String): String {
        val decoded = decodeEntities(raw)
        val uri = runCatching { URI(decoded) }.getOrNull() ?: return decoded
        if (uri.host?.endsWith("duckduckgo.com", true) != true) return decoded
        val target = uri.rawQuery.orEmpty().split('&')
            .firstOrNull { it.startsWith("uddg=") }
            ?.substringAfter('=')
            ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() }
        return target ?: decoded
    }

    private fun cleanHtml(raw: String): String {
        val withoutNoise = Regex("""(?is)<(script|style|noscript)\b[^>]*>.*?</\1>""")
            .replace(raw, " ")
        val text = Regex("""<[^>]+>""").replace(withoutNoise, " ")
        return decodeEntities(text).replace(Regex("""\s+"""), " ").trim()
    }

    private fun decodeEntities(value: String): String {
        val named = value
            .replace("&amp;", "&", true)
            .replace("&quot;", "\"", true)
            .replace("&#39;", "'", true)
            .replace("&apos;", "'", true)
            .replace("&lt;", "<", true)
            .replace("&gt;", ">", true)
            .replace("&nbsp;", " ", true)
        return Regex("""&#(x[0-9a-fA-F]+|[0-9]+);""").replace(named) { match ->
            val token = match.groupValues[1]
            val code = if (token.startsWith("x", true)) token.drop(1).toIntOrNull(16) else token.toIntOrNull()
            code?.takeIf { it in 1..0x10FFFF }?.let { String(Character.toChars(it)) } ?: " "
        }
    }

    private fun safeSearchResultUrl(raw: String): String? {
        if (!isSafePublicWebUrl(raw)) return null
        val uri = runCatching { URI(raw.trim()) }.getOrNull() ?: return null
        val host = uri.host?.lowercase().orEmpty()
        if (host == "duckduckgo.com" || host.endsWith(".duckduckgo.com") ||
            host == "bing.com" || host.endsWith(".bing.com") ||
            host == "search.brave.com" || host == "google.com" || host.endsWith(".google.com")
        ) return null
        return uri.toASCIIString()
    }

    companion object {
        private const val REQUEST_TIMEOUT_MS = 6_500L
        private const val SEARCH_TOTAL_TIMEOUT_MS = 18_000L
        private const val MAX_HTML_BYTES = 2 * 1024 * 1024
        private const val MAX_SNIPPET_CHARS = 520
        private const val CACHE_TTL_MS = 5 * 60_000L
        private const val CACHE_ENTRIES = 24
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"

        private data class Cached(val at: Long, val hits: List<SearchHit>)
        private val cache = object : LinkedHashMap<String, Cached>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Cached>): Boolean =
                size > CACHE_ENTRIES
        }

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .callTimeout(8, TimeUnit.SECONDS)
            .followRedirects(true)
            .retryOnConnectionFailure(false)
            .build()

        private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

        private fun normalizeEngine(value: String): String = when (value.trim().lowercase()) {
            "ddg", "duckduckgo", "duck duck go" -> "duckduckgo"
            "bing" -> "bing"
            "google" -> "google"
            else -> "brave"
        }

        private fun cached(key: String): List<SearchHit>? = synchronized(cache) {
            val item = cache[key] ?: return@synchronized null
            if (System.currentTimeMillis() - item.at > CACHE_TTL_MS) {
                cache.remove(key)
                null
            } else item.hits
        }

        private fun cache(key: String, hits: List<SearchHit>) = synchronized(cache) {
            cache[key] = Cached(System.currentTimeMillis(), hits.toList())
        }

        internal fun canonicalUrl(raw: String): String = canonicalResearchUrl(raw)
    }
}

/** Rejects non-web URLs, credentials, local hostnames and literal private IPs before extraction. */
internal fun isSafePublicWebUrl(raw: String): Boolean {
    val uri = runCatching { URI(raw.trim()) }.getOrNull() ?: return false
    if (!uri.scheme.equals("http", true) && !uri.scheme.equals("https", true)) return false
    if (uri.rawUserInfo != null) return false
    val host = uri.host?.lowercase()?.trimEnd('.') ?: return false
    if (host.isBlank() || host == "localhost" || host.endsWith(".localhost") ||
        host.endsWith(".local") || host.endsWith(".internal") || host.endsWith(".test")
    ) return false

    val bracketless = host.removePrefix("[").removeSuffix("]")
    val isIpLiteral = bracketless.contains(':') ||
        bracketless.matches(Regex("""\d{1,3}(?:\.\d{1,3}){3}"""))
    if (isIpLiteral) {
        val address = runCatching { InetAddress.getByName(bracketless) }.getOrNull() ?: return false
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress
        ) return false
    }
    return true
}

/** Stable identity for a source URL: drops fragments and common tracking parameters. */
internal fun canonicalResearchUrl(raw: String): String {
    val uri = runCatching { URI(raw.trim()) }.getOrNull() ?: return raw.trim().lowercase().trimEnd('/')
    val base = uri.toASCIIString().substringBefore('#')
    val query = uri.rawQuery ?: return base.trimEnd('/')
    val kept = query.split('&').filter { pair ->
        val key = pair.substringBefore('=').lowercase()
        !(key.startsWith("utm_") || key in setOf("fbclid", "gclid", "mc_cid", "mc_eid", "ref", "source"))
    }
    val path = base.substringBefore('?')
    return if (kept.isEmpty()) path.trimEnd('/') else (path + "?" + kept.joinToString("&")).trimEnd('/')
}
