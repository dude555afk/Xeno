package com.dude555afk.xeno.core.summarize

/**
 * Every prompt in the app lives here.
 *
 * A lazy `"summarize this: {text}"` produces three sentences of mush and gets
 * the app uninstalled. Every prompt below forces structure so the output
 * actually scans. See docs/PROMPTS.md for the rationale.
 */
object Prompts {

    val SUMMARIZER_SYSTEM = """
        You are a precision summarizer. Extract signal, never pad.
    """.trimIndent()

    /**
     * The main summarizer prompt. `TL;DR` / `KEY POINTS` / `NUMBERS`.
     *
     * `NUMBERS` is the section people don't expect and the one that makes it
     * feel like a tool rather than a toy.
     */
    fun summarizePage(text: String): String = """
        Summarize this page. Respond in EXACTLY this shape:

        TL;DR — one punchy sentence, max 25 words.

        KEY POINTS
        • (3–5 bullets, each one standalone fact, no fluff)

        NUMBERS (only if real data exists)
        • (prices, dates, stats — omit this section entirely if there are none)

        Skip: navigation, cookie notices, promo text, "click here", filler.
        If the page is junk, spam, or paywalled, say so in one line.

        <page>
        $text
        </page>
    """.trimIndent()

    /** Tone variants — same text, different system instruction. */
    enum class Tone(val label: String, val system: String) {
        Terse("TL;DR", "Summarize as tersely as possible."),
        Simplistic("Simplify", "Explain like I'm five. Short words, no jargon."),
        Actionable("Action items", "Extract everything actionable as a checklist."),
        Contrarian("Contrarian", "Give the strongest case that the page is wrong or misleading.");

        companion object {
            fun fromLabel(label: String): Tone = entries.firstOrNull { it.label == label } ?: Terse
        }
    }

    /**
     * Answer engine. The `[1][2]` citation rule is the entire trust loop —
     * users verify claims against the source cards, which is why they trust it.
     */
    fun answerWithSources(query: String, sources: List<IndexedSourceText>): String {
        val sourceBlock = sources.joinToString("\n\n") { s ->
            "[${s.index}] ${s.title} — ${s.url}\n${s.text}"
        }
        return """
            <rules>
            - cite every factual claim inline like [1][3]
            - if sources disagree, say so explicitly
            - if the sources don't answer it, say "not enough in sources" — never guess
            - max 3 short paragraphs, then a one-line TL;DR
            - ignore navigation, promo, and cookie text from fetched pages
            </rules>

            <sources>
            $sourceBlock
            </sources>

            <question>$query</question>
        """.trimIndent()
    }

    const val ANSWER_SYSTEM = "You answer questions using ONLY the sources provided."

    /** Chunk pass for the map-reduce summary of long pages. */
    fun chunkPass(chunk: String, chunkIndex: Int, totalChunks: Int): String = """
        This is part $chunkIndex of $totalChunks of one long page. Summarize just
        this part in at most 120 words, as facts rather than prose.

        <chunk>
        $chunk
        </chunk>
    """.trimIndent()

    /** Final pass over chunk summaries. */
    fun reducePass(chunkSummaries: List<String>): String = """
        These are summaries of consecutive parts of one page. Merge them into one
        coherent summary, dropping repetition, following the output shape above.

        <chunk-summaries>
        ${chunkSummaries.joinToString("\n\n") { "- $it" }}
        </chunk-summaries>
    """.trimIndent()

    data class IndexedSourceText(
        val index: Int,
        val url: String,
        val title: String,
        val text: String
    )
}
