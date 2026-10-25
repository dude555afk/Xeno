package com.dude555afk.xeno.core.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import com.dude555afk.xeno.core.ai.ProviderConfig
import com.dude555afk.xeno.core.ai.ProviderPresets
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small local preference store. Provider keys are encrypted using an Android
 * Keystore AES-GCM key before being written to SharedPreferences.
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Volatile
    private var cachedApiKey: String? = null

    var providerId: String
        get() = prefs.getString(KEY_PROVIDER, ProviderPresets.Kilo.id) ?: ProviderPresets.Kilo.id
        set(value) = prefs.edit { putString(KEY_PROVIDER, value) }

    var apiKey: String
        get() = cachedApiKey ?: readApiKey().also { cachedApiKey = it }
        set(value) {
            if (value.isBlank()) {
                prefs.edit {
                    remove(KEY_API_KEY_ENCRYPTED)
                    remove(KEY_API_KEY_LEGACY)
                }
                cachedApiKey = ""
                return
            }
            val encrypted = runCatching { encryptSecret(value) }.getOrNull() ?: return
            prefs.edit {
                putString(KEY_API_KEY_ENCRYPTED, encrypted)
                remove(KEY_API_KEY_LEGACY)
            }
            cachedApiKey = value
        }

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, ProviderPresets.Kilo.baseUrl) ?: ProviderPresets.Kilo.baseUrl
        set(value) = prefs.edit { putString(KEY_BASE_URL, value) }

    var model: String
        get() = prefs.getString(KEY_MODEL, ProviderPresets.Kilo.model) ?: ProviderPresets.Kilo.model
        set(value) = prefs.edit { putString(KEY_MODEL, value) }

    var blockingEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLOCKING, true)
        set(value) = prefs.edit { putBoolean(KEY_BLOCKING, value) }

    var thirdPartyCookiesEnabled: Boolean
        get() = prefs.getBoolean(KEY_THIRD_PARTY_COOKIES, false)
        set(value) = prefs.edit { putBoolean(KEY_THIRD_PARTY_COOKIES, value) }

    var fingerprintingProtectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_FINGERPRINTING_PROTECTION, true)
        set(value) = prefs.edit { putBoolean(KEY_FINGERPRINTING_PROTECTION, value) }

    var activeSpaceId: Long
        get() = prefs.getLong(KEY_SPACE, 1L)
        set(value) = prefs.edit { putLong(KEY_SPACE, value) }

    fun activeTabId(spaceId: Long): Long? =
        if (prefs.contains(activeTabKey(spaceId))) {
            prefs.getLong(activeTabKey(spaceId), -1L).takeIf { it > 0L }
        } else {
            null
        }

    fun setActiveTabId(spaceId: Long, tabId: Long) {
        if (tabId > 0L) prefs.edit { putLong(activeTabKey(spaceId), tabId) }
    }

    private fun activeTabKey(spaceId: Long): String = KEY_ACTIVE_TAB_PREFIX + spaceId

    val providerConfig: ProviderConfig
        get() = ProviderConfig(
            id = providerId,
            displayName = ProviderPresets.byId(providerId).displayName,
            baseUrl = baseUrl,
            apiKey = apiKey,
            model = model
        )

    /** Return false rather than saving a key in plaintext if encryption fails. */
    fun saveProvider(config: ProviderConfig): Boolean {
        val encrypted = if (config.apiKey.isBlank()) null else
            runCatching { encryptSecret(config.apiKey) }.getOrElse { return false }

        prefs.edit {
            putString(KEY_PROVIDER, config.id)
            if (encrypted == null) remove(KEY_API_KEY_ENCRYPTED)
            else putString(KEY_API_KEY_ENCRYPTED, encrypted)
            remove(KEY_API_KEY_LEGACY)
            putString(KEY_BASE_URL, config.baseUrl)
            putString(KEY_MODEL, config.model)
        }
        cachedApiKey = config.apiKey
        return true
    }

    private fun readApiKey(): String {
        val encrypted = prefs.getString(KEY_API_KEY_ENCRYPTED, null)
        if (!encrypted.isNullOrBlank()) {
            return runCatching { decryptSecret(encrypted) }.getOrDefault("")
        }

        // One-time migration from the plaintext setting used by older builds.
        val legacy = prefs.getString(KEY_API_KEY_LEGACY, "").orEmpty()
        if (legacy.isBlank()) return ""
        val migrated = runCatching { encryptSecret(legacy) }.getOrNull()
        prefs.edit {
            if (migrated != null) putString(KEY_API_KEY_ENCRYPTED, migrated)
            remove(KEY_API_KEY_LEGACY)
        }
        return if (migrated != null) legacy else ""
    }

    private fun encryptSecret(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateEncryptionKey())
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val combined = ByteBuffer.allocate(cipher.iv.size + encrypted.size)
            .put(cipher.iv)
            .put(encrypted)
            .array()
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decryptSecret(value: String): String {
        val combined = Base64.decode(value, Base64.NO_WRAP)
        require(combined.size > IV_BYTES) { "Invalid encrypted provider key" }
        val buffer = ByteBuffer.wrap(combined)
        val iv = ByteArray(IV_BYTES).also { buffer.get(it) }
        val encrypted = ByteArray(buffer.remaining()).also { buffer.get(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateEncryptionKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }

    private fun getOrCreateEncryptionKey(): SecretKey {
        val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = store.getKey(KEYSTORE_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    val hasApiKey: Boolean get() = apiKey.isNotBlank()

    private companion object {
        const val PREFS = "xeno_prefs"
        const val KEY_PROVIDER = "provider_id"
        const val KEY_API_KEY_LEGACY = "api_key"
        const val KEY_API_KEY_ENCRYPTED = "api_key_encrypted"
        const val KEY_BASE_URL = "base_url"
        const val KEY_MODEL = "model"
        const val KEY_BLOCKING = "blocking_enabled"
        const val KEY_THIRD_PARTY_COOKIES = "third_party_cookies_enabled"
        const val KEY_FINGERPRINTING_PROTECTION = "fingerprinting_protection_enabled"
        const val KEY_SPACE = "active_space"
        const val KEY_ACTIVE_TAB_PREFIX = "active_tab_space_"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEYSTORE_ALIAS = "xeno.provider.api-key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
    }
}
