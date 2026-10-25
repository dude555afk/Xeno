package com.dude555afk.xeno

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dude555afk.xeno.core.storage.SitePermissionStore
import com.dude555afk.xeno.core.storage.XenoDatabase
import com.dude555afk.xeno.ui.browser.BrowserScreen
import com.dude555afk.xeno.ui.browser.BrowserViewModel
import com.dude555afk.xeno.ui.theme.XenoTheme

/**
 * Runs in :private, whose Android WebView profile is separate from the normal
 * browser process. The temporary database and WebView storage are cleared when
 * the private window closes and again on the next launch (crash recovery).
 */
class PrivateBrowserActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()
    private var browserContentInstalled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            AlertDialog.Builder(this)
                .setTitle("Private browsing unavailable")
                .setMessage("Isolated WebView profiles require Android 9 or newer on this device.")
                .setPositiveButton("Close") { _, _ -> finish() }
                .setOnCancelListener { finish() }
                .show()
            return
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        // The process-specific DB is safe to delete before this Activity lazily
        // creates its ViewModel. This also recovers from a previous force-stop.
        XenoDatabase.clearPrivateDatabase(this)
        // A crash/force-stop may skip onDestroy; every private window starts fresh.
        SitePermissionStore(this, privateMode = true).clear()
        clearStalePrivateWebData()
    }

    private fun clearStalePrivateWebData() {
        runCatching {
            val scratch = WebView(this)
            scratch.clearCache(true)
            scratch.clearHistory()
            scratch.clearFormData()
            scratch.destroy()
        }
        runCatching { WebStorage.getInstance().deleteAllData() }
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            runOnUiThread { installBrowserUi() }
        }
    }

    private fun installBrowserUi() {
        if (browserContentInstalled || isFinishing) return
        browserContentInstalled = true
        setContent {
            val state by viewModel.state.collectAsState()
            val accent = state.activeSpace?.accent ?: Color(0xFFC9F63E)
            XenoTheme(accent = accent) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
                        BrowserScreen(state = state, viewModel = viewModel, accent = accent)
                    }
                }
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (browserContentInstalled) viewModel.trimMemory(level)
    }

    override fun onDestroy() {
        // On a final close, ViewModel.onCleared performs profile/database cleanup
        // after cancelling its collectors. On recreation, detach but retain state.
        if (browserContentInstalled && !isFinishing) viewModel.detachHost()
        super.onDestroy()
    }
}
