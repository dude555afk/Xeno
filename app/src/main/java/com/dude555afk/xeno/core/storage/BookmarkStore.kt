package com.dude555afk.xeno.core.storage

import android.content.Context
import com.dude555afk.xeno.core.model.Bookmark
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lightweight local bookmark storage. Bookmarks are small and user-owned, so a
 * bounded JSON preference avoids a schema migration for one simple collection.
 */
class BookmarkStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun all(): List<Bookmark> {
        val raw = prefs.getString(KEY_BOOKMARKS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val url = item.optString("url").trim()
                    if (url.isBlank()) continue
                    add(
                        Bookmark(
                            url = url,
                            title = item.optString("title").ifBlank { url },
                            savedAt = item.optLong("savedAt", 0L)
                        )
                    )
                }
            }.sortedByDescending { it.savedAt }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun add(url: String, title: String): List<Bookmark> {
        val cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) return all()
        val now = System.currentTimeMillis()
        val updated = (listOf(Bookmark(cleanUrl, title.trim().ifBlank { cleanUrl }, now)) +
            all().filterNot { it.url == cleanUrl })
            .take(MAX_BOOKMARKS)
        persist(updated)
        return updated
    }

    @Synchronized
    fun remove(url: String): List<Bookmark> {
        val updated = all().filterNot { it.url == url }
        persist(updated)
        return updated
    }

    private fun persist(bookmarks: List<Bookmark>) {
        val array = JSONArray()
        bookmarks.forEach { bookmark ->
            array.put(
                JSONObject()
                    .put("url", bookmark.url)
                    .put("title", bookmark.title)
                    .put("savedAt", bookmark.savedAt)
            )
        }
        prefs.edit().putString(KEY_BOOKMARKS, array.toString()).apply()
    }

    private companion object {
        const val PREFS = "xeno_bookmarks"
        const val KEY_BOOKMARKS = "bookmarks"
        const val MAX_BOOKMARKS = 2_000
    }
}
