package com.dude555afk.xeno.core.fetch

import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEngineScriptTest {

    @Test
    fun rawScriptsUseJavaScriptRegexEscapesNotKotlinStringEscapes() {
        val scripts = listOf(SearchEngine.SCRAPE_DDG, SearchEngine.SCRAPE_DDG_LITE)

        scripts.forEach { script ->
            // These scripts are Kotlin raw strings, so each JS regex needs a single
            // backslash in the emitted source. Double escaping made the URL regex
            // invalid and caused the search scraper to return no results.
            assertTrue(script.contains(".replace(/\\s+/g, ' ')"))
            assertTrue(script.contains("!/^https?:\\/\\//i.test"))
        }
    }
}
