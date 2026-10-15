package com.dude555afk.xeno

import android.app.Application
import com.dude555afk.xeno.core.blocking.FilterListEngine
import com.dude555afk.xeno.core.blocking.HostsList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Process-wide singletons.
 *
 * The point of keeping the filter engine here rather than in the ViewModel is
 * that it survives the Activity lifecycle. Parsing a few hundred thousand
 * rules twice because the user rotated the screen is exactly the kind of tax
 * this app exists to avoid.
 */
class XenoApplication : Application() {

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
