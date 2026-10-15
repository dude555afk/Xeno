package com.dude555afk.xeno.core.summarize

import com.dude555afk.xeno.core.ai.AiClient
import com.dude555afk.xeno.core.ai.ChatMessage
import com.dude555afk.xeno.core.ai.ChatRequest
import com.dude555afk.xeno.core.ai.ProviderConfig
import com.dude555afk.xeno.core.model.ExtractedContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Page summarizer with a hard guarantee: the user always gets text.
 *
 * Path A is the AI: map-reduce over the page, chunk by chunk, then a merge
 * pass. Path B is [TextRank], which needs no key, no network and no quota.
 * B is not a fallback of last resort, it is a first-class mode — "offline" is
 * a legitimate state for a browser on a train.
 */
class Summarizer(private val ai: AiClient) {

    suspend fun summarize(
        content: ExtractedContent,
        config: ProviderConfig,
        tone: Prompts.Tone
    ): SummaryOutcome {
        if (content.text.isBlank() || content.isJunk) return SummaryOutcome.Unreadable

        // No key, no network path. Go straight to the local summary.
        if (config.apiKey.isBlank()) return SummaryOutcome.Offline(localSummary(content))

        return runCatching {
            val text = if (content.text.length <= SINGLE_SHOT_CHARS) {
                streamOnce(config, content.text, tone).collectDirect()
            } else {
                mapReduce(config, content.text, tone)
            }

            if (text.isBlank()) SummaryOutcome.Offline(localSummary(content))
            else SummaryOutcome.Offline(text)
        }.getOrElse {
            // Never surface a raw network error as the summary.
            SummaryOutcome.Offline(localSummary(content))
        }
    }

    /**
     * One prompt over the whole page. Fast path, and covers the large majority
     * of real pages — most articles are under the single-shot ceiling.
     */
    private fun streamOnce(
        config: ProviderConfig,
        text: String,
        tone: Prompts.Tone
    ): Flow<String> = flow {
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
        ai.stream(config, request).collect { emit(it) }
    }.flowOn(Dispatchers.Default)

    /**
     * Long page: summarise chunks, then merge the chunk summaries.
     *
     * The chunks are processed sequentially on purpose. Six parallel requests
     * to a 200 req/hr free tier is a great way to be rate limited into
     * uselessness, and sequential is fast enough that nobody notices.
     */
    private suspend fun mapReduce(
        config: ProviderConfig,
        text: String,
        tone: Prompts.Tone
    ): String = withContext(Dispatchers.Default) {
        val pieces = chunk(text, CHUNK_CHARS)

        val summaries = pieces.mapIndexed { index, piece ->
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
            ai.stream(config, request).collectDirect().ifBlank {
                TextRank.summarize(piece, sentenceCount = 3)
            }
        }

        if (summaries.isEmpty()) return@withContext ""

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
        ai.stream(config, request).collectDirect()
    }

    /** Plain local summary — the mode that always works. */
    private fun localSummary(content: ExtractedContent): String =
        TextRank.summarize(content.text, sentenceCount = 5)

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

    /** TextRank summary, no network involved. */
    data class Offline(val text: String) : SummaryOutcome

    /** Streamed AI summary. */
    data class Streaming(val deltas: Flow<String>) : SummaryOutcome
}
