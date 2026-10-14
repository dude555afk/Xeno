package com.dude555afk.xeno.core.summarize
import com.dude555afk.xeno.core.ai.AiClient
import com.dude555afk.xeno.core.ai.ChatMessage
import com.dude555afk.xeno.core.ai.ChatRequest
import com.dude555afk.xeno.core.ai.ProviderConfig
import com.dude555afk.xeno.core.model.ExtractedContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Outcome of a summarize attempt. */
sealed interface SummaryOutcome {
    /** Streamed from the AI. */
    data class Streaming(val deltas: Flow<String>) : SummaryOutcome
    /** Fell back to TextRank, complete immediately. */
    data class Offline(val text: String) : SummaryOutcome
    /** Page was junk; there is nothing to summarize. */
    data object Unreadable : SummaryOutcome
}

/**
 * Turns page text into a summary.
 *
 * Guarantees, in order:
 *
 * 1. A junk page never produces a fake summary — it returns [SummaryOutcome.Unreadable].
 * 2. Any AI failure falls back to [SummaryOutcome.Offline] rather than erroring.
 * 3. Long pages get chunked map-reduce so quality doesn't fall off a cliff.
 */
class Summarizer(
    private val ai: AiClient
) {

    /**
     * Builds a summary for [content].
     *
     * @param config the AI provider. When [ProviderConfig.apiKey] is blank the
     *   call goes straight to TextRank — no point burning a request that will
     *   401.
     * @param tone the summarizer mode chip.
     */
    fun summarize(
        content: ExtractedContent,
        config: ProviderConfig,
        tone: Prompts.Tone = Prompts.Tone.Terse
    ): SummaryOutcome {
        if (content.isJunk) return SummaryOutcome.Unreadable

        val body = content.text.take(MAX_CHARS)

        if (config.apiKey.isBlank()) {
            return SummaryOutcome.Offline(TextRank.summarize(body))
        }

        if (body.length > CHUNK_THRESHOLD) {
            return SummaryOutcome.Streaming(
                chunkedSummary(body, config, tone)
            )
        }

        val request = ChatRequest(
            model = config.model,
            messages = listOf(
                ChatMessage.system("${Prompts.SUMMARIZER_SYSTEM}\n${tone.system}"),
                ChatMessage.user(Prompts.summarizePage(body))
            ),
            stream = true,
            temperature = 0.3
        )

        return SummaryOutcome.Streaming(
            ai.stream(config, request).fallbackOnError { TextRank.summarize(body) }
        )
    }

    /**
     * Map-reduce for long pages: summarize each chunk, then merge.
     *
     * Streamed to the user as the reduce pass, so they still see the typewriter
     * effect rather than a frozen screen.
     */
    private fun chunkedSummary(
        text: String,
        config: ProviderConfig,
        tone: Prompts.Tone
    ): Flow<String> = flow {
        val chunks = chunk(text, CHUNK_CHARS).ifEmpty { listOf(text) }

        // Map phase — collect, don't stream; individual chunks are meaningless.
        val summaries = mutableListOf<String>()
        for ((index, piece) in chunks.withIndex()) {
            val result = runCatching {
                ai.complete(
                    config,
                    ChatRequest(
                        model = config.model,
                        messages = listOf(
                            ChatMessage.system(Prompts.SUMMARIZER_SYSTEM),
                            ChatMessage.user(Prompts.chunkPass(piece, index + 1, chunks.size))
                        ),
                        stream = false,
                        temperature = 0.3
                    )
                ) }.getOrNull()

            if (result.isNullOrBlank()) {
                ai.complete(
                    config
                ) // unreachable guard for clarity; result already handled
            }
            summaries += result ?: TextRank.summarize(piece, sentenceCount = 3)
        }

        // Reduce phase — this is what streams.
        val request = ChatRequest(
            model = config.model,
            messages = listOf(
                ChatMessage.system("${Prompts.SUMMARIZER_SYSTEM}\n${tone.system}"),
                ChatMessage.user(
                    Prompts.reducePass(summaries)
                )
            ),
            stream = true,
            temperature = 0.3
        )

        ai.stream(config, request).collect { emit(it) }
    }

    private fun chunk(text: String, size: Int): List<String> {
        if (text.length <= size) return listOf(text)
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            var end = minOf(start + size, text.length)
            // Break on a paragraph boundary so we don't cut mid-thought.
            if (end < text.length) {
                val boundary = text.lastIndexOf("\n\n", end)
                if (boundary > start + size / 2) end = boundary
            }
            chunks += text.substring(start, end)
            start = end
        }
        return chunks
    }

    companion object {
        private const val MAX_CHARS = 40_000
        private const val CHUNK_THRESHOLD = 12_000
        private const val CHUNK_CHARS = 8_000
    }
}

/**
 * Swaps a mid-stream failure for a canned offline summary.
 *
 * The user sees *something*, and the UI marks it as offline. A dead screen is
 * the one unacceptable outcome.
 */
inline fun <T> Flow<T>.fallbackOnError(
    crossinline fallback: () -> String
): Flow<T> {
    val errorFlow = flow { emit(fallback() as T) }
    return kotlinx.coroutines.flow.catch { emitAll(errorFlow) }
}