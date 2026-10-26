package com.dude555afk.xeno.core.reader

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractionJsonParserTest {

    private val fallbackUrl = "https://fallback.example/"
    private val longArticle = (1..100).joinToString(" ") { "articleword$it" }

    @Test
    fun parsesDirectJsonObjectFromWebView() {
        val payload = """
            {
              "url": "https://en.wikipedia.org/wiki/King_Von",
              "title": "King Von - Wikipedia",
              "byline": "Wikipedia contributors",
              "text": "$longArticle",
              "excerpt": "An article excerpt",
              "wordCount": 100
            }
        """.trimIndent()

        val result = ExtractionJsonParser.parse(payload, fallbackUrl)

        assertEquals("https://en.wikipedia.org/wiki/King_Von", result.url)
        assertEquals("King Von - Wikipedia", result.title)
        assertEquals("Wikipedia contributors", result.byline)
        assertEquals(100, result.wordCount)
        assertFalse("A full article must not be classified as junk", result.isJunk)
    }

    @Test
    fun parsesJsonStringReturnedByEvaluateJavascript() {
        val payload = """
            {"url":"https://example.com/article","title":"Article","text":"$longArticle","wordCount":100}
        """.trimIndent()
        val encodedString = JsonPrimitive(payload).toString()

        val result = ExtractionJsonParser.parse(encodedString, fallbackUrl)

        assertEquals("https://example.com/article", result.url)
        assertEquals("Article", result.title)
        assertEquals(100, result.wordCount)
        assertFalse(result.isJunk)
    }

    @Test
    fun derivesWordCountWhenMissingOrInvalid() {
        val payload = """{"title":"Article","text":"$longArticle","wordCount":"invalid"}"""

        val result = ExtractionJsonParser.parse(payload, fallbackUrl)

        assertEquals(100, result.wordCount)
        assertFalse(result.isJunk)
        assertEquals(fallbackUrl, result.url)
    }

    @Test
    fun nullOrMalformedJavascriptResultReturnsJunkInsteadOfThrowing() {
        assertTrue(ExtractionJsonParser.parse(null, fallbackUrl).isJunk)
        assertTrue(ExtractionJsonParser.parse("null", fallbackUrl).isJunk)
        assertTrue(ExtractionJsonParser.parse("not-json", fallbackUrl).isJunk)
        assertTrue(ExtractionJsonParser.parse("{}", fallbackUrl).isJunk)
    }
}
