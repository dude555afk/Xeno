package com.dude555afk.xeno.core.blocking

/**
 * One parsed line of a filter list.
 *
 * [Network.literal] is populated only when the pattern is a plain substring
 * with no metacharacters, which is the common case. Checking `String.contains`
 * on a literal is roughly an order of magnitude cheaper than running a
 * [Regex], and this runs once per subresource request.
 */
sealed interface FilterRule {

    /** A network rule — blocks or allows a request. */
    data class Network(
        val pattern: String,
        val regex: Regex,
        val isException: Boolean,
        val options: RuleOptions,
        val literal: String? = null
    ) : FilterRule

    /** A cosmetic rule — `##.selector`, applied via JS after load. */
    data class Cosmetic(
        val domains: Set<String>,
        val selectors: List<String>,
        val isException: Boolean
    ) : FilterRule
}
