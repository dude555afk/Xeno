package com.dude555afk.xeno.core.fetch

import com.dude555afk.xeno.core.model.AnswerSource
import com.dude555afk.xeno.core.model.SearchHit
import java.net.URI

/**
 * Hard per-question limits, adapted from AndroidHarness' source/read budgeting.
 * It keeps iterative research bounded even if the planner repeats itself or
 * search engines return the same pages in different orders.
 */
internal class ResearchBudget(
    private val maxQueries: Int = 3,
    private val maxSources: Int = 8,
    private val maxReads: Int = 5,
    private val maxPerHost: Int = 3
) {
    private val queries = LinkedHashSet<String>()
    private val sources = LinkedHashSet<String>()
    private val readPages = LinkedHashSet<String>()
    private val hostCounts = HashMap<String, Int>()

    val queriesUsed: Int get() = queries.size
    val sourcesUsed: Int get() = sources.size
    val pagesRead: Int get() = readPages.size
    val remainingSources: Int get() = (maxSources - sources.size).coerceAtLeast(0)

    fun claimQuery(raw: String): Boolean {
        val query = raw.trim().replace(Regex("\\s+"), " ").lowercase()
        if (query.length < 3 || query in queries || queries.size >= maxQueries) return false
        queries += query
        return true
    }

    /** Adds unique, host-diversified results in their original relevance order. */
    fun accept(results: List<SearchHit>): List<SearchHit> {
        if (remainingSources == 0) return emptyList()
        val accepted = ArrayList<SearchHit>()
        for (hit in results) {
            if (sources.size >= maxSources) break
            if (!isSafePublicWebUrl(hit.url)) continue
            val canonical = canonicalResearchUrl(hit.url)
            if (canonical.isBlank() || canonical in sources) continue
            val host = runCatching { URI(hit.url).host?.lowercase() }.getOrNull().orEmpty()
            if (host.isBlank() || (hostCounts[host] ?: 0) >= maxPerHost) continue
            sources += canonical
            hostCounts[host] = (hostCounts[host] ?: 0) + 1
            accepted += hit
        }
        return accepted
    }

    /** Claims up to [maxReads] distinct pages for extraction. */
    fun claimReads(candidates: List<AnswerSource>): List<AnswerSource> {
        val accepted = ArrayList<AnswerSource>()
        for (source in candidates) {
            if (readPages.size >= maxReads) break
            val key = canonicalResearchUrl(source.url)
            if (key.isBlank() || key in readPages || !isSafePublicWebUrl(source.url)) continue
            readPages += key
            accepted += source
        }
        return accepted
    }
}
