package com.dude555afk.xeno.core.fetch

import com.dude555afk.xeno.core.model.AnswerSource
import com.dude555afk.xeno.core.model.SearchHit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResearchBudgetTest {

    @Test
    fun boundsQueriesSourcesAndPageReads() {
        val budget = ResearchBudget(maxQueries = 2, maxSources = 2, maxReads = 1, maxPerHost = 1)

        assertTrue(budget.claimQuery("Initial question"))
        assertFalse(budget.claimQuery(" initial   question "))
        assertTrue(budget.claimQuery("Second evidence angle"))
        assertFalse(budget.claimQuery("Third evidence angle"))

        val accepted = budget.accept(
            listOf(
                SearchHit("https://one.example/a?utm_source=x", "A"),
                SearchHit("https://one.example/a#section", "Duplicate"),
                SearchHit("https://two.example/b", "B"),
                SearchHit("https://one.example/c", "Same host cap")
            )
        )

        assertEquals(listOf("A", "B"), accepted.map { it.title })
        assertEquals(2, budget.sourcesUsed)
        assertEquals(0, budget.remainingSources)

        val reads = budget.claimReads(
            accepted.mapIndexed { index, hit ->
                AnswerSource(index + 1, hit.url, hit.title, hit.snippet)
            }
        )
        assertEquals(1, reads.size)
        assertEquals(1, budget.pagesRead)
        assertTrue(budget.claimReads(reads).isEmpty())
    }
}
