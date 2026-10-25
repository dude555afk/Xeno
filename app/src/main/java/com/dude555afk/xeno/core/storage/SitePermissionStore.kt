package com.dude555afk.xeno.core.storage

import android.content.Context
import com.dude555afk.xeno.core.model.SiteCapability
import com.dude555afk.xeno.core.model.SitePermissions

/**
 * Per-origin settings. Exact hosts are used deliberately: allowing camera on
 * one host must not implicitly allow a sibling subdomain.
 */
class SitePermissionStore(context: Context, privateMode: Boolean) {
    private val prefs = context.getSharedPreferences(
        if (privateMode) PRIVATE_PREFS else PREFS,
        Context.MODE_PRIVATE
    )

    fun read(host: String, defaultThirdPartyCookies: Boolean = false): SitePermissions {
        if (host.isBlank()) return SitePermissions(thirdPartyCookiesEnabled = defaultThirdPartyCookies)
        return SitePermissions(
            javaScriptEnabled = get(host, SiteCapability.JAVASCRIPT, true),
            thirdPartyCookiesEnabled = get(host, SiteCapability.THIRD_PARTY_COOKIES, defaultThirdPartyCookies),
            allowLocation = get(host, SiteCapability.LOCATION, false),
            allowCamera = get(host, SiteCapability.CAMERA, false),
            allowMicrophone = get(host, SiteCapability.MICROPHONE, false)
        )
    }

    fun isAllowed(host: String, capability: SiteCapability, default: Boolean = false): Boolean =
        host.isNotBlank() && get(host, capability, default)

    fun set(host: String, capability: SiteCapability, allowed: Boolean) {
        if (host.isBlank()) return
        prefs.edit().putBoolean(key(host, capability), allowed).apply()
    }

    fun clear(): Boolean = prefs.edit().clear().commit()

    private fun get(host: String, capability: SiteCapability, default: Boolean): Boolean =
        prefs.getBoolean(key(host, capability), default)

    private fun key(host: String, capability: SiteCapability): String =
        host.trim().lowercase().trimEnd('.') + "." + capability.name.lowercase()

    private companion object {
        const val PREFS = "xeno_site_permissions"
        const val PRIVATE_PREFS = "xeno_private_site_permissions"
    }
}
