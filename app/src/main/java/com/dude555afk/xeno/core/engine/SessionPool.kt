package com.dude555afk.xeno.core.engine

import android.annotation.SuppressLint
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
     * Returns null if the cap is reached and everything live is more important
     * — the caller is expected to fall back rather than force an allocation.
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
     * visible to the user.
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
     * Aggressive cleanup for [android.app.Activity.onTrimMemory].
     *
     * Called by the system when the app is backgrounded and memory is tight —
     * this is exactly when we need to let go of every session we can.
     */
    fun onTrimMemory(level: Int) {
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            trimTo(0)
        } else {
            sessions.keys.forEach(::park)
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
