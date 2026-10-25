package com.dude555afk.xeno.core.ai

/**
 * A single OpenAI-compatible endpoint.
 *
 * OpenRouter Free is the default provider; Kilo remains available as an alternative, but
 * because the shape is the standard one, anything else (Groq, OpenRouter, a
 * local llama.cpp server) works by changing three strings. No provider-specific
 * code anywhere in the client.
 */
data class ProviderConfig(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val enabled: Boolean = true
) {
    /**
     * Anonymous requests are limited to Kilo's explicit free models or an
     * explicitly selected local provider on loopback/private LAN. Arbitrary
     * public endpoints never qualify for the no-key path.
     */
    val allowsAnonymousFreeModel: Boolean
        get() {
            if (id == "kilo") {
                val supportedGateway = baseUrl.trimEnd('/').equals(
                    "https://ai-gateway.kilo.ai/api/v1",
                    ignoreCase = true
                ) || baseUrl.trimEnd('/').equals(
                    "https://api.kilo.ai/api/gateway",
                    ignoreCase = true
                )
                // Kilo documents both explicit :free models and its virtual
                // kilo-auto/free router as available without authentication.
                val freeModel = model.endsWith(":free", ignoreCase = true) ||
                    model.equals("kilo-auto/free", ignoreCase = true)
                return supportedGateway && freeModel
            }
            if (id != "local" || !baseUrl.startsWith("http://", ignoreCase = true)) return false
            val host = runCatching { java.net.URI(baseUrl).host?.lowercase() }.getOrNull().orEmpty()
            return host == "localhost" || host == "127.0.0.1" || host == "::1" ||
                host == "10.0.2.2" || host.endsWith(".local") ||
                host.startsWith("10.") || host.startsWith("192.168.") ||
                Regex("^172\\.(1[6-9]|2[0-9]|3[01])\\.").containsMatchIn(host)
        }
}
/**
 * Built-in presets. The user can override every field.
 *
 * BYOK by design — Xeno has no backend, so any AI traffic the user makes is on
 * their own key. That keeps the app free at any scale and keeps it off the
 * wrong side of anyone's ToS.
 */
object ProviderPresets {

    val Kilo = ProviderConfig(
        id = "kilo",
        displayName = "Kilo Gateway",
        baseUrl = "https://ai-gateway.kilo.ai/api/v1",
        apiKey = "",
        model = "kilo-auto/free"
    )

    val Groq = ProviderConfig(
        id = "groq",
        displayName = "Groq",
        baseUrl = "https://api.groq.com/openai/v1",
        apiKey = "",
        model = "llama-3.3-70b-versatile"
    )

    val OpenRouter = ProviderConfig(
        id = "openrouter",
        displayName = "OpenRouter",
        baseUrl = "https://openrouter.ai/api/v1",
        apiKey = "",
        model = "meta-llama/llama-3.3-70b-instruct:free"
    )

    /** For people running their own box. */
    val Local = ProviderConfig(
        id = "local",
        displayName = "Local (OpenAI-compatible)",
        baseUrl = "http://127.0.0.1:8080/v1",
        apiKey = "",
        model = "local-model"
    )

    val all = listOf(Kilo, Groq, OpenRouter, Local)

    fun byId(id: String): ProviderConfig = all.firstOrNull { it.id == id } ?: OpenRouter
}
