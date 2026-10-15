package com.dude555afk.xeno.core.engine

/**
 * Injected-once helpers for every Xeno WebView.
 *
 * Two jobs, both of which have to run from inside the page:
 *
 * 1. **Cosmetic filtering.** `el.remove()` for the selectors the filter engine
 *    matched. This is the approximation that covers the visible damage
 *    `$cosmetic` rules describe, given that `shouldInterceptRequest` gives us
 *    no body access.
 * 2. **Popup taming.** `window.open` is redirected to a same-tab navigation.
 *    Implementing `onCreateWindow` properly means a second live WebView, which
 *    is the one thing the 2-session cap in [SessionPool] exists to prevent.
 */
object XenoInjector {

    /**
     * Build the page-context script for [documentUrl].
     *
     * Returns null when there is nothing to inject, which is the common case —
     * no cosmetic rules and no override to install means zero JS cost.
     */
    fun script(
        documentUrl: String,
        cosmeticSelectors: List<String>,
        redirectPopups: Boolean
    ): String? {
        val parts = mutableListOf<String>()

        if (cosmeticSelectors.isNotEmpty()) {
            val quoted = cosmeticSelectors.joinToString(",") { jsonString(it) }
            parts += """
                (function(){
                  try {
                    var s = [$quoted];
                    for (var i = 0; i < s.length; i++) {
                      var els = document.querySelectorAll(s[i]);
                      for (var j = 0; j < els.length; j++) els[j].remove();
                    }
                  } catch (e) {}
                })();
            """.trimIndent()
        }

        if (redirectPopups) {
            parts += """
                (function(){
                  if (window.__xenoPopupTamed) return;
                  window.__xenoPopupTamed = true;
                  window.open = function(u) {
                    try { window.location.href = String(u); } catch (e) {}
                    return null;
                  };
                })();
            """.trimIndent()
        }

        if (parts.isEmpty()) return null
        return parts.joinToString("\n")
    }

    private fun jsonString(raw: String): String {
        val escaped = raw
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
        return "'$escaped'"
    }
}
