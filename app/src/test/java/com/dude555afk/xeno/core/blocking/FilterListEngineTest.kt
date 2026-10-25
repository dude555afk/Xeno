package com.dude555afk.xeno.core.blocking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterListEngineTest {

    @Test
    fun hostAnchoredRulesMatchExactAndSubdomainHosts() {
        val engine = FilterListEngine()
        engine.load("||ads.example^")

        assertTrue(engine.shouldBlock(
            "https://ads.example/banner.js",
            "https://news.test/",
            ResourceType.SCRIPT,
            thirdParty = true
        ))
        assertTrue(engine.shouldBlock(
            "https://cdn.ads.example/banner.js",
            "https://news.test/",
            ResourceType.SCRIPT,
            thirdParty = true
        ))
    }

    @Test
    fun thirdPartyOptionsRespectInitiatingDocumentContext() {
        val engine = FilterListEngine()
        engine.load("||metrics.example^$third-party")

        assertTrue(engine.shouldBlock(
            "https://metrics.example/pixel",
            "https://shop.other/",
            ResourceType.IMAGE,
            thirdParty = true
        ))
        assertFalse(engine.shouldBlock(
            "https://metrics.example/pixel",
            "https://shop.other/",
            ResourceType.IMAGE,
            thirdParty = false
        ))
    }

    @Test
    fun allowExceptionOverridesMatchingNetworkBlock() {
        val engine = FilterListEngine()
        engine.load("||ads.example^\n@@||ads.example/allowed.js")

        assertTrue(engine.shouldBlock(
            "https://ads.example/blocked.js",
            "https://news.test/",
            ResourceType.SCRIPT,
            thirdParty = true
        ))
        assertFalse(engine.shouldBlock(
            "https://ads.example/allowed.js",
            "https://news.test/",
            ResourceType.SCRIPT,
            thirdParty = true
        ))
    }

    @Test
    fun cosmeticExceptionsRemoveSelectorsOnlyOnTheirDomain() {
        val engine = FilterListEngine()
        engine.load("##.ad\nexample.com##.sponsor\nexample.com#@#.ad")

        val onExample = engine.cosmeticFor("https://www.example.com/article")
        assertFalse(".ad should be excepted for example.com", ".ad" in onExample)
        assertTrue(".sponsor should remain", ".sponsor" in onExample)

        val onOther = engine.cosmeticFor("https://news.test/article")
        assertTrue(".ad should remain globally elsewhere", ".ad" in onOther)
    }

    @Test
    fun identicalFilterTextIsNotLoadedTwice() {
        val engine = FilterListEngine()
        val rules = "||ads.example^\n||track.example^"
        engine.load(rules)
        val firstCount = engine.ruleCount

        engine.load(rules)

        assertTrue(firstCount > 0)
        assertTrue("duplicate source should not duplicate rules", engine.ruleCount == firstCount)
    }
}
