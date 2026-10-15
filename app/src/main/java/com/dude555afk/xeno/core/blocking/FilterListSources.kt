package com.dude555afk.xeno.core.blocking

/**
 * Where the default filter lists come from.
 *
 * Bundled as assets so the very first launch blocks properly with no network
 * round-trip, then refreshed from the network in the background once the user
 * is comfortable spending the bytes.
 */
object FilterListSources {

    data class ListSpec(
        val id: String,
        val name: String,
        val remoteUrl: String,
        val assetName: String,
        val enabledByDefault: Boolean
    ) {
        /**
         * Alias for [remoteUrl].
         *
         * The ViewModel's network-refresh path reads `spec.url`, and the
         * bundled-asset path reads `spec.assetName`, so the same property has
         * historically been spelled two ways in this codebase. Keeping both
         * names pointing at one value costs a getter and removes the
         * Unresolved-reference class of bug entirely.
         */
        val url: String get() = remoteUrl
    }

    val defaults = listOf(
        ListSpec(
            id = "easyprivacy",
            name = "EasyPrivacy",
            remoteUrl = "https://easylist.to/easylist/easyprivacy.txt",
            assetName = "filters/easyprivacy.txt",
            enabledByDefault = true
        ),
        ListSpec(
            id = "easylist",
            name = "EasyList",
            remoteUrl = "https://easylist.to/easylist/easylist.txt",
            assetName = "filters/easylist.txt",
            enabledByDefault = true
        ),
        ListSpec(
            id = "ubofilters",
            name = "uBlock filters — badware",
            remoteUrl = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/badware.txt",
            assetName = "filters/badware.txt",
            enabledByDefault = true
        ),
        ListSpec(
            id = "ublockprivacy",
            name = "uBlock filters — privacy",
            remoteUrl = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/privacy.txt",
            assetName = "filters/privacy.txt",
            enabledByDefault = true
        )
    )

    /**
     * The specs worth fetching over the network, in priority order.
     *
     * An alias for [defaults] so the refresh path does not have to care which
     * name the list is filed under. Bundled-list loading stays on `defaults`
     * because it may include entries that are intentionally not refreshed.
     */
    val remote: List<ListSpec> get() = defaults
}
