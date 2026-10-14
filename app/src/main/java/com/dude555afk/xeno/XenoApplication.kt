package com.dude555afk.xeno

import android.app.Application
import com.dude555afk.xeno.core.blocking.FilterListSources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class XenoApplication : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Load the bundled filter lists off the main thread so first paint
        // isn't delayed by parsing a few hundred thousand rules.
        scope.launch {
            FilterListSources.defaults.filter { it.enabledByDefault }.forEach { spec ->
                runCatching {
                    assets.open(spec.assetName).bufferedReader().use { it.readText() }
                }
            }
        }
    }
}
