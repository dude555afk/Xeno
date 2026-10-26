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
 * Lightweight local preferences. Each provider has its own endpoint, model and
 * Android-Keystore-encrypted API key, so switching providers never overwrites
 * another provider's credentials.
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val cachedApiKeys = mutableMapOf<String, String>()

    var providerId: String
        get() = prefs.getString(KEY_PROVIDER, ProviderPresets.Kilo.id) ?: ProviderPresets.Kilo.id
        set(value) {
            prefs.edit { putString(KEY_PROVIDER, value) }
        }

    var apiKey: String
        get() = apiKeyFor(providerId)
        set(value) {
            setApiKeyFor(providerId, value)
        }

    var baseUrl: String
        get() = baseUrlFor(providerId)
        set(value) = prefs.edit { putString(providerBaseUrlKey(providerId), value) }

    var model: String
        get() = modelFor(providerId)
        set(value) = prefs.edit { putString(providerModelKey(providerId), value) }

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
        } else null

    fun setActiveTabId(spaceId: Long, tabId: Long) {
        if (tabId > 0L) prefs.edit { putLong(activeTabKey(spaceId), tabId) }
    }

    private fun activeTabKey(spaceId: Long): String = KEY_ACTIVE_TAB_PREFIX + spaceId

    val providerConfig: ProviderConfig
        get() = providerConfigFor(providerId)

    fun providerConfigFor(id: String): ProviderConfig {
        val preset = ProviderPresets.byId(id)
        return preset.copy(
            baseUrl = baseUrlFor(id),
            apiKey = apiKeyFor(id),
            model = modelFor(id)
        )
    }

    /** Return false rather than persisting a secret in plaintext if encryption fails. */
    @Synchronized
    fun saveProvider(config: ProviderConfig): Boolean {
        val previousProviderId = providerId
        // Migrate any single-provider settings before changing the active ID.
        baseUrlFor(previousProviderId)
        modelFor(previousProviderId)
        apiKeyFor(previousProviderId)

        val encrypted = if (config.apiKey.isBlank()) null else
            runCatching { encryptSecret(config.apiKey) }.getOrElse { return false }

        prefs.edit {
            putString(KEY_PROVIDER, config.id)
            putString(providerBaseUrlKey(config.id), config.baseUrl)
            putString(providerModelKey(config.id), config.model)

            val providerKeyName = providerApiKeyKey(config.id)
            if (encrypted == null) remove(providerKeyName) else putString(providerKeyName, encrypted)

            // Old single-provider fields are now either migrated under the
            // previous provider ID or replaced by the selected provider's values.
            remove(KEY_API_KEY_LEGACY)
            remove(KEY_API_KEY_ENCRYPTED_LEGACY)
            remove(KEY_BASE_URL_LEGACY)
            remove(KEY_MODEL_LEGACY)
        }
        cachedApiKeys[config.id] = config.apiKey
        return true
    }

    private fun legacyProviderId(): String =
        prefs.getString(KEY_PROVIDER, ProviderPresets.Kilo.id) ?: ProviderPresets.Kilo.id

    private fun baseUrlFor(id: String): String {
        prefs.getString(providerBaseUrlKey(id), null)?.let { return it }
        val preset = ProviderPresets.byId(id)
        val legacy = if (id == legacyProviderId()) prefs.getString(KEY_BASE_URL_LEGACY, null) else null
        val value = legacy ?: preset.baseUrl
        if (legacy != null) prefs.edit { putString(providerBaseUrlKey(id), legacy) }
        return value
    }

    private fun modelFor(id: String): String {
        prefs.getString(providerModelKey(id), null)?.let { return it }
        val preset = ProviderPresets.byId(id)
        val legacy = if (id == legacyProviderId()) prefs.getString(KEY_MODEL_LEGACY, null) else null
        val value = legacy ?: preset.model
        if (legacy != null) prefs.edit { putString(providerModelKey(id), legacy) }
        return value
    }

    @Synchronized
    private fun apiKeyFor(id: String): String {
        cachedApiKeys[id]?.let { return it }
        val newKey = prefs.getString(providerApiKeyKey(id), null)
        if (!newKey.isNullOrBlank()) {
            return runCatching { decryptSecret(newKey) }.getOrDefault("").also { cachedApiKeys[id] = it }
        }

        // One-time migration from the single key written by earlier versions.
        if (id == legacyProviderId()) {
            val legacyEncrypted = prefs.getString(KEY_API_KEY_ENCRYPTED_LEGACY, null)
            val legacyPlain = prefs.getString(KEY_API_KEY_LEGACY, "").orEmpty()
            val previous = when {
                !legacyEncrypted.isNullOrBlank() -> runCatching { decryptSecret(legacyEncrypted) }.getOrDefault("")
                legacyPlain.isNotBlank() -> legacyPlain
                else -> ""
            }
            if (previous.isNotBlank()) {
                val encrypted = runCatching { encryptSecret(previous) }.getOrNull()
                if (encrypted != null) {
                    prefs.edit {
                        putString(providerApiKeyKey(id), encrypted)
                        remove(KEY_API_KEY_LEGACY)
                        remove(KEY_API_KEY_ENCRYPTED_LEGACY)
                    }
                    cachedApiKeys[id] = previous
                    return previous
                }
            } else {
                prefs.edit {
                    remove(KEY_API_KEY_LEGACY)
                    remove(KEY_API_KEY_ENCRYPTED_LEGACY)
                }
            }
        }

        cachedApiKeys[id] = ""
        return ""
    }

    @Synchronized
    private fun setApiKeyFor(id: String, value: String) {
        if (value.isBlank()) {
            prefs.edit { remove(providerApiKeyKey(id)) }
            cachedApiKeys[id] = ""
            return
        }

        val encrypted = runCatching { encryptSecret(value) }.getOrNull() ?: return
        prefs.edit { putString(providerApiKeyKey(id), encrypted) }
        cachedApiKeys[id] = value
    }

    private fun providerBaseUrlKey(id: String) = KEY_BASE_URL_PREFIX + id
    private fun providerModelKey(id: String) = KEY_MODEL_PREFIX + id
    private fun providerApiKeyKey(id: String) = KEY_API_KEY_ENCRYPTED_PREFIX + id

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

        // Legacy single-provider keys, migrated lazily.
        const val KEY_API_KEY_LEGACY = "api_key"
        const val KEY_API_KEY_ENCRYPTED_LEGACY = "api_key_encrypted"
        const val KEY_BASE_URL_LEGACY = "base_url"
        const val KEY_MODEL_LEGACY = "model"

        const val KEY_API_KEY_ENCRYPTED_PREFIX = "api_key_encrypted_provider_"
        const val KEY_BASE_URL_PREFIX = "base_url_provider_"
        const val KEY_MODEL_PREFIX = "model_provider_"

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
