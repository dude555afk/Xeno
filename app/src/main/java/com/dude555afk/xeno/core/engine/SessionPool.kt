package com.dude555afk.xeno.core.engine

import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.StorageController
import java.io.File
import java.util.LinkedHashMap

/**
 * Owns live Gecko sessions for Xeno's tabs.
 * Tabs are durable database rows; only two live engine sessions are retained.
 */
class SessionPool(
    private val context: Context,
    private val isPrivateMode: Boolean = false,
    private val maxLiveSessions: Int = MAX_LIVE_SESSIONS
) {
    private val runtime = GeckoRuntimeOwner.get(context, isPrivateMode)
    private val sessions = LinkedHashMap<String, GeckoBrowserSession>()
    private val lastUsed = LinkedHashMap<String, Long>()

    fun acquire(key: String, parent: ViewGroup? = null): GeckoBrowserSession {
        sessions[key]?.let { session ->
            touch(key)
            attachToParent(session, parent)
            session.onResume()
            return session
        }
        trimTo((maxLiveSessions - 1).coerceAtLeast(0))
        val session = GeckoBrowserSession(context, runtime, isPrivateMode)
        sessions[key] = session
        touch(key)
        attachToParent(session, parent)
        session.onResume()
        return session
    }

    private fun attachToParent(session: GeckoBrowserSession, parent: ViewGroup?) {
        if (parent == null) return
        sessions.values.forEach { other ->
            if (other !== session && other.view.parent === parent) {
                parent.removeView(other.view)
                other.onPause()
            }
        }
        (session.view.parent as? ViewGroup)?.takeIf { it !== parent }?.removeView(session.view)
        if (session.view.parent !== parent) parent.addView(session.view)
        session.view.bringToFront()
    }

    fun detach(key: String) {
        park(key)
        val session = sessions[key] ?: return
        (session.view.parent as? ViewGroup)?.removeView(session.view)
    }

    fun detachAll() {
        sessions.keys.toList().forEach(::detach)
    }

    fun peek(key: String): GeckoBrowserSession? = sessions[key]
    fun liveKeys(): Set<String> = sessions.keys.toSet()
    fun liveCount(): Int = sessions.size

    fun discardCrashed(key: String, crashedSession: GeckoBrowserSession) {
        if (sessions[key] !== crashedSession) return
        (crashedSession.view.parent as? ViewGroup)?.removeView(crashedSession.view)
        sessions.remove(key)
        lastUsed.remove(key)
        crashedSession.clearAndClose()
    }

    fun park(key: String) {
        sessions[key]?.let { runCatching { it.onPause() } }
    }

    fun resume(key: String) {
        sessions[key]?.let { runCatching { it.onResume() } }
    }

    fun destroy(key: String) {
        sessions.remove(key)?.let { session ->
            (session.view.parent as? ViewGroup)?.removeView(session.view)
            runCatching { session.stopLoading() }
            session.clearAndClose()
        }
        lastUsed.remove(key)
    }

    fun trimTo(target: Int, keep: Set<String> = emptySet()) {
        val boundedTarget = target.coerceAtLeast(0)
        if (sessions.size <= boundedTarget) return
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

    /** Close every GeckoSession before removing data from Gecko's profile. */
    fun clearStorageData(onComplete: () -> Unit = {}) {
        destroyAll()
        runtime.storageController.clearData(StorageController.ClearFlags.ALL).accept {
            Handler(Looper.getMainLooper()).post { onComplete() }
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
        const val MAX_LIVE_SESSIONS = 2
    }
}

/**
 * GeckoRuntime is process-wide. Normal and private Android processes receive
 * separate profile directories; private-mode sessions also use Gecko's
 * non-persistent storage mode.
 */
private object GeckoRuntimeOwner {
    @Volatile private var runtime: GeckoRuntime? = null

    @Synchronized
    fun get(context: Context, privateMode: Boolean): GeckoRuntime {
        runtime?.let { return it }
        val appContext = context.applicationContext
        val profileName = if (privateMode) "gecko-private-profile" else "gecko-profile"
        val profileDirectory = File(appContext.filesDir, profileName)
        profileDirectory.mkdirs()

        // Keep Mozilla telemetry and crash uploads disabled in Xeno's embedded engine.
        // GeckoView reads this app-private config before starting the Gecko runtime.
        val privacyConfig = File(appContext.filesDir, "geckoview-privacy-$profileName.yaml")
        val privacyPreferences = """
            prefs:
              toolkit.telemetry.enabled: false
              toolkit.telemetry.unified: false
              toolkit.telemetry.archive.enabled: false
              toolkit.telemetry.firstShutdownPing.enabled: false
              toolkit.telemetry.shutdownPingSender.enabled: false
              datareporting.healthreport.uploadEnabled: false
              datareporting.policy.dataSubmissionEnabled: false
              datareporting.healthreport.service.enabled: false
              datareporting.crashreporter.submitEnabled: false
              toolkit.crashreporter.enabled: false
              app.shield.optoutstudies.enabled: false
              app.normandy.enabled: false
              browser.ping-centre.telemetry: false
              browser.newtabpage.activity-stream.telemetry: false
              browser.newtabpage.activity-stream.feeds.telemetry: false
        """.trimIndent()
        if (!privacyConfig.exists() || privacyConfig.readText() != privacyPreferences) {
            privacyConfig.writeText(privacyPreferences)
        }

        val settings = GeckoRuntimeSettings.Builder()
            .arguments(arrayOf("-profile", profileDirectory.absolutePath))
            .configFilePath(privacyConfig.absolutePath)
            // Avoid preloading an extra Gecko process. A little slower to first paint,
            // but more appropriate for Xeno's low-memory target.
            .appZygoteProcessEnabled(false)
            // Xeno ships an embedded engine, not a remote-debuggable Firefox app.
            // Keep diagnostics and developer endpoints off in production.
            .remoteDebuggingEnabled(false)
            .debugLogging(false)
            .consoleOutput(false)
            .aboutConfigEnabled(false)
            // Do not enroll this browser in Mozilla experiment/rollout systems.
            .experimentDelegate(null)
            .build()
        return GeckoRuntime.create(appContext, settings).also { runtime = it }
    }
}
