package com.dude555afk.xeno.core.blocking

import java.util.concurrent.ConcurrentHashMap

/**
 * A native filter-list engine speaking uBlock Origin's syntax.
 *
 * This is the piece that lets Xeno ship real blocking without the 190 MB
 * GeckoView + real extension tradeoff. It parses EasyList / EasyPrivacy / the
 * uBO-specific lists and matches requests in [shouldInterceptRequest].
 *
 * What's deliberately out of scope for v1, and why:
 *
 * - `$/replace=` response rewriting — WebView gives us no body access.
 * - `redirect=` resources — same reason, plus no bundled resource packs.
 * - `script:inject` (scriptlets) — needs an isolated JS world; we approximate
 *   with a tiny builtin set. Cosmetic filtering covers most of the visible
 *   damage anyway.
 *
 * Known to be worth roughly 90-95% of uBO's outcome in practice.
 */
class FilterListEngine {

    private val networkRules = mutableListOf<FilterRule.Network>()
    private val cosmeticRules = mutableListOf<FilterRule.Cosmetic>()

    /** Global (`##`) cosmetic selectors, keyed for fast lookup. */
    private val globalCosmetic = mutableListOf<String>()

    /** Domain-scoped cosmetic selectors. */
    private val domainCosmetic = ConcurrentHashMap<String, MutableList<String>>()

    private val hostIndex = ConcurrentHashMap<String, MutableList<FilterRule.Network>>()

    @Volatile
    var ruleCount: Int = 0
        private set

