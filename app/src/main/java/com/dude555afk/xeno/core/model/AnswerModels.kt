package com.dude555afk.xeno.core.model

/**
 * A source backing an answer. Cards for these render *before* the answer text
 * because fetching takes ~1-2s and generation takes ~4s — the cards make the
 * wait feel productive.
 */
data class AnswerSource(
    val index: Int,
    val url: String,
    val title: String,
    val snippet: String = "",
    val fetched: Boolean = false,
    val extracted: ExtractedContent? = null
) {
    val host: String
        get() = runCatching { java.net.URI(url).host ?: url }.getOrDefault(url)
}

/** An anchored citation as parsed out of streamed text, e.g. `[1][3]`. */
data class Citation(val sourceIndex: Int)

/**
 * The full answer-engine result. `markdown` is what streams; `citations` is the
 * parsed index list for tap-through.
 */
data class AnswerResult(
    val query: String,
    val sources: List<AnswerSource>,
    val markdown: String = "",
    val citations: List<Citation> = emptyList(),
    val fromCache: Boolean = false
) {
    val complete: Boolean
        get() = markdown.isNotBlank()

    /** Look up a citation index back to its source card. */
    fun sourceFor(index: Int): AnswerSource? = sources.firstOrNull { it.index == index }
}
