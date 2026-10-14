# Architecture

## The answer engine pipeline

The flagship flow. Everything else in the app is in service of this.

```
query
  │
  ├─ 1. INTENT
  │     URL? → navigate.
  │     Question? → route to engine. (also "?<query>" or AI key)
  │
  ├─ 2. SEARCH
  │     Offscreen WebView loads duckduckgo.com/?q=<query>
  │     Inject JS → scrape result links + titles
  │     Returns N candidate URLs.
  │     Fallbacks: SearXNG instance → Mojeek → DDG Lite.
  │
  ├─ 3. FETCH
  │     Top 4 URLs fetched in parallel.
  │     Readability.js injected per page → clean article text.
  │
  ├─ 4. PRUNE
  │     Drop paywalls, cookie-junk pages, near-duplicates.
  │     Cap ~2000 tokens per source, ~8k total.
  │
  ├─ 5. SYNTHESIZE
  │     Kilo Gateway, sources + question in context.
  │     Prompt forces inline citations [1][2].
  │     SSE stream, rendered token-by-token.
  │
  └─ 6. RENDER
        Source cards appear first (fast, favicon + title + domain).
        Answer streams beneath them with a blinking caret.
        Tap a citation → opens that source in-app.
```

Source cards land before the answer because fetching is ~1-2s and
generation is ~4s. Perceived speed matters more than actual speed.

## Page summarizer pipeline

```
page → Readability.js → clean text
     → map-reduce chunking if long
     → Kilo (structured prompt, see docs/PROMPTS.md)
     → SSE stream → typewriter render in bottom sheet
```

Structured output, always — `TL;DR` / `KEY POINTS` / `NUMBERS`. Free-form
summaries read like mush and get apps uninstalled.

Cached per URL + content-hash. Summarize once, never burn quota twice.

## Module layout

```
app/                  Compose app, navigation, entry point
core/
  engine/             WebView factory, session pool, lifecycle
  peek/               Overlay WebView + draggable shell
  reader/             Readability.js injector + result model
  fetch/              Offscreen scraper + parallel fetch + rank
  blocking/           adblock-rust bindings + filter list loader
  ai/                 Provider layer (OpenAI-compatible), stream parsing,
                      Kilo preset, BYOK screen
  embed/              MiniLM ONNX via sherpa-onnx
  summarize/          Prompt construction + chunking + TextRank fallback
  storage/            Room: tabs, spaces, history, summary cache
ui/
  theme/              tokens, typography, spring specs
  spaces/
  tabstack/
  cmdbar/             fuzzy palette + AI sheet host
  answersheet/        streaming render + source cards
```

## Known constraints

- **RAM is the budget.** Each WebView session is 150-250 MB. Hard cap of
  2 live sessions. Everything else gets `onPause`d and destroyed.
- **No extension support** on system WebView. This is the accepted
  tradeoff for a ~15 MB app; adblock-rust + uBO lists covers ~95% of
  the same outcome.
- **JS injection can't reach cross-origin iframes.** Boost-style CSS
  restyling will not fix ads inside iframes.
- **Peek interception can break SPAs.** Hash-routed and JS-driven links
  may fight it. Fallback: offer "open anyway" after a timeout.

## Hard rules

1. `reader` must land before `ai` — the extractor is a hard dependency.
2. Never ship a dead screen. Every AI path degrades to TextRank offline.
3. Cache aggressively — Kilo's free tier is rate-limited and shared.
