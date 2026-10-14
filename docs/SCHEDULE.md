# Schedule

Start: **Fri 16 Oct 2026** (IST, UTC+05:30)
Target finish: **Thu 5 Nov 2026**
Cadence: part-time, evening blocks — roughly 2-3h nights, longer weekends.

---

## Week 1 — Skeleton, browser, reader

| Date | Day | Work | Exit condition |
|---|---|---|---|
| Oct 16 | Fri | **M0** Gradle, module dirs, `ui/theme` tokens | app launches, theme visible |
| Oct 17 | Sat | **M0** typography, shapes, motion specs verified | every token applied to a test screen |
| Oct 18 | Sun | **M1** WebView factory + session pool (cap 2 live) | two pages load, third gets frozen |
| Oct 19 | Mon | **M1** floating URL bar + navigation + progress | can browse real sites |
| Oct 20 | Tue | **M1** filter-list engine + `shouldInterceptRequest` | ads blocked on a news site |
| Oct 21 | Wed | **M1** tab stack bottom sheet + swipe-to-dismiss | tabs work and look right |
| Oct 22 | Thu | **M1** spaces + accent tinting | **M1 shippable** |

## Week 2 — Reader, summarizer, answer engine

| Date | Day | Work | Exit condition |
|---|---|---|---|
| Oct 23 | Fri | **M2** Readability injector + result model | clean text from a news article |
| Oct 24 | Sat | **M2** test 20 real pages, incl. SPA + paywall | 90%+ clean extraction |
| Oct 25 | Sun | **buffer** — catch up, fix breakage | |
| Oct 26 | Mon | **M3** `core/ai` provider + Kilo preset + BYOK | Kilo responds with stream |
| Oct 27 | Tue | **M3** summarize prompt + map-reduce chunking | good output on a long article |
| Oct 28 | Wed | **M3** streaming bottom sheet + typewriter | **M3 shippable — AI browser** |
| Oct 29 | Thu | **M4** offscreen DDG scraper + fallback chain | candidate URLs extracted |
| Oct 30 | Fri | **M4** parallel fetch + prune + token budget | sources ranked cleanly |
| Oct 31 | Sat | **M4** synthesis + forced citations + source cards | answer with `[1][2]` |

## Week 3 — Answer engine, Arc feel, polish

| Date | Day | Work | Exit condition |
|---|---|---|---|
| Nov 1 | Sun | **M4** answer sheet UX + caching | **M4 shippable — the flagship** |
| Nov 2 | Mon | **M4** hardening: failures, timeouts, junk sources | never a dead screen |
| Nov 3 | Tue | **M5** peek bubble (draggable overlay WebView) | link preview works |
| Nov 4 | Wed | **M5** command palette + fuzzy search + spring | **M5 shippable — Arc feel** |
| Nov 5 | Thu | **M6** MiniLM semantic search, TextRank fallback, cache tune | **M6 shippable — v1 done** 🎯 |

---

## Rules of the schedule

1. **A milestone ships or you move on.** M1's filter engine does not get to eat
   two days. Fallback, ship, revisit in M6.
2. **RSI-aware honor policy is not optional.** If Oct 27 is a bad hand day,
   slide M3 and keep the finish line. The buffer on Oct 25 exists for that.
3. **No milestone starts before its dependency is genuinely done.**
   `reader` before `ai` — no exceptions, or you will rewrite both.
4. **Every shippable day ends with an APK on your phone.** Not "the code is
   written" — an APK you actually tapped.
