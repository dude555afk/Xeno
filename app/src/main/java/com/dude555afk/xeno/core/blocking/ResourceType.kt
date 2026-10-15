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
        /**
         * Best-effort classification from what the WebView actually gives us.
         *
         * The header sniff is deliberately ordered: `Sec-Fetch-Dest` is the
         * authoritative answer where present, `Accept` is the fallback, and a
         * URL-extension check runs first only because media fetches often
         * arrive with a fully generic Accept value.
         */
        fun fromRequest(request: WebResourceRequest): ResourceType {
            val url = request.url.toString()

            val byUrl = when {
                url.endsWith(".js", true) || url.endsWith(".mjs", true) -> SCRIPT
                url.endsWith(".css", true) -> STYLESHEET
                url.endsWith(".woff", true) || url.endsWith(".woff2", true) ||
                    url.endsWith(".ttf", true) || url.endsWith(".otf", true) ||
                    url.endsWith(".eot", true) -> FONT
                else -> null
            }
            if (byUrl != null) return byUrl

            if (url.matches(Regex(".*\\.(png|jpe?g|gif|webp|svg|bmp|ico|tiff?)(\\?.*)?$", RegexOption.IGNORE_CASE))) {
                return IMAGE
            }

            val headers = request.requestHeaders
            val dest = headers["Sec-Fetch-Dest"] ?: headers["sec-fetch-dest"]
            val mode = headers["Sec-Fetch-Mode"] ?: headers["sec-fetch-mode"]

            when (dest) {
                "script" -> return SCRIPT
                "style", "stylesheet" -> return STYLESHEET
                "image", "img" -> return IMAGE
                "font" -> return FONT
                "iframe", "frame", "embed", "object" -> return SUBDOCUMENT
                "audio", "video", "track" -> return MEDIA
                "document" -> return DOCUMENT
                "empty" -> if (mode != "no-cors") return XHR
            }

            val accept = headers["Accept"] ?: headers["accept"] ?: ""
            return when {
                accept.contains("text/css", true) -> STYLESHEET
                accept.contains("text/html", true) -> DOCUMENT
                accept.contains("image/", true) -> IMAGE
                accept.contains("font/", true) -> FONT
                accept.contains("javascript", true) -> SCRIPT
                accept.contains("application/", true) -> XHR
                else -> OTHER
            }
        }
    }
}

/**
 * Parsed `$options` from a filter line.
 *
 * Covers the options EasyList and EasyPrivacy actually use. The exotic ones
 * (`$replace`, `$redirect`, `$csp`, `$removeparam`) are deliberately absent —
 * see the class-level note in FilterListEngine for why.
 */
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

        /**
         * Parse the tail of a rule after the first `$`.
         *
         * Tolerant by design: an unrecognised token is skipped rather than
         * discarding the whole rule. EasyList grows new options faster than
         * anyone updates a parser, and a blocked rule that missed one option
         * is a far smaller problem than a rule that stopped matching at all.
         */
        fun parse(raw: String): RuleOptions {
            if (raw.isBlank()) return Empty

            var thirdParty: Boolean? = null
            var important = false
            var matchCase = false
            val domains = linkedSetOf<String>()
            val notDomains = linkedSetOf<String>()
            val types = linkedSetOf<ResourceType>()
            val notTypes = linkedSetOf<ResourceType>()

            raw.split(',').forEach { tokenRaw ->
                val token = tokenRaw.trim()
                if (token.isEmpty()) return@forEach

                when {
                    token == "third-party" -> thirdParty = true
                    token == "~third-party" -> thirdParty = false
                    token == "important" -> important = true
                    token == "match-case" -> matchCase = true
                    token.startsWith("domain=") -> {
                        token.removePrefix("domain=").split('|').forEach { entryRaw ->
                            val entry = entryRaw.trim().lowercase().removePrefix("www.")
                            if (entry.isEmpty()) return@forEach
                            if (entry.startsWith("~")) {
                                notDomains += entry.removePrefix("~")
                            } else {
                                domains += entry
                            }
                        }
                    }
                    token.startsWith("~") -> typeFor(token.removePrefix("~"))?.let(notTypes::add)
                    else -> typeFor(token)?.let(types::add)
                }
            }

            return RuleOptions(
                thirdParty = thirdParty,
                domains = domains,
                notDomains = notDomains,
                types = types,
                notTypes = notTypes,
                important = important,
                matchCase = matchCase
            )
        }

        private fun typeFor(token: String): ResourceType? = when (token.lowercase()) {
            "document", "doc", "main_frame" -> ResourceType.DOCUMENT
            "script", "js" -> ResourceType.SCRIPT
            "stylesheet", "css" -> ResourceType.STYLESHEET
            "image", "img" -> ResourceType.IMAGE
            "font" -> ResourceType.FONT
            "xmlhttprequest", "xhr" -> ResourceType.XHR
            "subdocument", "frame", "iframe" -> ResourceType.SUBDOCUMENT
            "media" -> ResourceType.MEDIA
            "websocket", "ws" -> ResourceType.WEBSOCKET
            "object-subrequest", "object" -> ResourceType.OBJECT
            "popup", "popunder" -> ResourceType.POPUP
            "other" -> ResourceType.OTHER
            else -> null
        }
    }
}
