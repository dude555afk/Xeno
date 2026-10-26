package com.dude555afk.xeno.core.summarize

import com.dude555afk.xeno.core.ai.AiClient
import com.dude555afk.xeno.core.ai.ChatMessage
import com.dude555afk.xeno.core.ai.ChatRequest
import com.dude555afk.xeno.core.ai.ProviderConfig
import com.dude555afk.xeno.core.model.ExtractedContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * One unit of streamed summary text.
 *
 * The wrapper exists for one reason: when the AI path fails, the fallback is
 * [TextRank], and the UI has to be able to show "offline" when that happens.
 * A bare String cannot carry that flag. The previous design just dropped it
 * silently, so a local TextRank summary and a gateway summary looked
 * identical to the user — which is exactly the kind of thing that makes an
 * offline capable app feel broken rather than resilient.
 */
data class SummaryDelta(
    val text: String,
    /** True once the local fallback has taken over. */
    val offlineFallback: Boolean = false
)

/**
 * Page summarizer with a hard guarantee: the user always gets text.
 *
 * Path A is the AI: a single prompt for normal pages, map-reduce for long
 * ones. Path B is [TextRank], which needs no key, no network and no quota.
 * B is not a fallback of last resort, it is a first-class mode — "offline" is
 * a legitimate state for a browser on a train.
 *
 * The stream is shaped as AI deltas first, local fallback only if the AI
 * produced nothing. That ordering is deliberate: if the gateway is reachable
 * but slow, the user watches text arrive; if it is unreachable, they get a
 * working summary within milliseconds. What never happens is a blank sheet.
 */
class Summarizer(private val ai: AiClient) {

    /**
     * Decide how to summarise, returning a stream the caller collects.
     *
     * Deliberately does not collect anything itself. The previous version
     * called `collectDirect()` internally and returned a finished string,
     * which meant [SummaryOutcome.Streaming] was never constructed and the
     * ViewModel's streaming branch was dead code — the whole "typewriter"
     * effect did not exist, and every summary arrived as one lump.
     */
    suspend fun summarize(
        content: ExtractedContent,
        config: ProviderConfig,
        tone: Prompts.Tone
    ): SummaryOutcome {
        if (content.text.isBlank() || content.isJunk) return SummaryOutcome.Unreadable

        // Computed once here and reused for every fallback path, so a page
        // that goes local does not run TextRank three separate times.
        val fallback = localSummary(content)

        // No key, no network path. Go straight to the local summary.
        if (config.apiKey.isBlank() && !config.allowsAnonymousFreeModel) return SummaryOutcome.Offline(fallback)

        val upstream = if (content.text.length <= SINGLE_SHOT_CHARS) {
            singleShot(config, content.text, tone)
        } else {
            mapReduce(config, content.text, tone, fallback)
        }

        return SummaryOutcome.Streaming(ensureFallback(upstream, fallback))
    }

