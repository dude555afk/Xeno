package com.dude555afk.xeno

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import com.dude555afk.xeno.ui.corebrowser.CoreBrowserRoot
import com.dude555afk.xeno.ui.theme.XenoTheme

/** Minimal Activity entry point for the Layer 1 core browser. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val initialUrl = intent?.data?.toString()
        setContent {
            XenoTheme(accent = Color(0xFF7C5CFF), darkTheme = true) {
                CoreBrowserRoot(initialUrl = initialUrl)
            }
        }
    }
}
