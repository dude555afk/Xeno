package com.dude555afk.xeno.core.model

/**
 * A tab. One tab == one WebView session, and sessions are the expensive
 * resource in this app (150-250 MB each). See [com.dude555afk.xeno.core.engine.SessionPool].
 *
 * The session is intentionally NOT stored here — the pool owns its own map
 * keyed by tab id, so a Tab can be serialized/restored without dragging a live
 * WebView along with it.
 */
data class Tab(
    val id: Long = 0L,
    val spaceId: Long = 1L,
    val url: String = "",
    val title: String = "",
    val faviconUrl: String? = null,
    val isPeek: Boolean = false,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    /** "web" or "ai"; the AI query is retained so a tab can be reopened. */
    val type: String = "web",
    val query: String = "",
    /** Incognito tabs live only in memory and are never written to Room. */
    val isIncognito: Boolean = false,
    val desktopSite: Boolean = false,
    val zoomLevel: Int = 100,
    val blockedAdsCount: Int = 0
) {
    val displayTitle: String
        get() = title.ifBlank { url.ifBlank { "New tab" } }

    val hasContent: Boolean
        get() = url.isNotBlank()
}

/**
 * Loading state mirrored from the WebView so the UI can render a progress line
 * without holding a reference to the view.
 */
data class PageLoadState(
    val progress: Float = 0f,      // 0..1
    val loading: Boolean = false,
    val url: String = "",
    val title: String = ""
)


/** A normal tab removed from the carousel and retained in the archive. */
data class ArchivedTab(
    val id: Long,
    val originalTabId: Long,
    val spaceId: Long,
    val url: String,
    val title: String = "",
    val faviconUrl: String? = null,
    val type: String = "web",
    val query: String = "",
    val desktopSite: Boolean = false,
    val zoomLevel: Int = 100,
    val blockedAdsCount: Int = 0,
    val archivedAt: Long = System.currentTimeMillis()
)

/** A saved page, independent of whether its tab is still open. */
data class PinnedPage(
    val url: String,
    val title: String = "",
    val faviconUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
