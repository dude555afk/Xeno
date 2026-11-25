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
        get() = adBlock
        set(value) { adBlock = value }

    var adBlock: Boolean
        get() = prefs.getBoolean(KEY_BLOCKING, true)
        set(value) = prefs.edit { putBoolean(KEY_BLOCKING, value) }

    var searchEngine: String
        get() = prefs.getString(KEY_SEARCH_ENGINE, "duckduckgo") ?: "duckduckgo"
        set(value) = prefs.edit { putString(KEY_SEARCH_ENGINE, value) }

    var languageMode: String
        get() = prefs.getString(KEY_LANGUAGE_MODE, "infer") ?: "infer"
        set(value) = prefs.edit { putString(KEY_LANGUAGE_MODE, value) }

    var archiveAfter: String
        get() = prefs.getString(KEY_ARCHIVE_AFTER, "1d") ?: "1d"
        set(value) = prefs.edit { putString(KEY_ARCHIVE_AFTER, value) }

    var alwaysOpenAppLinks: Boolean
        get() = prefs.getBoolean(KEY_ALWAYS_OPEN_APP_LINKS, false)
        set(value) = prefs.edit { putBoolean(KEY_ALWAYS_OPEN_APP_LINKS, value) }

    var allowGoogleTranslate: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_GOOGLE_TRANSLATE, true)
        set(value) = prefs.edit { putBoolean(KEY_ALLOW_GOOGLE_TRANSLATE, value) }

    var forceDark: Boolean
        get() = prefs.getBoolean(KEY_FORCE_DARK, true)
        set(value) = prefs.edit { putBoolean(KEY_FORCE_DARK, value) }

    var appIcon: String
        get() = prefs.getString(KEY_APP_ICON, "default") ?: "default"
        set(value) = prefs.edit { putString(KEY_APP_ICON, value) }

    var autoOpenKeyboard: Boolean
        get() = prefs.getBoolean(KEY_AUTO_OPEN_KEYBOARD, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_OPEN_KEYBOARD, value) }

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "system") ?: "system"
        set(value) = prefs.edit { putString(KEY_THEME_MODE, value) }

    /** Zero means follow the active space accent. */
    var accentOverrideArgb: Int
        get() = prefs.getInt(KEY_ACCENT_OVERRIDE, 0)
        set(value) = prefs.edit { putInt(KEY_ACCENT_OVERRIDE, value) }

    var animationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANIMATIONS_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_ANIMATIONS_ENABLED, value) }

    var activeSpaceId: Long
        get() = prefs.getLong(KEY_SPACE, 1L)
        set(value) = prefs.edit { putLong(KEY_SPACE, value) }

    fun isPinned(url: String): Boolean =
        url.isNotBlank() && url in (prefs.getStringSet(KEY_PINNED_URLS, emptySet()) ?: emptySet())

    fun togglePinned(url: String): Boolean {
        val current = (prefs.getStringSet(KEY_PINNED_URLS, emptySet()) ?: emptySet()).toSet()
        val updated = if (url in current) current - url else current + url
        prefs.edit { putStringSet(KEY_PINNED_URLS, updated) }
        return url in updated
    }

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
        const val KEY_AUTO_OPEN_KEYBOARD = "auto_open_keyboard"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_ACCENT_OVERRIDE = "accent_override_argb"
        const val KEY_ANIMATIONS_ENABLED = "animations_enabled"
        const val KEY_SPACE = "active_space"
        const val KEY_PINNED_URLS = "pinned_urls"
        const val KEY_SEARCH_ENGINE = "search_engine"
        const val KEY_LANGUAGE_MODE = "language_mode"
        const val KEY_ARCHIVE_AFTER = "archive_after"
        const val KEY_ALWAYS_OPEN_APP_LINKS = "always_open_app_links"
        const val KEY_ALLOW_GOOGLE_TRANSLATE = "allow_google_translate"
        const val KEY_FORCE_DARK = "force_dark"
        const val KEY_APP_ICON = "app_icon"
    }
}
