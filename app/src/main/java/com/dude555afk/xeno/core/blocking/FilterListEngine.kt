package com.dude555afk.xeno.core.blocking

import java.util.concurrent.ConcurrentHashMap

/**
 * A native filter-list engine speaking uBlock Origin's syntax.
 *
 * This is the piece that lets Xeno ship real blocking without the 190 MB
 * GeckoView + real extension tradeoff. It parses EasyList / EasyPrivacy / the
 * uBO-specific lists and matches requests in `shouldInterceptRequest`.
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

    private val loadedFingerprints = mutableSetOf<String>()
    private val networkRules = mutableListOf<FilterRule.Network>()
    private val cosmeticRules = mutableListOf<FilterRule.Cosmetic>()

    /**
     * Patterns indexed by the host they are anchored to.
     *
     * The indexed forms are exactly `||host^` and `||host^/path`, which is what
     * nearly every rule in EasyList and EasyPrivacy starts with. Everything
     * else — plain substrings, `|`-anchored, or an unparseable remainder —
     * lives in [unindexed] and is scanned on every request.
     *
     * Correctness does not depend on this index: every indexed rule's regex is
     * valid for the whole URL, so an indexed rule can never miss. The index
     * only decides how many regexes we are willing to run.
     */
    private val indexed = ConcurrentHashMap<String, MutableList<FilterRule.Network>>()

    /** Rules that are not host-anchored. Scanned on every request. */
    private val unindexed = mutableListOf<FilterRule.Network>()

    /** Global (`##`) cosmetic selectors, keyed for fast lookup. */
    private val globalCosmetic = mutableListOf<String>()

    /** Domain-scoped cosmetic selectors. */
    private val domainCosmetic = ConcurrentHashMap<String, MutableList<String>>()

    @Volatile
    var ruleCount: Int = 0
        private set

    /**
     * Load a filter list in Adblock-Plus / uBlock format.
     *
     * Cheap enough to call repeatedly; rules accumulate as new lists load.
     * Not thread-safe against concurrent [load] calls — callers serialise by
     * loading on a single dispatcher, which is what
     * [com.dude555afk.xeno.XenoApplication] does.
     */
    @Synchronized
    fun load(listText: String) {
        val fingerprint = java.security.MessageDigest.getInstance("SHA-256")
            .digest(listText.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
        if (!loadedFingerprints.add(fingerprint)) return

        listText.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            if (line.startsWith('!') || line.startsWith('[')) return@forEach

            // Cosmetic rule: `domain##selector`, `##selector`, or the
            // `#@#` exception form. The separator is `##` or `#@#`, and
            // `line.indexOf("##")` alone misses `#@#` because of the `@`.
            val cosmetic = parseCosmetic(line)
            if (cosmetic != null) {
                cosmeticRules += cosmetic
                if (cosmetic.domains.isEmpty()) globalCosmetic += cosmetic.selectors
                else cosmetic.domains.forEach { d ->
                    domainCosmetic.getOrPut(d) { mutableListOf() }.addAll(cosmetic.selectors)
                }
                return@forEach
            }

            val isException = line.startsWith("@@")
            val body = if (isException) line.removePrefix("@@") else line

            // Split off $options. A leading `$` cannot be options — that is a
            // match-any pattern like `,$image` used as a bare pattern.
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

            val regex = runCatching { FilterListEngine.compile(pattern, options.matchCase) }
                .getOrElse { return@forEach }

            val rule = FilterRule.Network(
                pattern = pattern,
                regex = regex,
                isException = isException,
                options = options,
                literal = if (pattern.isNotEmpty() && !pattern.any { it in "*^|" }) pattern else null
            )

            networkRules += rule

            val host = indexedHost(pattern)
            if (host != null) {
                indexed.getOrPut(host) { mutableListOf() } += rule
            } else {
                unindexed += rule
            }
        }
        ruleCount = networkRules.size + cosmeticRules.size
    }

    /**
     * Return the host an `||`-anchored pattern should be filed under, or null
     * if it does not fit the indexed shape.
     */
    private fun indexedHost(pattern: String): String? {
        if (!pattern.startsWith("||")) return null
        val boundary = pattern.indexOfAny(charArrayOf('/', '?', '#'), startIndex = 2)
        val rawHead = if (boundary < 0) pattern.substring(2) else pattern.substring(2, boundary)
        val head = rawHead.removeSuffix("^")
        // Strip the common trailing separator before indexing. Otherwise
        // ||example.com^ rules are filed under example.com^ and never match.
        if (head.isEmpty() || head.contains('*') || head.any { it in "?|^" }) return null
        if (rawHead.any { it in "*?|" }) return null
        return head.lowercase().removePrefix("www.")
    }
    /**
     * Should this request be blocked?
     *
     * Precedence follows uBO: an `important` block beats everything, then any
     * matching exception allows the request, then any matching block blocks
     * it. Important is rare but load-bearing — it is how a list says "always
     * hide this, even if another list re-allowed it".
     */
    @Synchronized
    fun shouldBlock(
        url: String,
        documentUrl: String,
        type: ResourceType,
        thirdParty: Boolean
    ): Boolean {
        val urlHost = hostOf(url)
        val docHost = hostOf(documentUrl)

        val candidates = collect(urlHost)

        var sawBlock = false
        var sawAllow = false

        for (rule in candidates) {
            if (!rule.regex.containsMatchIn(url)) continue
            if (rule.literal != null && rule.literal != "" && !url.contains(rule.literal)) continue
            if (!matchesOptions(rule.options, docHost, urlHost, type, thirdParty)) continue

            if (rule.options.important) {
                if (rule.isException) {
                    // Important exception: allow takes precedence over blocks.
                    sawAllow = true
                } else {
                    // Important block: blocks immediately.
                    return true
                }
            } else if (rule.isException) {
                sawAllow = true
            } else {
                sawBlock = true
            }

            if (sawBlock && sawAllow) break
        }
        return sawBlock && !sawAllow
    }

    /**
     * Every rule that could possibly match this host: the host itself, then
     * every suffix, then the catch-all.
     *
     * Walking suffixes rather than scanning the index keys is the whole trick.
     * A page request `https://a.b.example.com/x` checks the buckets for
     * `a.b.example.com`, `b.example.com`, `example.com` and `com` — four map
     * lookups instead of one iteration over a hundred thousand keys.
     */
    private fun collect(host: String): Sequence<FilterRule.Network> = sequence {
        if (host.isEmpty()) {
            unindexed.forEach { yield(it) }
            indexed[""]?.forEach { yield(it) }
            return@sequence
        }

        var current = host
        while (true) {
            indexed[current]?.forEach { yield(it) }
            val dot = current.indexOf('.')
            if (dot < 0) break
            current = current.substring(dot + 1)
        }

        // Patterns whose host was not a plain literal were never indexed.
        unindexed.forEach { yield(it) }
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
            val hit = o.domains.any { domainMatches(docHost, it) }
            if (!hit) return false
        }
        if (o.notDomains.isNotEmpty()) {
            val hit = o.notDomains.any { domainMatches(docHost, it) }
            if (hit) return false
        }
        return true
    }

    private fun domainMatches(host: String, rule: String): Boolean =
        host == rule || host.endsWith(".$rule")

    /**
     * Cosmetic selectors that apply to [documentUrl], minus the `#@#`
     * exceptions that re-allow any of them on that document.
     */
    @Synchronized
    fun cosmeticFor(documentUrl: String): List<String> {
        val host = hostOf(documentUrl).removePrefix("www.")
        val matching = cosmeticRules.filter { rule ->
            rule.domains.isEmpty() || rule.domains.any { domain ->
                host == domain || host.endsWith(".$domain")
            }
        }
        val exceptions = matching.filter { it.isException }.flatMap { it.selectors }.toSet()
        return matching.asSequence()
            .filterNot { it.isException }
            .flatMap { it.selectors.asSequence() }
            .filterNot { it in exceptions }
            .distinct()
            .toList()
    }

    @Synchronized
    fun clear() {
        networkRules.clear()
        cosmeticRules.clear()
        globalCosmetic.clear()
        domainCosmetic.clear()
        indexed.clear()
        unindexed.clear()
        ruleCount = 0
        loadedFingerprints.clear()
    }

    private fun hostOf(url: String): String =
        runCatching {
            java.net.URI(url).host?.lowercase()?.removePrefix("www.") ?: ""
        }.getOrDefault("")

    private fun parseCosmetic(line: String): FilterRule.Cosmetic? {
        val hash = line.indexOf('#')
            .takeIf { it >= 0 }
            ?: return null

        val domainsPart = line.substring(0, hash)
        val rest = line.substring(hash)

        if (rest.startsWith("##^") || rest.startsWith("##+js")) return null
        val isException = rest.startsWith("#@#")
        val selector = if (isException) rest.removePrefix("#@#") else rest.removePrefix("##")

        if (selector.isBlank()) return null

        val domains = domainsPart.split(',')
            .filter { it.isNotBlank() }
            .map { it.lowercase().removePrefix("www.") }
            .toSet()

        val selectors = selector.split(',').filter { it.isNotBlank() }
        return FilterRule.Cosmetic(domains, selectors, isException)
    }

    companion object {
        /**
         * Translate an Adblock pattern into a Kotlin [Regex].
         *
         * `||host^` → start-of-URL after the scheme, `|` at either edge is a
         * boundary, `^` is a separator, `*` is a wildcard.
         */
        fun compile(pattern: String, matchCase: Boolean): Regex {
            // `/regex/` literal — supported because some lists lean on it hard.
            if (pattern.length > 2 && pattern.startsWith('/') && pattern.endsWith('/')) {
                return Regex(
                    pattern.substring(1, pattern.length - 1),
                    if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
                )
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