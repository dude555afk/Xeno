package com.dude555afk.xeno.core.fetch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEngineTest {

    private val engine = SearchEngine()

    @Test
    fun parsesDuckDuckGoRedirectAndSnippet() {
        val html = """
            <a class="result__a" href="https://duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2Farticle">Research &amp; results</a>
            <a class="result__snippet">Readable snippet &amp; context</a>
        """.trimIndent()

        val results = engine.parseHtml("duckduckgo", html)

        assertEquals(1, results.size)
        assertEquals("https://example.com/article", results.single().url)
        assertEquals("Research & results", results.single().title)
        assertEquals("Readable snippet & context", results.single().snippet)
    }

    @Test
    fun parsesBingResultCards() {
        val html = """
            <li class="b_algo">
              <h2><a href="https://example.org/story">A story</a></h2>
              <p>Evidence from the page.</p>
            </li>
        """.trimIndent()

        val results = engine.parseHtml("bing", html)

        assertEquals(1, results.size)
        assertEquals("https://example.org/story", results.single().url)
        assertEquals("Evidence from the page.", results.single().snippet)
    }

    @Test
    fun canonicalUrlDropsFragmentsAndTrackingParameters() {
        assertEquals(
            "https://example.org/story?section=1",
            canonicalResearchUrl("https://example.org/story?utm_source=mail&section=1#comments")
        )
    }

    @Test
    fun rejectsLocalAndNonHttpTargets() {
        assertFalse(isSafePublicWebUrl("file:///etc/passwd"))
        assertFalse(isSafePublicWebUrl("http://localhost/admin"))
        assertFalse(isSafePublicWebUrl("http://127.0.0.1:8080/private"))
        assertFalse(isSafePublicWebUrl("http://192.168.1.10/"))
        assertFalse(isSafePublicWebUrl("https://user:password@example.org/"))
        assertTrue(isSafePublicWebUrl("https://example.org/article"))
    }
}
