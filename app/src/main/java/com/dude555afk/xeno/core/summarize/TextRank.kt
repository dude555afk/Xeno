package com.dude555afk.xeno.core.summarize

import kotlin.math.abs

/**
 * Extractive summarizer, TextRank style. No model, no network, no quota.
 *
 * Scores sentences by graph centrality over a word-overlap similarity matrix,
 * then returns the top sentences in their original order. Roughly 50 lines and
 * it always works — which is exactly why it's the fallback whenever the AI path
 * fails.
 *
 * Output is drier than an LLM summary. That's fine: a plain, correct summary
 * beats a confident, invented one, and it beats a dead screen every time.
 */
object TextRank {

    private const val DAMPING = 0.85
    private const val ITERATIONS = 20
    private const val TOLERANCE = 0.0001

    fun summarize(text: String, sentenceCount: Int = 5): String {
        val sentences = splitSentences(text)
        if (sentences.size <= sentenceCount) return text.trim()

        val tokens = sentences.map { tokenize(it) }
        val matrix = Array(sentences.size) { DoubleArray(sentences.size) }

        for (i in sentences.indices) {
            for (j in i + 1 until sentences.size) {
                val sim = similarity(tokens[i], tokens[j])
                matrix[i][j] = sim
                matrix[j][i] = sim
            }
        }

        var scores = DoubleArray(sentences.size) { 1.0 }
        repeat(ITERATIONS) {
            val next = DoubleArray(sentences.size)
            for (i in sentences.indices) {
                var sum = 0.0
                for (j in sentences.indices) {
                    if (i == j) continue
                    val outWeight = matrix[j].sum()
                    if (outWeight > 0) sum += (matrix[j][i] / outWeight) * scores[j]
                }
                next[i] = (1 - DAMPING) + DAMPING * sum
            }
            val delta = next.indices.sumOf { abs(next[it] - scores[it]) }
            scores = next
            if (delta < TOLERANCE) return@repeat
        }

        val ranked = scores.indices.sortedByDescending { scores[it] }
        val chosen = ranked.take(sentenceCount).sorted()

        return chosen.joinToString("\n\n") { sentences[it].trim() }
    }

    fun splitSentences(text: String): List<String> {
        val cleaned = text.replace(Regex("\\s+"), " ").trim()
        if (cleaned.isEmpty()) return emptyList()

        val parts = cleaned.split(Regex("(?<=[.!?])\\s+(?=[A-Z0-9\"'])"))
        return parts
            .map { it.trim() }
            .filter { it.length > 30 }
            .take(400)
    }

    private fun tokenize(sentence: String): Set<String> =
        Regex("[a-z0-9']+", RegexOption.IGNORE_CASE)
            .findAll(sentence.lowercase())
            .map { it.value }
            .filter { it.length > 2 }
            .toSet()

    /** Normalised overlap — shared words over log-scaled lengths. */
    private fun similarity(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val shared = a.intersect(b).size
        if (shared == 0) return 0.0
        return shared / (kotlin.math.ln(1.0 + a.size) + kotlin.math.ln(1.0 + b.size))
    }
}
