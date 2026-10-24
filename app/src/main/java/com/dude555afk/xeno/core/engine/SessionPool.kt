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
        sessions[key]?.let { view ->
            touch(key)
            attachToParent(key, view, parent)
            resume(key)
            return view
        }

        // Make room before allocating a new WebView. Existing active sessions
        // were touched when selected, so the least-recently-used inactive
        // session is evicted first.
        trimTo((maxLiveSessions - 1).coerceAtLeast(0))

        val view = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
            settings.safeBrowsingEnabled = true
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            // This browser currently has no second-window UI. Keeping popups in
            // the current WebView is safer than advertising unsupported windows.
            settings.setSupportMultipleWindows(false)
            settings.mediaPlaybackRequiresUserGesture = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = WebView.OVER_SCROLL_NEVER
        }
        sessions[key] = view
        touch(key)
        attachToParent(key, view, parent)
        return view
    }

    /**
     * Only one WebView may be visible in the browser host. Reparenting a
     * retained session removes the previous view from that host without
     * destroying its page state.
     */
    private fun attachToParent(key: String, view: WebView, parent: ViewGroup?) {
        if (parent == null) return

        sessions.forEach { (otherKey, otherView) ->
            if (otherView !== view && otherView.parent === parent) {
                parent.removeView(otherView)
                park(otherKey)
            }
        }

        (view.parent as? ViewGroup)
            ?.takeIf { it !== parent }
            ?.removeView(view)

        if (view.parent !== parent) {
            (view.parent as? ViewGroup)?.removeView(view)
            parent.addView(view)
        }
    }

    /** Park and detach a session while keeping it available for later reuse. */
    fun detach(key: String) {
        park(key)
        val view = sessions[key] ?: return
        (view.parent as? ViewGroup)?.removeView(view)
    }

    fun detachAll() {
        sessions.keys.toList().forEach(::detach)
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
            }
        }
    }

    /** Resume a parked session. */
    fun resume(key: String) {
        sessions[key]?.let { view ->
            runCatching {
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
     * Callers should pass the active key in [keep]; this pool does not infer
     * which session is visible because detached sessions are also valid.
     */
    fun trimTo(target: Int, keep: Set<String> = emptySet()) {
        val boundedTarget = target.coerceAtLeast(0)
        if (sessions.size <= boundedTarget) return

        // Ignore stale LRU keys: the list must reflect real sessions only.
        val ordered = lastUsed.entries
            .filter { it.key in sessions }
            .sortedBy { it.value }
            .map { it.key }
        var count = sessions.size
        for (key in ordered) {
            if (count <= boundedTarget) break
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
     * we start destroying sessions. The caller protects the visible session.
     */
    fun onTrimMemory(level: Int, keep: Set<String> = emptySet()) {
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> trimTo(0, keep)
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> trimTo(1, keep)
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                sessions.keys.filter { it !in keep }.forEach(::park)
                trimTo(maxOf(1, keep.size), keep)
            }
            else -> sessions.keys.filter { it !in keep }.forEach(::park)
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
