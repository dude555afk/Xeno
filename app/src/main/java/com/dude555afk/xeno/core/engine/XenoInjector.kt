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
        redirectPopups: Boolean,
        fingerprintingProtection: Boolean = false
    ): String? {
        val parts = mutableListOf<String>()

        if (cosmeticSelectors.isNotEmpty()) {
            val quoted = cosmeticSelectors.joinToString(",") { jsonString(it) }
            parts += """
                (function(){
                  try {
                    var s = [$quoted];
                    var removeMatches = function(root) {
                      for (var i = 0; i < s.length; i++) {
                        try {
                          if (root && root.matches && root.matches(s[i])) root.remove();
                          var els = (root || document).querySelectorAll(s[i]);
                          for (var j = 0; j < els.length; j++) els[j].remove();
                        } catch (e) {}
                      }
                    };
                    removeMatches(document);
                    // Many sites add ad placeholders after initial render. Keep
                    // cosmetic rules active for nodes inserted after injection.
                    if (document.documentElement && window.MutationObserver) {
                      var observer = new MutationObserver(function(records) {
                        for (var r = 0; r < records.length; r++) {
                          var nodes = records[r].addedNodes || [];
                          for (var n = 0; n < nodes.length; n++) {
                            if (nodes[n].nodeType === 1) removeMatches(nodes[n]);
                          }
                        }
                      });
                      observer.observe(document.documentElement, {childList:true, subtree:true});
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

        // Best-effort reduction of a few high-entropy WebView signals. This is
        // not full anti-fingerprinting: page scripts may run before the hook,
        // and WebView doesn't expose an isolated-world injection API here.
        if (fingerprintingProtection) {
            parts += """
                (function(){
                  if (window.__xenoFingerprintGuards) return;
                  window.__xenoFingerprintGuards = true;
                  try {
                    var np = window.Navigator && Navigator.prototype;
                    if (np) {
                      try { Object.defineProperty(np, 'hardwareConcurrency', {get:function(){return 4;}, configurable:true}); } catch(e) {}
                      try { if ('deviceMemory' in np) Object.defineProperty(np, 'deviceMemory', {get:function(){return 4;}, configurable:true}); } catch(e) {}
                    }
                    var patchGl = function(proto) {
                      if (!proto || !proto.getParameter) return;
                      var original = proto.getParameter;
                      proto.getParameter = function(p) {
                        if (p === 37445) return 'Generic';
                        if (p === 37446) return 'Generic WebGL Renderer';
                        return original.apply(this, arguments);
                      };
                    };
                    try { patchGl(window.WebGLRenderingContext && WebGLRenderingContext.prototype); } catch(e) {}
                    try { patchGl(window.WebGL2RenderingContext && WebGL2RenderingContext.prototype); } catch(e) {}
                  } catch(e) {}
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
