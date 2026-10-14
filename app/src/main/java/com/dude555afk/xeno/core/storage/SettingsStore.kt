package com.dude555afk.xeno.core.storage

import android.content.Context
import androidx.core.content.edit
import com.dude555afk.xeno.core.ai.ProviderConfig
import com.dude555afk.xeno.core.ai.ProviderPresets

/**
 * Settings and the provider config.
 *
 * SharedPreferences rather than DataStore on purpose: this is a handful of
 * short values read at startup, and adding a dependency for that isn't worth
 * the APK weight in a project whose whole point is being small.
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var providerId: String
        get() = prefs.getString(KEY_PROVIDER, ProviderPresets.Kilo.id) ?: ProviderPresets.Kilo.id
        set(value) = prefs.edit { putString(KEY_PROVIDER, value) }

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit { putString(KEY_API_KEY, value) }

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, ProviderPresets.Kilo.baseUrl) ?: ProviderPresets.Kilo.baseUrl
        set(value) = prefs.edit { putString(KEY_BASE_URL, value) }

    var model: String
        get() = prefs.getString(KEY_MODEL, ProviderPresets.Kilo.model) ?: ProviderPresets.Kilo.model
        set(value) = prefs.edit { putString(KEY_MODEL, value) }

    var blockingEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLOCKING, true)
        set(value) = prefs.edit { putBoolean(KEY_BLOCKING, value) }

    var activeSpaceId: Long
        get() = prefs.getLong(KEY_SPACE, 1L)
        set(value) = prefs.edit { putLong(KEY_SPACE, value) }

    val providerConfig: ProviderConfig
        get() = ProviderConfig(
            id = providerId,
            displayName = ProviderPresets.byId(providerId).displayName,
            baseUrl = baseUrl,
            apiKey = apiKey,
            model = model
        )

    /** Called after the settings screen saves. */
    fun saveProvider(config: ProviderConfig) {
        prefs.edit {
            putString(KEY_PROVIDER, config.id)
            putString(KEY_API_KEY, config.apiKey)
            putString(KEY_BASE_URL, config.baseUrl)
            putString(KEY_MODEL, config.model)
        }
    }

    val hasApiKey: Boolean get() = apiKey.isNotBlank()

    private companion object {
        const val PREFS = "xeno_prefs"
        const val KEY_PROVIDER = "provider_id"
        const val KEY_API_KEY = "api_key"
        const val KEY_BASE_URL = "base_url"
        const val KEY_MODEL = "model"
        const val KEY_BLOCKING = "blocking_enabled"
        const val KEY_SPACE = "active_space"
    }
}
