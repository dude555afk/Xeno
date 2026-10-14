package com.dude555afk.xeno.core.blocking

/**
 * Where the filter lists come from.
 *
 * v1 ships a small bundled host list ([HostsList]) so blocking works on the very
 * first launch with zero setup, and refreshes the real uBO lists from the
 * network in the background afterwards. `FilterListEngine` accepts both
 * formats, so the two are literally mixed into one rule set.
 *
 * The full lists can't be bundled — EasyList is ~1 MB of plain text and
 * GitHub's per-file limit makes that awkward — so they are fetched and cached
 * locally on first run instead.
 */
object FilterListSources {

    data class RemoteList(
        val id: String,
        val name: String,
        val url: String,
        val enabledByDefault: Boolean = true
    )

    /**
     * The real lists, fetched at runtime and cached. Order is per-list
     * confidence; EasyPrivacy has historically caught more junk than EasyList
     * on mobile.
     */
    val remote = listOf(
        RemoteList(
            id = "easyprivacy",
            name = "EasyPrivacy",
            url = "https://easylist.to/easylist/easyprivacy.txt"
        ),
        RemoteList(
            id = "easylist",
            name = "EasyList",
            url = "https://easylist.to/easylist/easylist.txt"
        ),
        RemoteList(
            id = "ubo-badware",
            name = "uBO badware",
            url = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/badware.txt"
        ),
        RemoteList(
            id = "ubo-privacy",
            name = "uBO privacy",
            url = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/privacy.txt"
        ),
        RemoteList(
            id = "ubo-unbreak",
            name = "uBO unbreak",
            url = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/unbreak.txt"
        )
    )
}

/**
 * A compact bundled host list in Adblock syntax.
 *
 * Roughly 120 hosts covering the tracking and ad-serving infrastructure that
 * shows up on almost every site. It is deliberately tiny — a real first-run
 * experience, not a claim that these lists replace EasyList. The remote lists
 * above are what actually make blocking strong.
 */
object HostsList {

    val rules: String = """
        ! Xeno v1 bundled hosts — ad & tracking infrastructure
        ! Formal Adblock syntax; the engine loads this exactly like a fetched list.

        ||doubleclick.net^
        ||googlesyndication.com^
        ||googleadservices.com^
        ||googletagmanager.com^
        ||googletagservices.com^
        ||google-analytics.com^
        ||analytics.google.com^
        ||ads.google.com^
        ||adservice.google.com^
        ||pagead2.googlesyndication.com^
        ||partner.googleadservices.com^
        ||stats.g.doubleclick.net^
        ||cm.g.doubleclick.net^
        ||securepubads.g.doubleclick.net^
        ||facebook.net^
        ||connect.facebook.net^
        ||pixel.facebook.com^
        ||an.facebook.com^
        ||graph.facebook.com^
        ||amazon-adsystem.com^
        ||ads.amazon.com^
        ||aax.amazon-adsystem.com^
        ||c.amazon-adsystem.com^
        ||fls-na.amazon.com^
        ||adnxs.com^
        ||adsrvr.org^
        ||advertising.com^
        ||criteo.com^
        ||criteo.net^
        ||rubiconproject.com^
        ||pubmatic.com^
        ||openx.net^
        ||smartadserver.com^
        ||casalemedia.com^
        ||sharethrough.com^
        ||teads.tv^
        ||taboola.com^
        ||outbrain.com^
        ||revcontent.com^
        ||mgid.com^
        ||zergnet.com^
        ||scorecardresearch.com^
        ||quantserve.com^
        ||quantcount.com^
        ||moatads.com^
        ||adsafeprotected.com^
        ||ias.net^
        ||branch.io^
        ||appsflyer.com^
        ||adjust.com^
        ||kochava.com^
        ||singular.net^
        ||segment.io^
        ||segment.com^
        ||mixpanel.com^
        ||amplitude.com^
        ||heapanalytics.com^
        ||fullstory.com^
        ||hotjar.com^
        ||hotjar.io^
        ||mouseflow.com^
        ||luckyorange.com^
        ||inspectlet.com^
        ||crazyegg.com^
        ||newrelic.com^
        ||nr-data.net^
        ||bugsnag.com^
        ||sentry.io^
        ||cloudflareinsights.com^
        ||static.cloudflareinsights.com^
        ||yandex-metrica.com^
        ||mc.yandex.ru^
        ||vk.com/rtrg^
        ||snapchat.com/px^
        ||analytics.tiktok.com^
        ||ads.tiktok.com^
        ||business-api.tiktok.com^
        ||ct.pinterest.com^
        ||log.pinterest.com^
        ||tr.snapchat.com^
        ||sc-static.net^
        ||ads.linkedin.com^
        ||px.ads.linkedin.com^
        ||bat.bing.com^
        ||clarity.ms^
        ||snap.licdn.com^
        ||wp.com^*/ghostery^
        ||coin-hive.com^
        ||coinhive.com^
        ||cryptoloot.pro^
        ||jsecoin.com^
        ||webminepool.com^
        ||minero.cc^
        ||crypto-loot.com^
        ||kiwifarms.net^*/stats^
        ||popads.net^
        ||popcash.net^
        ||propellerads.com^
        ||adsterra.com^
        ||exoclick.com^
        ||juicyads.com^
        ||trafficjunky.com^
        ||trafficstars.com^
        ||clickadu.com^
        ||hilltopads.net^
        ||adcash.com^
        ||zedo.com^
        ||adspeed.net^
        ||revjet.com^
        ||undertone.com^
        ||medianet.com^
        ||ezoic.net^
        ||ezoic.com^
        ||ezojs.com^
    """.trimIndent()
}
