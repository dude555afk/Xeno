package com.dude555afk.xeno.core.model

/** A locally stored bookmark. */
data class Bookmark(
    val url: String,
    val title: String,
    val savedAt: Long = System.currentTimeMillis()
)
