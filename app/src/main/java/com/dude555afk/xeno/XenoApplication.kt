package com.dude555afk.xeno

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import android.webkit.WebView
import com.dude555afk.xeno.core.blocking.FilterListEngine
import com.dude555afk.xeno.core.blocking.HostsList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Process-wide singletons.
 *
 * The private-browsing Activity runs in a separate Android process so Android
 * WebView can give it an independent on-disk profile. The suffix is set before
 * either process creates its first WebView.
 */
class XenoApplication : Application() {

    companion object {
        const val PRIVATE_PROCESS_SUFFIX = ":private"
        const val PRIVATE_WEBVIEW_SUFFIX = "xeno-private"
    }

    /** True only inside Xeno's isolated private-browsing process. */
    var isPrivateProcess: Boolean = false
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        val processName = currentProcessName(base)
        isPrivateProcess = processName.endsWith(PRIVATE_PROCESS_SUFFIX)
        if (isPrivateProcess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WebView.setDataDirectorySuffix(PRIVATE_WEBVIEW_SUFFIX)
        }
    }

    private fun currentProcessName(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return Application.getProcessName()
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        return manager?.runningAppProcesses
            ?.firstOrNull { it.pid == Process.myPid() }
            ?.processName.orEmpty()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Loaded on a background dispatcher so first paint is never delayed by
     * rule parsing. The bundled list is tiny by design, so this finishes long
     * before a user has finished typing a URL.
     */
    val filters: FilterListEngine = FilterListEngine()

    override fun onCreate() {
        super.onCreate()
        scope.launch { runCatching { filters.load(HostsList.rules) } }
    }
}
