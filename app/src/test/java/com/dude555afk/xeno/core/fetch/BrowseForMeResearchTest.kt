package com.dude555afk.xeno.core.fetch

import com.dude555afk.xeno.core.model.AnswerSource
import com.dude555afk.xeno.core.model.SearchHit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowseForMeResearchTest {

    @Test
    fun duckDuckGoParserExtractsTitleSnippetAndTargetUrl() {
        val html = """
            <div class="result">
              <a class="result__a" href="https://example.com/story">Alpha &amp; Beta</a>
              <a class="result__snippet">A useful preview of the story.</a>
            </div>
        """.trimIndent()

        val hits = SearchEngine().parseHtml("duckduckgo", html)

        assertEquals(1, hits.size)
        assertEquals("Alpha & Beta", hits.single().title)
        assertEquals("https://example.com/story", hits.single().url)
        assertEquals("A useful preview of the story.", hits.single().snippet)
    }

    @Test
    fun duckDuckGoRedirectsAreUnwrapped() {
        val html = """<a class="result__a" href="https://duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2Fpage">Example</a>"""
        val hits = SearchEngine().parseHtml("ddg", html)

        assertEquals("https://example.com/page", hits.single().url)
    }

    @Test
    fun bingParserExtractsExternalResult() {
        val html = """
            <li class="b_algo">
              <h2><a href="https://example.org/report">Research report</a></h2>
              <p>Summary of the report.</p>
            </li>
        """.trimIndent()

        val hits = SearchEngine().parseHtml("bing", html)

        assertEquals("Research report", hits.single().title)
        assertEquals("https://example.org/report", hits.single().url)
        assertEquals("Summary of the report.", hits.single().snippet)
    }

    @Test
    fun googleParserUnwrapsSearchResultUrl() {
        val html = """<a href="/url?q=https%3A%2F%2Fexample.net%2Farticle"><h3>Example article</h3></a>"""

        val hits = SearchEngine().parseHtml("google", html)

        assertEquals("https://example.net/article", hits.single().url)
        assertEquals("Example article", hits.single().title)
    }

    @Test
    fun resultUrlValidationRejectsNonWebAndLocalTargets() {
        assertTrue(isSafePublicWebUrl("https://example.com/path"))
        assertFalse(isSafePublicWebUrl("file:///etc/passwd"))
        assertFalse(isSafePublicWebUrl("https://127.0.0.1/admin"))
        assertFalse(isSafePublicWebUrl("http://localhost:8080"))
        assertFalse(isSafePublicWebUrl("https://user:pass@example.com/"))
    }

    @Test
    fun researchBudgetDeduplicatesAndDiversifiesResults() {
        val budget = ResearchBudget(maxQueries = 2, maxSources = 3, maxReads = 1, maxPerHost = 1)
        assertTrue(budget.claimQuery("latest science news"))
        assertFalse(budget.claimQuery(" LATEST   SCIENCE NEWS "))
        assertTrue(budget.claimQuery("independent scientific study"))

        val accepted = budget.accept(
            listOf(
                SearchHit("https://one.example/a", "One A"),
                SearchHit("https://one.example/b", "One B"),
                SearchHit("https://two.example/a", "Two A")
            )
        )
        assertEquals(listOf("One A", "Two A"), accepted.map { it.title })
        assertEquals(2, budget.sourcesUsed)

        val sources = accepted.mapIndexed { index, hit ->
            AnswerSource(index + 1, hit.url, hit.title)
        }
        assertEquals(1, budget.claimReads(sources).size)
        assertTrue(budget.claimReads(sources).isEmpty())
    }

    @Test
    fun plannerOutputIsBoundedAndCitationParserFindsUniqueIndices() {
        assertEquals(
            listOf("study methodology", "independent results"),
            AnswerEngine.parseFollowUpQueries(
                "1. study methodology\n- independent results\n- study methodology",
                "original question"
            )
        )
        assertEquals(
            listOf(1, 3),
            AnswerEngine.parseCitations("Claim [1] and another [3], repeated [1].")
                .map { it.sourceIndex }
        )
    }
}
