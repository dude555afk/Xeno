package com.dude555afk.xeno.core.storage

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import java.nio.charset.StandardCharsets

/**
 * Small, bounded local store for Arc-style pinned and archived pages.
 * The browser's tab database remains the source of truth for open tabs;
 * collections are separate so they don't add a Room migration to UI work.
 */
data class CollectedPage(
    val url: String,
    val title: String,
    val savedAt: Long = System.currentTimeMillis()
)

class PageCollectionStore(context: Context) {
    private val prefs = context.getSharedPreferences("xeno_page_collections", Context.MODE_PRIVATE)

    fun archivedPages(): List<CollectedPage> = read(KEY_ARCHIVE)
    fun pinnedPages(): List<CollectedPage> = read(KEY_PINNED)

    @Synchronized
    fun archive(url: String, title: String) {
        upsert(KEY_ARCHIVE, CollectedPage(url = url, title = title))
    }

    @Synchronized
    fun removeArchived(url: String) {
        write(KEY_ARCHIVE, read(KEY_ARCHIVE).filterNot { it.url == url })
    }

    @Synchronized
    fun pin(url: String, title: String) {
        upsert(KEY_PINNED, CollectedPage(url = url, title = title))
    }

    @Synchronized
    fun unpin(url: String) {
        write(KEY_PINNED, read(KEY_PINNED).filterNot { it.url == url })
    }

    private fun upsert(key: String, page: CollectedPage) {
        val next = (read(key).filterNot { it.url == page.url } + page)
            .sortedByDescending { it.savedAt }
            .take(MAX_ENTRIES)
        write(key, next)
    }

    private fun read(key: String): List<CollectedPage> =
        prefs.getStringSet(key, emptySet()).orEmpty()
            .mapNotNull(::decode)
            .distinctBy { it.url }
            .sortedByDescending { it.savedAt }
            .take(MAX_ENTRIES)

    private fun write(key: String, pages: List<CollectedPage>) {
        prefs.edit { putStringSet(key, pages.take(MAX_ENTRIES).map(::encode).toSet()) }
    }

    private fun encode(page: CollectedPage): String =
        listOf(
            encodeText(page.url),
            encodeText(page.title),
            page.savedAt.toString()
        ).joinToString(":")

    private fun decode(record: String): CollectedPage? {
        val parts = record.split(':', limit = 3)
        if (parts.size != 3) return null
        val url = decodeText(parts[0]) ?: return null
        val title = decodeText(parts[1]) ?: return null
        val savedAt = parts[2].toLongOrNull() ?: return null
        if (url.isBlank()) return null
        return CollectedPage(url = url, title = title, savedAt = savedAt)
    }

    private fun encodeText(value: String): String =
        Base64.encodeToString(
            value.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

    private fun decodeText(value: String): String? =
        runCatching {
            String(
                Base64.decode(value, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
                StandardCharsets.UTF_8
            )
        }.getOrNull()

    private companion object {
        const val KEY_ARCHIVE = "archived_pages"
        const val KEY_PINNED = "pinned_pages"
        const val MAX_ENTRIES = 200
    }
}
