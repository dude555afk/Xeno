# Xeno

**A lightweight, AI-powered browser for Android.**

Arc-grade feel — spaces, peek bubbles, a command palette that doubles as an answer engine — at a fraction of the footprint. No Gecko, no Electron. Just the system WebView and a lot of taste.

---

## What it is

Xeno is a browser built around one idea: **the address bar should answer, not just navigate.**

Type a question, get a cited, synthesized answer sourced from the live web. Type a URL, get a page. One bar, two modes, one keystroke apart.

```
you:   "what's quantum entanglement?"
xeno:  ┌─────────────────────────────┐
       │ Entanglement is a quantum   │
       │ correlation where measuring │
       │ one particle fixes the      │
       │ state of its partner        │
       │ instantly, at any distance  │
       │ [1][3]                      │
       └─────────────────────────────┘
         [1] Nature   [3] Physics.SE
```

## Feature set

| Feature | Status |
|---|---|
| WebView engine + multi-session pool | 🚧 planned |
| Readability.js page extraction | 🚧 planned |
| Filter-list ad blocking (uBO lists, native) | 🚧 planned |
| Spaces (accent-tinted workspaces) | 🚧 planned |
| Peek bubble (Little Arc equivalent) | 🚧 planned |
| Command palette + fuzzy search | 🚧 planned |
| **AI answer engine (search → fetch → cite)** | 🎯 flagship |
| Streaming page summarizer | 🚧 planned |
| Semantic tab/history search (MiniLM) | 🚧 planned |
| TextRank offline summarizer (fallback) | 🚧 planned |

---

## Architecture

```
┌──────────────────────────────────────────────┐
│                    UI                        │
│   Spaces · Tab stack · Command bar · AI sheet │
└───────────────────┬──────────────────────────┘
                    │
┌───────────────────▼──────────────────────────┐
│               AI LAYER                        │
│   summarizer · answer engine · embeddings     │
└───────────────────┬──────────────────────────┘
                    │
┌───────────────────▼──────────────────────────┐
│               CORE                            │
│   reader (Readability.js) · fetch/rank        │
│   blocking (adblock-rust + uBO lists)         │
└───────────────────┬──────────────────────────┘
                    │
┌───────────────────▼──────────────────────────┐
│            ENGINE                             │
│      Android System WebView                   │
└──────────────────────────────────────────────┘
```

**See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the pipeline specs and
[`docs/DESIGN.md`](docs/DESIGN.md) for the design tokens.**

## Stack

- Kotlin + Jetpack Compose (Material 3), `minSdk 26`, `targetSdk 35`
- Android System WebView (zero extra engine bytes)
- `adblock-rust` for native filter-list blocking — uBlock Origin's lists and syntax, without the 190 MB Gecko engine
- Readability.js injected in-page for clean text extraction
- Kilo Gateway (OpenAI-compatible) as the default AI backend, BYOK for anything OpenAI-compatible
- MiniLM ONNX via sherpa-onnx for on-device semantic search

## Design language

AMOLED black, frosted glass, everything is a pill, spring physics on every overlay. Design tokens live in [`docs/DESIGN.md`](docs/DESIGN.md).

## Status

Early scaffolding. First module going in: reader + summarizer pipeline.
