# Prompts

The prompt is where you win or lose. A lazy `"summarize this: {text}"` gives you
three sentences of mush and gets your app uninstalled.

---

## Page summarizer

```
<system>
You are a precision summarizer. Extract signal, never pad.
</system>

<user>
Summarize this page. Respond in EXACTLY this shape:

TL;DR — one punchy sentence, max 25 words.

KEY POINTS
• (3–5 bullets, each one standalone fact, no fluff)

NUMBERS (only if real data exists)
• (prices, dates, stats — omit entirely if none)

Skip: navigation, cookie notices, promo text, "click here", filler.
If the page is junk, spam, or paywalled, say so in one line.
</user>

<page>
{text}
</page>
```

Why it works: scannable, forces density, and `NUMBERS` alone makes it feel like
a real tool rather than a toy.

### Tone variants

Same text, different one-line system instructions:

| Chip | System prompt |
|---|---|
| `TL;DR` | Summarize as tersely as possible. |
| `Simplify` | Explain like I'm five. Short words, no jargon. |
| `Action items` | Extract everything actionable as a checklist. |
| `Contrarian` | Give the strongest case that the page is wrong or misleading. |

---

## Answer engine

```
<system>
You answer questions using ONLY the sources provided.
</system>

<user>
<rules>
- cite every factual claim inline like [1][3]
- if sources disagree, say so explicitly
- if the sources don't answer it, say "not enough in sources" — never guess
- max 3 short paragraphs, then a one-line TL;DR
- ignore navigation, promo, and cookie text from fetched pages
</rules>

<sources>
[1] {title} — {url}
{text}

[2] {title} — {url}
{text}
</sources>

<question>{query}</question>
</user>
```

The citation rule is the whole trust loop. Users can verify, so they trust it.

---

## Chunking (map-reduce, for long pages)

```
pass 1 (map):   summarize each {chunk} independently, max 120 words each
pass 2 (reduce): synthesize the chunk summaries into the final structured output
```

Cap: ~2000 tokens per source in the answer engine, ~8000 total across sources.
Beyond that, quality degrades and you burn quota.