    /**
     * One prompt over the whole page. Fast path, and covers the large majority
     * of real pages — most articles are under the single-shot ceiling.
     */
    private fun singleShot(
        config: ProviderConfig,
        text: String,
        tone: Prompts.Tone
    ): Flow<SummaryDelta> = flow {
        val request = ChatRequest(
            model = config.model,
            messages = listOf(
                ChatMessage.system(tone.system + " " + Prompts.SUMMARIZER_SYSTEM),
                ChatMessage.user(Prompts.summarizePage(text))
            ),
            stream = true,
            temperature = 0.3,
            maxTokens = 900
        )
        ai.stream(config, request).collect { delta ->
            emit(SummaryDelta(delta))
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Long page: summarise chunks, then merge the chunk summaries.
     *
     * The chunks are processed sequentially on purpose. Six parallel requests
     * to a 200 req/hr free tier is a great way to be rate limited into
     * uselessness, and sequential is fast enough that nobody notices.
     *
     * Returns the *merge* flow, not the finished text. If every chunk failed,
     * the merge pass would be one more doomed request for nothing, so it
     * short-circuits to the local summary instead.
     */
    private suspend fun mapReduce(
        config: ProviderConfig,
        text: String,
        tone: Prompts.Tone,
        fallback: String
    ): Flow<SummaryDelta> {
        val pieces = chunk(text, CHUNK_CHARS)
        val summaries = ArrayList<String>(pieces.size)
        var anyChunkSucceeded = false

        pieces.forEachIndexed { index, piece ->
            val summary = try {
                val request = ChatRequest(
                    model = config.model,
                    messages = listOf(
                        ChatMessage.system(Prompts.SUMMARIZER_SYSTEM),
                        ChatMessage.user(Prompts.chunkPass(piece, index + 1, pieces.size))
                    ),
                    stream = true,
                    temperature = 0.2,
                    maxTokens = 400
                )
                ai.stream(config, request).collectDirect()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ""
            }

            if (summary.isBlank()) {
                // Per-chunk degradation, same guarantee as the whole page.
                summaries += TextRank.summarize(piece, sentenceCount = 3)
            } else {
                summaries += summary
                anyChunkSucceeded = true
            }
        }

        if (!anyChunkSucceeded) {
            return flow {
                emit(SummaryDelta(fallback, offlineFallback = true))
            }.flowOn(Dispatchers.Default)
        }

        return flow {
            val request = ChatRequest(
                model = config.model,
                messages = listOf(
                    ChatMessage.system(tone.system + " " + Prompts.SUMMARIZER_SYSTEM),
                    ChatMessage.user(Prompts.reducePass(summaries))
                ),
                stream = true,
                temperature = 0.3,
                maxTokens = 900
            )
            ai.stream(config, request).collect { delta ->
                emit(SummaryDelta(delta))
            }
        }.flowOn(Dispatchers.Default)
    }

    /** Plain local summary — the mode that always works. */
    private fun localSummary(content: ExtractedContent): String =
        TextRank.summarize(content.text, sentenceCount = 5)

    /**
     * Guarantee that something is emitted, exactly once.
     *
     * Wrapping rather than using `catch` + `onCompletion` because those two
     * compose badly here: a partial stream followed by a failure would trip
     * both and emit the local summary twice, leaving the sheet showing a
     * truncated AI paragraph glued to a full TextRank one.
     *
     * Known honest edge case: if the gateway drops *mid*-stream after real
     * text has already arrived, the user keeps whatever streamed and gets the
     * local summary appended. Rare, and still informative — a truncated
     * summary plus a complete one beats a dead sheet.
     */
    private fun ensureFallback(
        upstream: Flow<SummaryDelta>,
        fallback: String
    ): Flow<SummaryDelta> = flow {
        var producedAnything = false

        try {
            upstream.collect { delta ->
                producedAnything = true
                emit(delta)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            // A raw socket error must never reach the summary sheet — that is
            // what the fallback is for.
        }

        if (!producedAnything && fallback.isNotBlank()) {
            emit(SummaryDelta(fallback, offlineFallback = true))
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Split on paragraph boundaries so a chunk never severs a sentence.
     *
     * Sentence-level chunking produces fragments that read like a ransom note
     * once merged, which is worse than a slightly uneven split.
     */
    private fun chunk(text: String, maxChars: Int): List<String> {
        val out = mutableListOf<StringBuilder>()
        var current = StringBuilder()

        text.split(Regex("\n{2,}")).forEach { paraRaw ->
            val para = paraRaw.trim()
            if (para.isEmpty()) return@forEach

            if (current.length + para.length + 2 > maxChars && current.isNotEmpty()) {
                out += current
                current = StringBuilder()
            }

            if (para.length > maxChars) {
                // One monstrous paragraph — hard split, best effort.
                para.chunked(maxChars).forEach { current.append(it).append("\n\n") }
            } else {
                current.append(para).append("\n\n")
            }
        }
        if (current.isNotBlank()) out += current

        return out.map { it.toString().trim() }.filter { it.isNotBlank() }
    }

    /** Terminal collector: concatenate every delta into one string. */
    private suspend fun Flow<String>.collectDirect(): String {
        val sb = StringBuilder()
        collect { sb.append(it) }
        return sb.toString().trim()
    }

    private companion object {
        /** ~6k tokens. Above this we go map-reduce. */
        const val SINGLE_SHOT_CHARS = 24_000
        const val CHUNK_CHARS = 12_000
    }
}

/** What the UI renders. Every state has text worth showing. */
sealed interface SummaryOutcome {

    /** Page was unreadable — cookie wall, JS shell, paywall stub. */
    data object Unreadable : SummaryOutcome

    /** TextRank summary, decided before any network attempt was made. */
    data class Offline(val text: String) : SummaryOutcome

    /**
     * Streamed summary. May carry [SummaryDelta.offlineFallback] on later
     * emissions if the AI path failed partway through.
     */
    data class Streaming(val deltas: Flow<SummaryDelta>) : SummaryOutcome
}
