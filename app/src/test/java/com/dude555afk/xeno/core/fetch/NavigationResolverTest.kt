package com.dude555afk.xeno.core.fetch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationResolverTest {

    @Test
    fun bareDomainUsesHttps() {
        assertEquals("https://example.com", NavigationResolver.resolve(" example.com "))
        assertEquals("https://example.com/path?q=1#top", NavigationResolver.resolve("example.com/path?q=1#top"))
    }

    @Test
    fun localhostAndValidPortAreRecognized() {
        assertEquals("https://localhost:8080", NavigationResolver.resolve("localhost:8080"))
        assertEquals("https://192.168.1.20:8443/status", NavigationResolver.resolve("192.168.1.20:8443/status"))
    }

    @Test
    fun explicitHttpAndHttpsUrlsArePreserved() {
        assertEquals("http://example.com/legacy", NavigationResolver.resolve("http://example.com/legacy"))
        assertEquals("https://example.com", NavigationResolver.resolve("https://example.com"))
    }

    @Test
    fun blankInputReturnsNullAndAboutBlankIsPreserved() {
        assertNull(NavigationResolver.resolve("   "))
        assertEquals("about:blank", NavigationResolver.resolve("about:blank"))
    }

    @Test
    fun queryIsUrlEncodedAndUnsafeSchemesAreNotExecuted() {
        assertEquals(
            "https://search.brave.com/search?q=hello+world",
            NavigationResolver.resolve("hello world")
        )
        assertEquals(
            "https://search.brave.com/search?q=javascript%3Aalert%281%29",
            NavigationResolver.resolve("javascript:alert(1)")
        )
        assertEquals(
            "https://search.brave.com/search?q=999.1.1.1",
            NavigationResolver.resolve("999.1.1.1")
        )
    }

    @Test
    fun selectableSearchEnginesUseTheirOwnEndpoints() {
        assertEquals(
            "https://duckduckgo.com/?q=hello+world",
            NavigationResolver.resolve("hello world", "duckduckgo")
        )
        assertEquals(
            "https://www.google.com/search?q=hello+world",
            NavigationResolver.resolve("hello world", "google")
        )
        assertEquals(
            "https://www.bing.com/search?q=hello+world",
            NavigationResolver.resolve("hello world", "bing")
        )
        assertEquals(
            "https://search.brave.com/search?q=hello+world",
            NavigationResolver.resolve("hello world", "brave")
        )
    }

    @Test
    fun outOfRangePortsFallBackToSearch() {
        assertEquals(
            "https://search.brave.com/search?q=example.com%3A99999",
            NavigationResolver.resolve("example.com:99999")
        )
    }
}