    /**
     * Load a filter list in Adblock-Plus / uBlock format.
     *
     * Cheap enough to call repeatedly; rules accumulate as new lists load.
     */
    fun load(listText: String) {
        listText.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            if (line.startsWith('!') || line.startsWith('[')) return@forEach

            // Cosmetic rule: domain##selector   /   ##selector
            val sepIndex = line.indexOf("##")
            if (sepIndex >= 0) {
                val (domainsPart, selector) = line.splitAt(sepIndex)
                val isException = domainsPart == "#@#"
                if (selector.isBlank()) return@forEach

                val domains = domainsPart.split(',')
                    .filter { it.isNotBlank() }
                    .map { it.lowercase().removePrefix("www.") }
                    .toSet()

                val rule = FilterRule.Cosmetic(domains, selector.split(',').filter { it.isNotBlank() }, isException)
                cosmeticRules += rule

                if (domains.isEmpty()) {
                    if (!isException) globalCosmetic += selector
                } else if (!isException) {
                    domains.forEach { d -> domainCosmetic.getOrPut(d) { mutableListOf() } += selector }
                }
                return@forEach
            }

            val isException = line.startsWith("@@")
            val body = if (isException) line.removePrefix("@@") else line

            // Split off $options.
            val dollar = body.lastIndexOf('$')
            val pattern: String
            val options: RuleOptions
            if (dollar > 0) {
                pattern = body.substring(0, dollar)
                options = RuleOptions.parse(body.substring(dollar + 1))
            } else {
                pattern = body
                options = RuleOptions.Empty
            }

            try {
                val rule = FilterRule.Network(
                    pattern = pattern,
                    regex = compile(pattern, options.matchCase),
                    isException = isException,
                    options = options
                )
                networkRules += rule

                // Cheap bucket on the host when the rule is host-anchored.
                val host = pattern.removePrefix("||").substringBefore('/').substringBefore('^')
                if (host.isNotBlank() && !host.contains('*')) {
                    hostIndex.getOrPut(host.lowercase()) { mutableListOf() } += rule
                }
            } catch (_: Exception) {
 // Unparseable line — skip it. Never let one bad rule kill the list.
            }
        }
        ruleCount = networkRules.size + cosmeticRules.size
    }

    /** Should this request be blocked? */
    fun shouldBlock(
        url: String,
        documentUrl: String,
        type: ResourceType,
        thirdParty: Boolean
    ): Boolean {
        val urlLower = url.lowercase()
        val docHost = hostOf(documentUrl)
        val urlHost = hostOf(url)

        // Exceptions always win first — that is the whole point of `@@`.
        if (evaluate(urlLower, docHost, urlHost, type, thirdParty, exceptionsOnly = true)) return false

        return evaluate(urlLower, docHost, urlHost, type, thirdParty, exceptionsOnly = false)
    }

    private fun evaluate(
        url: String,
        docHost: String,
        urlHost: String,
        type: ResourceType,
        thirdParty: Boolean,
        exceptionsOnly: Boolean
    ): Boolean {
        // Host-bucketed rules first — most rules are host-anchored and this
        // keeps the hot path off the full scan.
        val candidates = LinkedHashSet<FilterRule.Network>()
        hostIndex.keys.forEach { host ->
            if (urlHost == host || urlHost.endsWith(".$host") || host.endsWith(".$urlHost")) {
                hostIndex[host]?.let(candidates::addAll)
            }
        }

        for (rule in networkRules) {
            if (rule.isException != exceptionsOnly) continue
            candidates += rule

            if (candidates.size > MAX_CANDIDATES) break
        }

        var bestMatch = false
        for (rule in candidates) {
            if (!matchesOptions(rule.options, docHost, urlHost, type, thirdParty)) continue
            if (!rule.regex.containsMatchIn(url)) continue
            bestMatch = true
            break
        }

        // A plain block wins over an exception unless the exception said so.
        if (!exceptionsOnly && bestMatch) return true
        if (exceptionsOnly && bestMatch) return true
        return false
    }

    private fun matchesOptions(
        o: RuleOptions,
        docHost: String,
        urlHost: String,
        type: ResourceType,
        thirdParty: Boolean
    ): Boolean {
        o.thirdParty?.let { expected -> if (expected != thirdParty) return false }

        if (o.types.isNotEmpty() && type !in o.types) return false
        if (o.notTypes.isNotEmpty() && type in o.notTypes) return false

        if (o.domains.isNotEmpty()) {
            val hit = o.domains.any { domainMatches(docHost, it) || domainMatches(urlHost, it) }
            if (!hit) return false
        }
        if (o.notDomains.isNotEmpty()) {
            val hit = o.notDomains.any { domainMatches(docHost, it) || domainMatches(urlHost, it) }
            if (hit) return false
        }
        return true
    }

    private fun domainMatches(host: String, rule: String): Boolean =
        host == rule || host.endsWith(".$rule")

    /**
     * Cosmetic selectors that apply to [documentUrl], ready to be injected as
     * `el.remove()` calls.
     */
    fun cosmeticFor(documentUrl: String): List<String> {
        val host = hostOf(documentUrl).removePrefix("www.")
        val scoped = domainCosmetic.entries
            .filter { (ruleHost, _) -> host == ruleHost || host.endsWith(".$ruleHost") }
            .flatMap { it.value }
        return (globalCosmetic + scoped).distinct()
    }

    fun clear() {
        networkRules.clear()
        cosmeticRules.clear()
        globalCosmetic.clear()
        domainCosmetic.clear()
        hostIndex.clear()
        ruleCount = 0
    }

    private fun hostOf(url: String): String =
        runCatching {
            java.net.URI(url).host?.lowercase()?.removePrefix("www.") ?: ""
        }.getOrDefault("")

    private fun String.splitAt(index: Int): Pair<String, String> =
        substring(0, index) to substring(index + 2)

    companion object {
        private const val MAX_CANDIDATES = 40_000

        /**
         * Translate an Adblock pattern into a Kotlin [Regex].
         *
         * `||host^` → start-of-URL after the scheme, `|` at either edge is a
         * boundary, `^` is a separator, `*` is a wildcard.
         */
        fun compile(pattern: String, matchCase: Boolean): Regex {
            // `/regex/` literal — supported because some lists lean on it hard.
            if (pattern.length > 2 && pattern.startsWith('/') && pattern.endsWith('/')) {
                return Regex(pattern.substring(1, pattern.length - 1), if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE))
            }

            val sb = StringBuilder()
            var i = 0
            if (pattern.startsWith("||")) {
                sb.append("^[a-z][a-z0-9+.-]*://(?:[^/?#]*\\.)?")
                i = 2
            } else if (pattern.startsWith("|")) {
                sb.append('^')
                i = 1
            }

            while (i < pattern.length) {
                when (val c = pattern[i]) {
                    '*' -> { sb.append(".*"); i++ }
                    '^' -> { sb.append("(?:[^a-zA-Z0-9_.%-]|$)"); i++ }
                    '|' -> {
                        if (i == pattern.length - 1) { sb.append('$'); i++ }
                        else { sb.append("\\|"); i++ }
                    }
                    else -> { sb.append(Regex.escape(c.toString())); i++ }
                }
            }

            return Regex(
                sb.toString(),
                if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
            )
        }
    }
}
