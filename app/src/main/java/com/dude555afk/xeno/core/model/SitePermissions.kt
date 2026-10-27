package com.dude555afk.xeno.core.model

/** Explicit site-level choices. Sensitive capabilities default to denied. */
enum class SiteCapability {
    JAVASCRIPT,
    THIRD_PARTY_COOKIES,
    LOCATION,
    CAMERA,
    MICROPHONE
}

data class SitePermissions(
    val javaScriptEnabled: Boolean = true,
    val thirdPartyCookiesEnabled: Boolean = false,
    val allowLocation: Boolean = false,
    val allowCamera: Boolean = false,
    val allowMicrophone: Boolean = false
)
