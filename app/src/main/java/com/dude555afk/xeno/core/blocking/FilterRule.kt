package com.dude555afk.xeno.core.blocking

import android.webkit.WebResourceRequest

/**
 * Resource categories used by filter-list rule options (`$script`, `$image`,
 * `$third-party`, …).
 *
 * WebView's `shouldInterceptRequest` gives us a URL and, on API 21+, a way to
 * sniff the Accept header — that's the best we can do, and for the vast
 * majority of rules it's enough.
 */
enum class ResourceType {
    DOCUMENT, SCRIPT, STYLESHEET, IMAGE, FONT,
    XHR, SUBDOCUMENT, MEDIA, WEBSOCKET, OBJECT, POPUP, OTHER;

    companion object {
        fun fromRequest(request: WebResourceRequest): ResourceType {
            val headers = request.requestHeaders
            val accept = headers["Accept"] ?: headers["accept"] ?: ""
            val secFetchDest = headers["Sec-Fetch-Dest"] ?: headers["sec-fetch-dest"]
            val url = request.url.toString()

            val byUrl = when {
                url.endsWith(".js", true) -> SCRIPT
                url.endsWith(".css", true) -> STYLESHEET
                url.endsWith(".woff", true) || url.endsWith(".woff2", true) ||
                    url.endsWith(".ttf", true) || url.endsWith(".otf", true) -> FONT
                url.matches(Regex(".*\\.(png|jpe?g|gif|webp|svg|bmp|ico)(\\?.*)?$", RegexOption.IGNORE_CASE)) -> IMAGE
                else -> null
            }
            if (byUrl != null) return byUrl

            return when (secFetchDest) {
                "script" -> SCRIPT
                "style" -> STYLESHEET
                "image" -> IMAGE
                "font" -> FONT
                "iframe" -> SUBDOCUMENT
                "audio", "video" -> MEDIA
                else -> when {
                    accept.contains("text/css", true) -> STYLESHEET
                    accept.contains("text/html", true) -> DOCUMENT
                    accept.contains("image/", true) -> IMAGE
                    accept.contains("javascript", true) -> SCRIPT
                    else -> OTHER
                }
            }
        }
    }
}

/** Parsed `$options` from a filter line. */
data class RuleOptions(
    val thirdParty: Boolean? = null,
    val domains: Set<String> = emptySet(),
    val notDomains: Set<String> = emptySet(),
    val types: Set<ResourceType> = emptySet(),
    val notTypes: Set<ResourceType> = emptySet(),
    val important: Boolean = false,
    val matchCase: Boolean = false
) {
    companion object {
        val Empty = RuleOptions()

        fun parse(raw: String): RuleOptions {
            if (raw.isBlank()) return Empty
            var thirdParty: Boolean? = null
            var important = false
            var matchCase = false
            val domains = linkedSetOf<String>()
            val notDomains = linkedSetOf<String>()
            val types = linkedSetOf<ResourceType>()
            val notTypes = linkedSetOf<ResourceType>()

            raw.split(',').forEach { partRaw ->
                val part = partRaw.trim()
                if (part.isEmpty()) return@forEach

                when {
                    part == "third-party" -> thirdParty = true
                    part == "~third-party" -> thirdParty = false
                    part == "important" -> important = true
                    part == "match-case" -> matchCase = true
                    part == "~important" -> Unit
                    part.startsWith("domain=") -> {
                        part.removePrefix("domain=").split('|').forEach { d ->
                            val clean = d.trim().lowercase().removePrefix("www.")
                            if (clean.isEmpty()) return@forEach
                            if (clean.startsWith("~")) notDomains += clean.removePrefix("~")
                            else domains += clean
                        }
                    }
                    part.startsWith("~") -> typeFor(part.removePrefix("~"))?.let(notTypes::add)
                    else -> typeFor(part)?.let(types::add)
                }
            }

            return RuleOptions(thirdParty, domains, notDomains, types, notTypes, important, matchCase)
        }

        private fun typeFor(token: String): ResourceType? = when (token) {
            "document", "main_frame" -> DOCUMENT
            "script" -> SCRIPT
            "stylesheet", "css" -> STYLESHEET
            "image", "img" -> IMAGE
            "font" -> FONT
            "xmlhttprequest", "xhr" -> XHR
            "subdocument", "frame" -> SUBDOCUMENT
            "media" -> MEDIA
            "websocket" -> WEBSOCKET
            "object-subrequest", "object" -> OBJECT
            "popup" -> POPUP
            "other" -> OTHER
            else -> null
        }
    }
}

/** One parsed line of a filter list. */
sealed interface FilterRule {

    /** A network rule — blocks or allows a request. */
    data class Network(
        val pattern: String,
        val regex: Regex,
        val isException: Boolean,
        val options: RuleOptions
    ) : FilterRule

    /** A cosmetic rule — `##.selector`, applied via JS after load. */
    data class Cosmetic(
        val domains: Set<String>,
        val selectors: List<String>,
        val isException: Boolean
    ) : FilterRule
}
