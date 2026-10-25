package com.dude555afk.xeno.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderConfigTest {

    @Test
    fun anonymousKiloOnlyAllowsItsConfiguredFreeModel() {
        assertTrue(ProviderPresets.Kilo.allowsAnonymousFreeModel)
        assertFalse(
            ProviderPresets.Kilo.copy(model = "paid-model").allowsAnonymousFreeModel
        )
        assertFalse(
            ProviderPresets.Kilo.copy(baseUrl = "https://example.com/api/v1").allowsAnonymousFreeModel
        )
    }

    @Test
    fun catalogueMarksAnonymousFreeModelIds() {
        assertTrue(ModelInfo(id = "kilo-auto/free").isFree)
        assertTrue(ModelInfo(id = "provider/model:free").isFree)
        assertFalse(ModelInfo(id = "provider/paid-model").isFree)
    }

    @Test
    fun localProviderAllowsNoKeyOnlyOnLoopbackOrPrivateLan() {
        assertTrue(ProviderPresets.Local.copy(baseUrl = "http://127.0.0.1:11434/v1").allowsAnonymousFreeModel)
        assertTrue(ProviderPresets.Local.copy(baseUrl = "http://192.168.1.20:1234/v1").allowsAnonymousFreeModel)
        assertFalse(ProviderPresets.Local.copy(baseUrl = "http://public.example.com/v1").allowsAnonymousFreeModel)
        assertFalse(ProviderPresets.Local.copy(baseUrl = "https://public.example.com/v1").allowsAnonymousFreeModel)
    }

    @Test
    fun customPublicProvidersRequireAnApiKey() {
        val custom = ProviderConfig(
            id = "custom",
            displayName = "Custom",
            baseUrl = "https://provider.example/v1",
            apiKey = "",
            model = "model"
        )
        assertFalse(custom.allowsAnonymousFreeModel)
    }
}
