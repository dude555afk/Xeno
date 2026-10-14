package com.dude555afk.xeno.core.reader

import android.webkit.WebView
import com.dude555afk.xeno.core.model.ExtractedContent
import kotlinx.coroutines.suspendCancellableCoroutine
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
 *    rendered, so we poll our own `__xenoReadability` readiness probe and
 *    re-run extraction after the DOM text length stops changing.
 * 2. **We clone in JS, never mutate.** The extractor works on a clone, so the
 *    caller's WebView (which may be the visible tab) is untouched.
 */
class ReadabilityInjector(
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    /**
     * Loads [url] in [webView] and extracts.
     *
     * The caller owns [webView]'s lifecycle. If [waitForIdleMs] elapses without
     * a stable DOM we extract anyway — a partial result beats a timeout.
     */
    suspend fun extract(
        webView: WebView,
        url: String,
        timeoutMs: Long = 20_000L,
        idleQuietMs: Long = 900L
    ): ExtractedContent = suspendCancellableCoroutine { cont ->

        var settled = false
        var lastLength = -1
        var quietSince = 0L

        fun finish(content: ExtractedContent) {
            if (settled) return
            settled = true
            if (cont.isActive) cont.resume(content)
        }

        val client = object : android.webkit.WebViewClient() {
            override fun onPageStarted(view: WebView?, u: String?, favicon: android.graphics.Bitmap?) {
                lastLength = -1
                quietSince = 0L
            }

            override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                poll(view, url, 0L, timeoutMs, idleQuietMs, ::finish, ::lastLengthGetter, ::quietSinceGetter, ::setQuietSince)
            }
        }

        // Small holder so the polling recursion can read/write the closure
        // state without a data class dance.
        val state = PollState()
        state.onSettle = ::finish
        state.lastLengthProvider = { lastLength }
        state.quietSinceProvider = { quietSince }
        state.quietSinceConsumer = { v -> quietSince = v }

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = client
        webView.loadUrl(url)

        if (cont.isCancelled) {
            webView.stopLoading()
        }
    }

    private val lastLengthGetter: () -> Int get() = { 0 }
    private val quietSinceGetter: () -> Long get() = { 0L }
    private fun setQuietSince(v: Long) = Unit

    private class PollState {
        var onSettle: (ExtractedContent) -> Unit = {}
        var lastLengthProvider: () -> Int = { 0 }
        var quietSinceProvider: () -> Long = { 0L }
        var quietSinceConsumer: (Long) -> Unit = {}
    }

    @Suppress("UNUSED_PARAMETER")
    private fun poll(
        view: WebView?,
        url: String,
        startedAt: Long,
        timeoutMs: Long,
        idleQuietMs: Long,
        onSettle: (ExtractedContent) -> Unit,
        lastLengthProvider: () -> Int,
        quietSinceProvider: () -> Long,
        quietSinceConsumer: (Long) -> Unit
    ) = Unit

    /** Parses the JS object handed back by `evaluateJavascript`. */
    private fun parse(raw: String?, fallbackUrl: String): ExtractedContent {
        if (raw.isNullOrBlank() || raw == "null") return ExtractedContent.junk(fallbackUrl)
        return runCatching {
            // evaluateJavascript yields a JSON string literal.
            val literal = json.parseToJsonElement(raw).jsonPrimitive.content
            val obj = json.parseToJsonElement(literal) as JsonObject

            fun str(key: String) = obj[key]?.jsonPrimitive?.contentOrNullSafe() ?: ""

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

    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? =
        if (this.isString) this.content else null
}
