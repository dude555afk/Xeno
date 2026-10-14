# Roadmap

## The dependency truth

`reader` feeds **everything** AI. `engine` feeds `reader`. The order is forced,
not a preference:

```
skeleton → engine → reader → summarizer → answer engine → Arc feel → polish
```

Every milestone ends with something runnable on a real phone.

---

## Milestones

| # | Milestone | You get | Key unknowns |
|---|---|---|---|
| **M0** | Skeleton + design tokens | app opens, blank WebView, theme applied | none — safe |
| **M1** | Real browser | navigate, URL bar, adblock, tab stack, spaces | filter engine on Android |
| **M2** | Reader | Readability.js → clean text + junk detection | SPA reliability |
| **M3** | AI summarizer ⭐ | AI button → TL;DR / KEY POINTS streams in sheet | prompt tuning only |
| **M4** | Answer engine ⭐ | question → search → fetch → **cited** answer | DDG selector fragility |
| **M5** | Arc feel | peek bubble, command palette, spring overlays | RAM with 2 live sessions |
| **M6** | Polish | MiniLM semantic search, TextRank fallback, cache | none |

M3 and M4 are the two stars. Everything before them is load-bearing scaffolding.

---

## M0 — Skeleton + design tokens

Gradle, Compose, `minSdk 26` / `targetSdk 35`, module dirs, `ui/theme` with the
tokens from DESIGN.md. The black + pill + spring specs ship first so every screen
after inherits the feel for free.


## M1 — Real browser

- WebView factory with a **session pool capped at 2 live sessions**. Hidden
  sessions get `onPause`d and destroyed — RAM is the budget.
- Bottom floating URL bar.
- `shouldInterceptRequest` → filter-list engine (uBO lists, native Kotlin).
- Tab stack as a bottom sheet, swipe-to-dismiss.
- Spaces with accent tinting.

> If the filter engine fights back, fall back to a slim compiled host list and
> ship M1 anyway. Never let one module block a milestone.


## M2 — Reader

Readability-style injector: candidate scoring, best-node selection, junk
detection. Result model carries title, text, byline, word count, excerpt.

**Test against 20 real pages** — news, blog, docs, a JS-heavy SPA, a paywalled
article. Test breadth now or debug it forever later.


## M3 — AI summarizer ⭐

- `core/ai`: OpenAI-compatible provider, Kilo preset, BYOK screen.
- `core/summarize`: structured prompt (see PROMPTS.md), map-reduce chunking.
- Bottom sheet with typewriter streaming.
- Cache by URL + content-hash — summarize once, never burn quota twice.


## M4 — Answer engine ⭐

The flagship. Search → fetch → prune → synthesize with forced `[1][2]`
citations. Source cards land *before* the answer streams, because fetching is
~1-2s and generation is ~4s.


## M5 — Arc feel

Peek bubble (overlay WebView, draggable, 28dp, blurred), full-screen command
palette with fuzzy search, spring entrances everywhere, chrome that hides on
scroll.


## M6 — Polish

MiniLM ONNX for semantic tab search, TextRank offline summarizer as the
graceful fallback, tuned cache layer.

---

## Explicitly NOT in v1

Scope creep kills these projects.

- ❌ extensions — that was the Gecko tradeoff, already made
- ❌ local LLM — RAM says no
- ❌ split view — a second live session costs ~250 MB
- ❌ Firefox sync
- ❌ easels / notes

---

## Risks

| Risk | Mitigation |
|---|---|
| Filter engine is fiddly on Android | slim compiled host list as fallback |
| DDG selectors change | scrape multiple patterns + SearXNG/Mojeek fallback chain |
| Kilo rate limits (shared free tier) | aggressive caching + TextRank fallback |
| SPAs beat the reader | wait-for-network-idle + content-hash re-extraction |
