package com.dude555afk.xeno.core.blocking

/**
 * A compact bundled host list in Adblock syntax.
 *
 * Roughly 120 hosts covering the tracking and ad-serving infrastructure that
 * shows up on almost every site. It is deliberately tiny — a real first-run
 * experience, not a claim that these lists replace EasyList. The remote lists
 * in [FilterListSources] are what actually make blocking strong.
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
