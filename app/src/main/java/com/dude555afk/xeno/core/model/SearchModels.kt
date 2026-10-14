package com.dude555afk.xeno.core.model

/** A single search-hit row returned by the offscreen scraper. */
data class SearchHit(
    val url: String,
    val title: String,
    val snippet: String = ""
)

/** How far along the offscreen scrape got, for UI feedback. */
sealed interface SearchProgress {
    data object Idle : SearchProgress
    data class Searching(val engine: String) : SearchProgress
    data class Done(val hits: List<SearchHit>) : SearchProgress
    data class Failed(val message: String, val engine: String) : SearchProgress
}
