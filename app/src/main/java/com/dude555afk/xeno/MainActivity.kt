package com.dude555afk.xeno

import android.content.Intent
import android.os.Bundle
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
import com.dude555afk.xeno.ui.browser.BrowserScreen
import com.dude555afk.xeno.ui.browser.BrowserViewModel
import com.dude555afk.xeno.ui.theme.XenoTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // A cold-start deep link opens in the active tab. Restored Activities
        // already recover their durable tab state, so don't replay the same link.
        if (savedInstanceState == null) handleIncomingLink(intent)

        setContent {
            val state by viewModel.state.collectAsState()
            val accent = state.activeSpace?.accent ?: Color(0xFF7C5CFF)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingLink(intent)
    }

    private fun handleIncomingLink(incomingIntent: Intent?) {
        val incoming = incomingIntent?.data?.toString()
        if (!incoming.isNullOrBlank()) viewModel.navigate(incoming)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        viewModel.trimMemory(level)
    }

    override fun onDestroy() {
        viewModel.detachHost()
        super.onDestroy()
    }
}
