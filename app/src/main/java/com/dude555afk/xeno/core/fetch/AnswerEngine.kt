package com.dude555afk.xeno.core.fetch

import com.dude555afk.xeno.core.ai.AiClient
import com.dude555afk.xeno.core.ai.ChatMessage
import com.dude555afk.xeno.core.ai.ChatRequest
import com.dude555afk.xeno.core.ai.ProviderConfig
import com.dude555afk.xeno.core.model.AnswerResult
import com.dude555afk.xeno.core.model.AnswerSource
import com.dude555afk.xeno.core.model.Citation
import com.dude555afk.xeno.core.summarize.Prompts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * The flagship pipeline: query → search → read → synthesize with citations.
 *
 * Everything before this exists to feed it. The output ordering matters as much
 * as the synthesis: source cards render *before* the answer streams, because
 * fetching takes ~1-2s and generation takes ~4s. Cards first makes the wait
 * feel like progress instead of a hang.
 */
class AnswerEngine(
    private val search: SearchEngine,
    private val fetcher: PageFetcher,
    private val ai: AiClient
) {

    /** A normalised query — drops the URL-shaped things the palette might pass. */
    fun isQuestion(raw: String): Boolean {
        val q = raw.trim()
        if (q.isEmpty()) return false

        // Explicit escape hatch beats heuristics.
        if (q.startsWith("?") || q.startsWith("ask:")) return true

        val looksLikeUrl = q.matches(
            Regex("^(https?://)?[\\w.-]+\\.[a-z]{2,}(/.*)?$", RegexOption.IGNORE_CASE)
        ) && !q.contains(' ')
        if (looksLikeUrl) return false

        val words = q.split(' ').size
        val lowercase = q.lowercase()
        return words >= 2 && (
            lowercase.startsWith("what") || lowercase.startsWith("who") ||
                lowercase.startsWith("when") || lowercase.startsWith("where") ||
                lowercase.startsWith("why") || lowercase.startsWith("how") ||
                lowercase.startsWith("is ") || lowercase.startsWith("are ") ||
                lowercase.startsWith("can ") || lowercase.startsWith("does ") ||
                lowercase.startsWith("vs") || q.endsWith("?")
            )
    }

    /**
     * Runs the full pipeline.
     *
     * Emits intermediate [AnswerResult]s so the UI can render progress:
     * sources appear first, then streamed answer text, then citations parsed.
     */
    fun answer(query: String, config: ProviderConfig): Flow<AnswerResult> = flow {
        emit(AnswerResult(query, emptyList())) // "finding sources…"

        val hits = search.search(query)
        if (hits.isEmpty()) {
            emit(AnswerResult(query, emptyList(), markdown = NO_SOURCES))
            return@flow
        }

        val sources = hits.mapIndexed { i, hit ->
            AnswerSource(index = i + 1, url = hit.url, title = hit.title, snippet = hit.snippet)
        }

        // Source cards land here, before any answer text.
        emit(AnswerResult(query, sources))

        val read = fetcher.fetch(sources, maxSources = MAX_SOURCES)
        val hitByIndex = hits.mapIndexed { index, hit -> (index + 1) to hit }.toMap()
        val displaySources = read

        emit(AnswerResult(query, displaySources))

        // Prefer extracted article text. For any page that fails individually,
        // retain the search engine's snippet as limited evidence. This keeps a
        // single broken site from suppressing useful text from other sources.
        // Snippets are labelled as previews; paywalls are never bypassed.
        val promptSources = displaySources.mapNotNull { source ->
            val content = source.extracted
            if (content != null && !content.isJunk) {
                Prompts.IndexedSourceText(
                    index = source.index,
                    url = source.url,
                    title = source.title.ifBlank { content.title },
                    text = content.text.take(CHARS_PER_SOURCE)
                )
            } else {
                val hit = hitByIndex[source.index] ?: return@mapNotNull null
                if (hit.snippet.isBlank()) return@mapNotNull null
                Prompts.IndexedSourceText(
                    index = source.index,
                    url = source.url,
                    title = source.title.ifBlank { hit.title },
                    text = "Search-result snippet only; full page could not be extracted. " + hit.snippet.take(600)
                )
            }
        }

        if (promptSources.isEmpty()) {
            emit(AnswerResult(query, read, markdown = UNREADABLE))
            return@flow
        }

        if (config.apiKey.isBlank() && !config.allowsAnonymousFreeModel) {
            // No key configured — hand back the sources rather than nothing.
            emit(AnswerResult(query, displaySources, markdown = NO_KEY))
            return@flow
        }

        val request = ChatRequest(
            model = config.model,
            messages = listOf(
                ChatMessage.system(Prompts.ANSWER_SYSTEM),
                ChatMessage.user(Prompts.answerWithSources(query, promptSources))
            ),
            stream = true,
            temperature = 0.2
        )

        val buffer = StringBuilder()
        var charsSinceEmission = 0
        ai.stream(config, request).collect { delta ->
            buffer.append(delta)
            charsSinceEmission += delta.length
            // Emit often enough to feel live, but don't reparse the entire
            // Markdown response on every tiny SSE token.
            if (charsSinceEmission >= UI_EMIT_CHAR_BATCH) {
                val markdown = buffer.toString()
                emit(
                    AnswerResult(
                        query = query,
                        sources = displaySources,
                        markdown = markdown,
                        citations = parseCitations(markdown)
                    )
                )
                charsSinceEmission = 0
            }
        }
        if (charsSinceEmission > 0) {
            val markdown = buffer.toString()
            emit(
                AnswerResult(
                    query = query,
                    sources = displaySources,
                    markdown = markdown,
                    citations = parseCitations(markdown)
                )
            )
        }
    }.flowOn(Dispatchers.Default)

    companion object {
        private const val UI_EMIT_CHAR_BATCH = 28
        private const val MAX_SOURCES = 4
        private const val CHARS_PER_SOURCE = 2_000

        const val NO_SOURCES = "No sources found for that query."
        const val UNREADABLE = "Found sources but couldn't read any of them — they may be paywalled or JavaScript-only."
        const val NO_KEY = "Add an AI provider key in settings to get a synthesized answer."

        /** Pulls `[1][2][12]` markers out of streamed text. */
        fun parseCitations(markdown: String): List<Citation> {
            if (markdown.isBlank()) return emptyList()
            return Regex("\\[(\\d{1,2})]").findAll(markdown)
                .mapNotNull { m -> m.groupValues[1].toIntOrNull()?.let(::Citation) }
                .distinctBy { it.sourceIndex }
                .toList()
        }
    }
}
