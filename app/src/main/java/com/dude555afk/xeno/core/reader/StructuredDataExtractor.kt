package com.dude555afk.xeno.core.reader

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Extracts only declarative JSON-LD text already embedded in a page.
 *
 * Inspired by AndroidHarness' StructuredPageText approach. We do not evaluate
 * scripts, follow embedded URLs, or treat structured data as executable code.
 * Output is deliberately bounded and marked as structured page-supplied text.
 */
internal object StructuredDataExtractor {
    private val json = Json { ignoreUnknownKeys = true }
    private val supportedTypes = setOf(
        "Article", "NewsArticle", "BlogPosting", "DiscussionForumPosting",
        "SocialMediaPosting", "Comment", "Question", "Answer"
    )
    private const val MAX_TOTAL_CHARS = 80_000
    private const val MAX_NODE_CHARS = 12_000
    private const val MAX_NODES = 350
    private const val MAX_DEPTH = 10

    fun extract(rawScripts: List<String>): String? {
        if (rawScripts.isEmpty()) return null
        val sections = LinkedHashSet<String>()
        var visited = 0
        var collectedChars = 0

        fun field(obj: JsonObject, key: String): String =
            (obj[key] as? JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty().trim()

        fun clean(raw: String): String = raw
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ", ignoreCase = true)
            .replace("&amp;", "&", ignoreCase = true)
            .replace("&lt;", "<", ignoreCase = true)
            .replace("&gt;", ">", ignoreCase = true)
            .replace(Regex("\\s+"), " ")
            .trim()

        fun walk(element: JsonElement?, depth: Int) {
            if (element == null || depth > MAX_DEPTH || visited >= MAX_NODES ||
                collectedChars >= MAX_TOTAL_CHARS
            ) return
            visited++

            when (element) {
                is JsonArray -> element.take(60).forEach { walk(it, depth + 1) }
                is JsonObject -> {
                    val type = when (val raw = element["@type"]) {
                        is JsonPrimitive -> listOf(raw.content)
                        is JsonArray -> raw.mapNotNull { (it as? JsonPrimitive)?.content }
                        else -> emptyList()
                    }.map { it.substringAfterLast('/').substringAfterLast('#') }

                    if (type.any { it in supportedTypes }) {
                        val title = clean(field(element, "headline").ifBlank { field(element, "name") })
                        val author = when (val rawAuthor = element["author"]) {
                            is JsonObject -> clean(field(rawAuthor, "name"))
                            is JsonPrimitive -> clean(rawAuthor.content)
                            is JsonArray -> rawAuthor.mapNotNull { item ->
                                when (item) {
                                    is JsonObject -> field(item, "name").takeIf { it.isNotBlank() }
                                    is JsonPrimitive -> item.content.takeIf { it.isNotBlank() }
                                    else -> null
                                }
                            }.distinct().joinToString(", ")
                            else -> ""
                        }
                        val body = clean(
                            field(element, "articleBody")
                                .ifBlank { field(element, "text") }
                                .ifBlank { field(element, "description") }
                        )
                        if (body.length >= 80 && !body.equals("[deleted]", true) &&
                            !body.equals("[removed]", true)
                        ) {
                            val block = buildString {
                                if (title.isNotBlank()) append(title).append('\n')
                                if (author.isNotBlank()) append("Author: ").append(author).append('\n')
                                append(body.take(MAX_NODE_CHARS))
                            }
                            if (sections.add(block)) collectedChars += block.length
                        }
                    }

                    // Traverse common Schema.org containers, not arbitrary properties.
                    listOf(
                        "@graph", "mainEntity", "mainEntityOfPage", "comment",
                        "acceptedAnswer", "suggestedAnswer", "hasPart", "itemListElement"
                    ).forEach { key -> walk(element[key], depth + 1) }
                }
                else -> Unit
            }
        }

        rawScripts.take(16).forEach { raw ->
            if (raw.isBlank() || raw.length > 50_000) return@forEach
            runCatching { json.parseToJsonElement(raw) }.getOrNull()?.let { walk(it, 0) }
        }

        if (sections.isEmpty()) return null
        return "[Page-supplied structured text; it may represent only part of the page]\n" +
            sections.joinToString("\n\n").take(MAX_TOTAL_CHARS)
    }
}
