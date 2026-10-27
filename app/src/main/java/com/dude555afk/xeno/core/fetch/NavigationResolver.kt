package com.dude555afk.xeno.core.fetch

import java.net.URI
import java.net.URLEncoder

/**
 * Resolves address-bar text without ever executing script/file schemes.
 * Plain terms use the search provider; valid host-like input becomes HTTPS.
 */
object NavigationResolver {
    private val hostLike = Regex(
        "^(?:(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,}|localhost|(?:[0-9]{1,3}\\.){3}[0-9]{1,3})(?::[0-9]{1,5})?(?:[/?#].*)?$",
        RegexOption.IGNORE_CASE
    )
    private val ipv4 = Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")

    fun resolve(raw: String, searchEngineId: String = "duckduckgo"): String? {
        val query = raw.trim()
        if (query.isEmpty()) return null
        if (query.equals("about:blank", ignoreCase = true)) return "about:blank"

        if (query.startsWith("https://", ignoreCase = true) ||
            query.startsWith("http://", ignoreCase = true)
        ) {
            return query
        }

        if (hostLike.matches(query) && isValidHostAndPort(query)) {
            return "https://$query"
        }

        val encoded = URLEncoder.encode(query, "UTF-8")
        val base = when (searchEngineId) {
            "duckduckgo" -> "https://duckduckgo.com/?q="
            "google" -> "https://www.google.com/search?q="
            "bing" -> "https://www.bing.com/search?q="
            "startpage" -> "https://www.startpage.com/sp/search?query="
            else -> "https://search.brave.com/search?q="
        }
        return base + encoded
    }

    private fun isValidHostAndPort(value: String): Boolean {
        val uri = runCatching { URI("https://$value") }.getOrNull() ?: return false
        val host = uri.host ?: return false
        if (uri.port == 0 || uri.port > 65535) return false
        if (ipv4.matches(host)) {
            val octets = host.split('.').mapNotNull { it.toIntOrNull() }
            if (octets.size != 4 || octets.any { it !in 0..255 }) return false
        }
        return true
    }
}
