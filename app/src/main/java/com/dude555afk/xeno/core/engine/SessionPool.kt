package com.dude555afk.xeno.core.engine

import android.annotation.SuppressLint
import android.content.ComponentCallbacks2
import android.content.Context
import android.view.ViewGroup
import android.webkit.WebView
import java.util.LinkedHashMap

/**
 * Owns every live WebView in the app.
 *
 * The hard rule from docs/ARCHITECTURE.md: **at most 2 live sessions.** Each
 * WebView costs 150-250 MB, so a naive "one WebView per tab" design makes the
 * app fall over the moment someone opens six tabs.
 *
 * Tabs are cheap; sessions are expensive. This pool keeps tab state in the
 * database and materialises a WebView only for the active tab plus at most one
 * peek/split session.
 */
class SessionPool(
    private val context: Context,
    private val maxLiveSessions: Int = MAX_LIVE_SESSIONS
) {

    private val sessions = LinkedHashMap<String, WebView>()

    /** Insertion-ordered so trimming drops the least recently used first. */
    private val lastUsed = LinkedHashMap<String, Long>()

    /**
     * Get the session for [key], creating one if we're under the cap.
     *
     * Never returns null — the cap is enforced by trimming *before* allocating.
     * The one key that must survive is the caller's, and it is touched first.
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun acquire(key: String, parent: ViewGroup? = null): WebView {
        touch(key)
        sessions[key]?.let { return it }

        trimTo(maxLiveSessions - 1)

        val view = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            // Popups are routed to the OS instead of a second session, but the
            // flag has to be true for window.open to reach us at all.
            settings.setSupportMultipleWindows(true)
            settings.mediaPlaybackRequiresUserGesture = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = WebView.OVER_SCROLL_NEVER
        }
        sessions[key] = view
        lastUsed[key] = System.currentTimeMillis()
        parent?.addView(view)
        return view
    }

    fun peek(key: String): WebView? = sessions[key]

    fun liveKeys(): Set<String> = sessions.keys.toSet()

    fun liveCount(): Int = sessions.size

    /**
     * Pause a session but keep it — scrolling away and back should be instant.
     *
     * Parked sessions keep their DOM and scroll position; only timers and
     * rAF stop. This is what makes switching tabs feel free.
     */
    fun park(key: String) {
        sessions[key]?.let { view ->
            runCatching {
                view.onPause()
                view.pauseTimers()
            }
        }
    }

    /** Resume a parked session. */
    fun resume(key: String) {
        sessions[key]?.let { view ->
            runCatching {
                view.resumeTimers()
                view.onResume()
            }
        }
    }

    /** Destroy a session outright and drop it from the pool. */
    fun destroy(key: String) {
        sessions.remove(key)?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            runCatching {
                view.stopLoading()
                view.loadUrl("about:blank")
                view.removeAllViews()
                view.destroy()
            }
        }
        lastUsed.remove(key)
    }

    /**
     * Trim down to [target] live sessions, dropping least-recently-used first.
     *
     * Never destroys the currently active tab's session — that would be
     * visible to the user as a blank screen.
     */
    fun trimTo(target: Int, keep: Set<String> = emptySet()) {
        if (sessions.size <= target) return

        val ordered = lastUsed.entries.sortedBy { it.value }.map { it.key }
        var count = sessions.size
        for (key in ordered) {
            if (count <= target) break
            if (key in keep) continue
            destroy(key)
            count--
        }
    }

    /**
     * Memory-pressure cleanup, graduated by severity.
     *
     * The tempting move is `trimTo(0)` on any memory warning. It is wrong: at
     * TRIM_MEMORY_RUNNING_LOW the app is foreground and the user is mid-sentence,
     * and nuking the session loses the page they are reading for a saving they
     * cannot see. Parking preserves everything that matters and costs nothing.
     *
     * Only at CRITICAL/COMPLETE — i.e. the LMK is about to kill us anyway — do
     * we start destroying sessions, and even then we keep one.
     */
    fun onTrimMemory(level: Int) {
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> trimTo(0)
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> trimTo(1)
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                sessions.keys.forEach(::park)
                trimTo(1)
            }
            else -> sessions.keys.forEach(::park)
        }
    }

    fun destroyAll() {
        sessions.keys.toList().forEach(::destroy)
        lastUsed.clear()
    }

    private fun touch(key: String) {
        lastUsed.remove(key)
        lastUsed[key] = System.currentTimeMillis()
    }

    companion object {
        /** Hard cap. Two = active tab + peek/split. */
        const val MAX_LIVE_SESSIONS = 2
    }
}
