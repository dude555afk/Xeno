package com.dude555afk.xeno.core.fetch

import android.content.Context
import com.dude555afk.xeno.core.model.AnswerSource
import com.dude555afk.xeno.core.model.ExtractedContent
import com.dude555afk.xeno.core.model.SearchHit
import com.dude555afk.xeno.core.reader.ReadabilityInjector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Fetches and reads the top search hits concurrently.
 *
 * The concurrency cap is not a style choice. Each extraction spins up a real
 * WebView (~150-250 MB), so four in flight can briefly hold a gigabyte. The
 * cap is 3 by default and drops to 1 on a low-memory device — checked against
 * [android.app.ActivityManager] rather than assumed, since the user's phone is
 * the only device that actually matters.
 */
class PageFetcher(
    private val context: Context,
    private val reader: ReadabilityInjector
) {

    suspend fun fetch(sources: List<AnswerSource>, maxSources: Int = 4): List<AnswerSource> =
        withContext(Dispatchers.Main) {
            val gate = Semaphore(effectiveConcurrency())

            coroutineScope {
                sources.take(maxSources).map { source ->
                    async {
                        gate.withPermit {
                            val webView = createWebView(source.url)
                            try {
                                val extracted = reader.extract(
                                    webView = webView,
                                    url = source.url,
                                    timeoutMs = 20_000L
                                )
                                val usable = if (extracted.isJunk) null else extracted
                                source.copy(
                                    fetched = true,
                                    extracted = usable,
                                    title = if (source.title.isBlank() && usable != null) usable.title else source.title
                                )
                            } finally {
                                runCatching { webView.destroy() }
                            }
                        }
                    }
                }.awaitAll()
            }
        }

    private fun createWebView(url: String): android.webkit.WebView =
        android.webkit.WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.blockNetworkImage = true
            settings.loadsImagesAutomatically = false
        }

    /**
     * Decide how many pages to read at once based on actual free memory, not a
     * guess. Phones report this reliably.
     */
    private fun effectiveConcurrency(): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            ?: return 3
        val info = android.app.ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)

        return when {
            info.lowMemory -> 1
            info.availMem < LOW_MEM_THRESHOLD -> 1
            info.totalMem < 4L * 1024 * 1024 * 1024 -> 2
            else -> 3
        }
    }

    private companion object {
        const val LOW_MEM_THRESHOLD = 600L * 1024 * 1024
    }
}
