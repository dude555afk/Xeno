package com.dude555afk.xeno.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A browsing space. Owns an accent that retints the whole UI.
 *
 * The accent is stored as an ARGB int rather than a string name so adding a
 * palette later doesn't need a migration.
 */
@Entity(tableName = "spaces")
data class SpaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val accentArgb: Int,
    val position: Int = 0,
    val pinned: Boolean = false
)

/**
 * A tab. Tab state is durable; the WebView session is not — the pool owns that.
 *
 * That split is what makes the 2-session cap invisible: closing the app and
 * reopening restores every tab's URL and title without ever holding a session
 * for it.
 */
@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val spaceId: Long,
    val url: String,
    val title: String = "",
    val faviconUrl: String? = null,
    val isPeek: Boolean = false,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
)

/**
 * History, deduplicated by URL — visiting a page 20 times is one entry with a
 * fresher timestamp, not 20 rows.
 */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val url: String,
    val title: String = "",
    val visitedAt: Long = System.currentTimeMillis()
)

/**
 * A cached AI summary, keyed by URL + content hash.
 *
 * The hash is what makes the cache honest: if the page changes, the cached
 * summary is stale and must not be shown.
 */
@Entity(tableName = "summary_cache")
data class SummaryCacheEntity(
    @PrimaryKey val cacheKey: String,
    val url: String,
    val title: String = "",
    val summary: String,
    val tone: String = "TL;DR",
    val createdAt: Long = System.currentTimeMillis()
)

/** Cached answers, same content-hash discipline as summaries. */
@Entity(tableName = "answer_cache")
data class AnswerCacheEntity(
    @PrimaryKey val cacheKey: String,
    val query: String,
    val markdown: String,
    val sourcesJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
