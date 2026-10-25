package com.dude555afk.xeno.core.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XenoInjectorTest {

    @Test
    fun popupTamingIsIncludedForBrowserPages() {
        val script = XenoInjector.script(
            documentUrl = "https://example.com",
            cosmeticSelectors = emptyList(),
            redirectPopups = true
        )

        assertNotNull(script)
        assertTrue(script!!.contains("window.open"))
        assertTrue(script.contains("__xenoPopupTamed"))
    }

    @Test
    fun fingerprintProtectionNormalizesSelectedHighEntropySignals() {
        val script = XenoInjector.script(
            documentUrl = "https://example.com",
            cosmeticSelectors = emptyList(),
            redirectPopups = false,
            fingerprintingProtection = true
        )

        assertNotNull(script)
        assertTrue(script!!.contains("hardwareConcurrency"))
        assertTrue(script.contains("deviceMemory"))
        assertTrue(script.contains("Generic WebGL Renderer"))
    }

    @Test
    fun fingerprintProtectionIsOmittedWhenDisabledAndNothingElseIsNeeded() {
        val script = XenoInjector.script(
            documentUrl = "https://example.com",
            cosmeticSelectors = emptyList(),
            redirectPopups = false,
            fingerprintingProtection = false
        )

        assertFalse(script != null)
    }

    @Test
    fun cosmeticSelectorsAreEscapedAsJavaScriptStrings() {
        val script = XenoInjector.script(
            documentUrl = "https://example.com",
            cosmeticSelectors = listOf(".ad", "a[href='x']"),
            redirectPopups = false,
            fingerprintingProtection = false
        )

        assertNotNull(script)
        assertTrue(script!!.contains("document.querySelectorAll"))
        assertTrue(script.contains("a[href="))
    }
}
