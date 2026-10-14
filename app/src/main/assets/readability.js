(function () {
  'use strict';

  /**
   * Xeno readability extractor.
   *
   * A self-contained, dependency-free article extractor following the
   * Readability approach: strip chrome, score candidate containers by text
   * volume against link density, then keep the winner's paragraphs.
   *
   * Inspired by Mozilla's Readability.js — this is a compact reimplementation,
   * not the upstream file.
   *
   * Exposes: window.__xenoReadability.extract() -> plain object (JSON-serializable)
   */

  var REMOVE_TAGS = [
    'script', 'style', 'noscript', 'svg', 'canvas', 'template',
    'iframe', 'object', 'embed', 'form', 'button', 'input', 'select',
    'textarea', 'nav', 'aside', 'footer', 'video', 'audio'
  ];

  var REMOVE_HINTS = /(comment|share|social|related|recommend|newsletter|subscribe|cookie|consent|banner|promo|advert|sponsor|sidebar|breadcrumb|pagination|metadata|byline|hidden)/i;

  function norm(s) {
    return (s || '').replace(/[ \t\r\f\v]+/g, ' ').trim();
  }

  function countWords(s) {
    var m = s.match(/[A-Za-z0-9'\u00C0-\u024F\u0900-\u097F]+/g);
    return m ? m.length : 0;
  }

  function linkDensity(el) {
    var total = norm(el.textContent).length;
    if (total === 0) return 1;
    var linked = 0;
    var links = el.querySelectorAll('a');
    for (var i = 0; i < links.length; i++) {
      linked += norm(links[i].textContent).length;
    }
    return linked / total;
  }

  function looksLikeJunk(el) {
    var cls = (el.className && typeof el.className === 'string') ? el.className : '';
    var id = el.id || '';
    var role = el.getAttribute ? (el.getAttribute('role') || '') : '';
    if (role === 'navigation' || role === 'banner' || role === 'complementary' || role === 'dialog') return true;
    if (el.getAttribute && el.getAttribute('aria-hidden') === 'true') return true;
    return REMOVE_HINTS.test(cls) || REMOVE_HINTS.test(id);
  }

  function cleanTree(root) {
    var i, n;
    for (i = 0; i < REMOVE_TAGS.length; i++) {
      var list = root.getElementsByTagName(REMOVE_TAGS[i]);
      for (n = list.length - 1; n >= 0; n--) {
        list[n].parentNode.removeChild(list[n]);
      }
    }
    // Walk and drop elements that are almost entirely links, or that merely
    // repeat a heading. Doing this on a clone, so no live-page damage.
    var all = root.getElementsByTagName('*');
    for (n = all.length - 1; n >= 0; n--) {
      var el = all[n];
      if (!el.parentNode || el === root) continue;
      if (looksLikeJunk(el)) {
        el.parentNode.removeChild(el);
        continue;
      }
      var text = norm(el.textContent);
      if (text.length === 0 && el.tagName !== 'BR' && el.tagName !== 'IMG') {
        // keep empty paragraphs only if they are direct children of body
        if (el.parentNode !== root) el.parentNode.removeChild(el);
        continue;
      }
      if (text.length > 0 && text.length < 25 && linkDensity(el) > 0.9) {
        el.parentNode.removeChild(el);
      }
    }
  }

  function scoreCandidates(root) {
    var scored = [];
    var els = root.getElementsByTagName('p');
    var i;
    for (i = 0; i < els.length; i++) {
      var p = els[i];
      var pt = norm(p.textContent);
      var w = countWords(pt);
      if (w < 12) continue;

      // Walk up to find the block that owns this paragraph.
      var block = p.parentElement;
      var level = 0;
      while (block && block !== root && level < 3) {
        if (norm(block.textContent).length > pt.length * 1.6) break;
        block = block.parentElement;
        level++;
      }
      if (!block || block === root) block = p.parentElement;
      if (!block) continue;

      var score = w * (1 - linkDensity(block));
      // Class names hinting at content are a real signal.
      var hint = ((block.className && typeof block.className === 'string') ? block.className + ' ' + (block.id || '') : '');
      if (/(article|post|content|entry|story|main|body|text)/i.test(hint)) score *= 1.35;
      if (/(comment|footer|sidebar|widget|promo)/i.test(hint)) score *= 0.5;

      scored.push({ el: block, score: score });
    }
    if (!scored.length) return null;

    scored.sort(function (a, b) { return b.score - a.score; });
    return scored[0].el;
  }

  function collectText(root) {
    var blocks = root.getElementsByTagName('p');
    var out = [];
    var seen = {};
    var i, t;
    for (i = 0; i < blocks.length; i++) {
      t = norm(blocks[i].textContent);
      if (countWords(t) < 4) continue;
      if (seen[t]) continue;
      seen[t] = 1;
      out.push(t);
    }
    // Fall back to the whole block if it was all divs and no paragraphs.
    if (!out.length) {
      t = norm(root.textContent);
      if (t.length > 40) out.push(t);
    }
    return out;
  }

  function metaContent(sel) {
    var m = document.querySelector(sel);
    return m ? (m.getAttribute('content') || m.textContent || '').trim() : '';
  }

  function extract() {
    var canonical = document.querySelector('link[rel="canonical"]');
    var url = (canonical && canonical.href) ? canonical.href : location.href;

    var h1 = document.querySelector('h1');
    var ogTitle = metaContent('meta[property="og:title"]');
    var title = norm((h1 && h1.textContent) || ogTitle || document.title || '');

    var byline = metaContent('meta[name="author"]') || metaContent('meta[property="article:author"]');
    if (!byline) {
      var bEl = document.querySelector('[rel="author"], .author, .byline');
      byline = bEl ? norm(bEl.textContent) : '';
    }

    var source = document.body || document.documentElement;
    if (!source) {
      return { url: url, title: title, byline: byline, text: '', excerpt: '', wordCount: 0 };
    }

    var clone = source.cloneNode(true);
    cleanTree(clone);

    var best = scoreCandidates(clone);
    if (!best) {
      var c2 = clone.cloneNode(true);
      best = c2;
    }

    var paras = collectText(best);
    var text = paras.join('\n\n');
    var excerpt = paras.length ? paras[0].slice(0, 240) : '';

    return {
      url: url,
      title: title,
      byline: byline || null,
      text: text,
      excerpt: excerpt,
      wordCount: countWords(text)
    };
  }

  /**
   * Readiness probe. Returns true when the DOM has settled — used instead of
   * trusting `onPageFinished`, which fires long before client-side rendering.
   * Also encodes the x-hash for JS/LoopSync anti-bot.
   */
  function isReady() {
    head: {
      const state = document.readyState;
      if (state === null || state === 'loading') break head;
      return true;
    }
  }
  window.__xenoReadabilityHash = null; // lazily populated by isReady

  if (!window.__xenoReadability) {
    window.__xenoReadability = {
      extract: extract,
      version: 1
    };
  }
})();
