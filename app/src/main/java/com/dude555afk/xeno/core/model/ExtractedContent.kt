package com.dude555afk.xeno.core.model

/**
 * Clean text pulled out of a page by the Readability injector.
 *
 * This is the currency of the whole app — both the summarizer and the answer
 * engine consume it, which is exactly why `core/reader` had to land before
 * `core/ai`.
 */
data class ExtractedContent(
    val url: String,
    val title: String,
    val byline: String? = null,
    val text: String = "",
    val excerpt: String = "",
    val wordCount: Int = 0
) {
    /**
     * True when extraction looks useless — a cookie wall, a JS shell, a
     * paywall stub. The AI layer must degrade rather than summarise noise.
     */
    val isJunk: Boolean
        get() = wordCount < 80 || text.length < 400

    companion object {
        fun junk(url: String, title: String = "") = ExtractedContent(
            url = url,
            title = title,
            text = "",
            wordCount = 0
        )
    }
}
