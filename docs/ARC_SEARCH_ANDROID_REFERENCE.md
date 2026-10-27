# Arc Search Android — Evidence & Reimplementation Specification

**Status:** research baseline, pass 1  
**Date:** 2026-10-27  
**Target:** Arc Search for Android, not iOS or desktop Arc  
**Xeno branch:** feature/xeno-arc-search-e2e-rebuild

This is a living specification, not a claim of pixel parity. Promotional artwork and third-party descriptions are useful evidence, but do not substitute for captures from the actual Android app.

## 1. Evidence rules

- **[O] Official:** Arc Android release notes, help documents, or Play Store listing.
- **[D] Design source:** published by a designer who worked on Arc Search Android.
- **[R] Reporting:** independent review or news report.
- **[V] Video/demo:** actual screen recording, walkthrough, or transcript.
- **[M] Measured:** geometry or motion measured from a real app capture. No [M] measurements are claimed yet.
- **[H] Hypothesis:** plausible interpretation requiring live verification.

Do not turn hypotheses into requirements without evidence. Do not treat iOS behavior as Android behavior.

## 2. Source inventory: first pass

### Primary sources

1. **[O] Official Arc Search for Android release notes, 2024–2026**  
   https://resources.arc.net/hc/en-us/articles/27306904379031-Arc-Search-for-Android-Release-Notes  
   Most important source. Lists app behaviors and regressions fixed: voice search, fidget-spinner motion, custom search engines, Browse for Me toggle, language settings, bottom-bar tab switching, tab dismissal, landscape, refresh-rate behavior, App Links, downloads, per-site Force Dark, predictive back, archive search and tab export. Latest listed release: v1.12.11, 2026-10-19.

2. **[D] Arc Search Android design case study**  
   https://nataliealmosa.ca/arc  
   The designer describes Android-specific visual treatment, texture and shape usage, horizontal tab scrolling, and a Material-style carousel in Browse for Me. The author says marketing animations were actual app screen recordings. This is the strongest public design rationale found in this pass.

3. **[O] Arc Search on Google Play**  
   https://play.google.com/store/apps/details?id=company.thebrowser.arc  
   Official product positioning and screenshot gallery. Covers search-first home, Browse for Me, site settings, tabs/archive, voice search, widgets and default-browser onboarding. These are promotional composites; use them for information hierarchy and component inventory, not exact coordinates.

4. **[O] Current Arc lifecycle notice and Android release notes**  
   https://arc.net/  
   https://resources.arc.net/hc/en-us/articles/27306904379031-Arc-Search-for-Android-Release-Notes  
   Arc says AI features and sign-in/sync turn off on 2026-11-23, and Arc Search for Android is removed from Play Store and installed devices on 2026-12-22. Xeno must own its AI/extraction rather than depend on Arc services.

5. **[O] Opening links in new tabs**  
   https://resources.arc.net/hc/en-us/articles/25481689976343-How-Do-I-Open-Links-in-New-Tabs-Using-Arc-Search  
   For Android and iOS: long-press a link and choose Open in New Tab. The help article explicitly distinguishes iOS-only background-tab behavior.

6. **[O, comparative only] Arc Search for iOS release notes**  
   https://resources.arc.net/hc/en-us/articles/23528454620311-Arc-Search-for-iOS-Release-Notes  
   Product history around query editing, translation, zoom, desktop site, summarization, cookie banners and control sheets. This is not authority for Android parity unless cross-checked against Android sources.

### Independent reporting and demos

7. **[R] 9to5Google, Android beta, 2024-10-04**  
   https://9to5google.com/2024/10/04/arc-search-for-android-is-now-available-in-beta/  
   Early Android inventory: Browse for Me, blocking and automatic tab archive.

8. **[R] 9to5Google, Android general availability, 2024-11-14**  
   https://9to5google.com/2024/11/14/arc-search-for-android-leaves-beta-adds-voice-search-widgets-and-more/  
   Voice search and distinctive animation, home-screen widgets/icons, landscape, language handling, Android 12 support, high-refresh behavior.

9. **[R] Android Authority, early hands-on review**  
   https://www.androidauthority.com/arc-search-browser-3488152/  
   Describes the Android-style recent-app tab switcher: one shrunken tab preview at a time with horizontal scrolling. Useful corroboration, not the latest-version authority.

10. **[R] Indian Express, Android early-access walkthrough, 2024-10-05**  
    https://indianexpress.com/article/technology/tech-news-technology/arc-search-android-ai-features-early-access-9605194/  
    Explains Browse for Me as reading multiple sites and generating a multi-section answer; also discusses auto-archive, pinning ordinary sites and the keyboard preference.

11. **[R] TechCrunch, Android general availability, 2024-11-14**  
    https://techcrunch.com/2024/11/14/the-browser-company-makes-arc-search-for-android-generally-available-as-it-thinks-about-other-products/  
    Confirms the feature set at stable launch.

12. **[V] The Browser Company mobile feature video / transcript mirror**  
    https://glasp.co/youtube/p/we-added-your-top-requests-to-our-new-mobile-browser-arc-search  
    Covers sharing, incognito and pinch-to-summarize. Treat as iOS/product-history context unless the video itself confirms Android.

13. **[V] Arc Search mobile demo/review, 2025-10-12**  
    https://3degreesnorth.com.au/videos/arc-search-mobile-app-demo-and-review/  
    Screen-recorded query → multi-source answer → source navigation → summarize flow. Platform in each shot must be verified before using it as an Android reference.

**Scope note:** this is a substantive first-pass source ledger, not a provably complete list of every Arc-related video/article on the internet. Continue adding original sources and timecodes. Prefer first-party demonstrations and designer material over SEO reposts.

## 3. Android-specific evidence established

- **Search-first home:** Arc's launch materials describe automatic keyboard opening, suggestions, Browse for Me, auto-archiving and built-in blocking. Later Android notes add more behavior.
- **Fidget spinner:** Android v1.8.1 explicitly says the spinner was made to rotate more smoothly. This does not justify an arbitrary idle animation; its real idle/interaction states must be recorded.
- **Voice:** Android supports long-pressing the Search plus button and a voice button in the search bar. Release notes mention haptic feedback as speech words are recognized and fixes for duplicated activations.
- **Search modes:** Android v1.9 added custom search engines and a toggle between Browse for Me and the preferred search engine.
- **Tabs:** Android v1.6 added left/right swiping on the bottom bar to switch tabs; earlier notes mention swipe-dismissable tab cards. The designer confirms horizontal scrolling is an intentional Android adaptation.
- **Archive:** automatic archival is core behavior; archive search was added to Settings in Android v1.10. Exact current Android idle timing remains unverified. Do not borrow desktop's 12-hour default as an Android fact.
- **Site controls:** Android v1.3.1 documents rebuilt ad/tracker blocking, Force Dark per site, improved desktop mode and predictive back for bottom sheets. Android v1.4 made desktop-site preference persistent per hostname.
- **Browse for Me:** documented as reading multiple pages to generate a structured page. Xeno must extract readable content, attribute sources and handle errors/paywalls. Redirecting the user to a search engine is not equivalent.
- **Platform gap:** background-tab opening is documented as iOS-only. Do not blindly port iOS Call Arc or pinch-to-summarize to Android. Android's own long-press voice interaction is the correct baseline; Android notes added a Summarize button in v1.11.1.
- **Lifecycle:** Arc says AI is disabled on 2026-11-23 and the Android app is removed on 2026-12-22. Xeno needs an independent pipeline.

## 4. Feature contracts for Xeno

“Parity” means matching observable interaction and visual intent where technically feasible, not copying private code or assuming proprietary assets can be reused.

| ID | Surface | Required contract | Evidence / unresolved work |
|---|---|---|---|
| HOME-01 | New-tab canvas | Match reference background treatment, composition, mark, search placement and dock. No invented dashboard. | [D][O]; coordinates unmeasured |
| HOME-02 | Search focus | Tap search opens editable input; keyboard obeys persistent auto-open preference in empty, existing-tab and relaunch states. | [O]; test all states |
| SEARCH-01 | Suggestions | Correct empty/query suggestions with explicit row actions; layout and motion measured from live captures. | [O]; layout pending |
| SEARCH-02 | URL or query | URL navigates; ordinary text routes to selected engine or Browse for Me; preserve punctuation and non-English input. | [O][R]; edge-case tests |
| SEARCH-03 | Engine selection | Custom templates persist and are applied to ordinary search. | [O] |
| SEARCH-04 | Browse toggle | Switch modes without losing or submitting the query twice. | [O] |
| VOICE-01 | Voice search | Search-field button and long-press plus; permission, listening, partial/final text, cancel/error/denied states; prevent duplicate requests. | [O][R]; timing pending |
| BFM-01 | Answer pipeline | Search multiple sources, extract clean text, deduplicate, generate structured answer, show source provenance, reveal results progressively and expose retry/errors. | [O][R]; Xeno backend needed |
| BFM-02 | Answer layout | Headings/lists, sourced images/cards where appropriate, direct citations, related follow-ups when supported, language handling. | [O][R]; screen inventory incomplete |
| WEB-01 | Reading surface | Main viewport belongs to page content; controls remain reachable without covering content. | [O][R]; bar behavior pending |
| WEB-02 | Page controls | Verify Android's actual action set/order: navigation, URL copy/share, reload, translate, pin, zoom, desktop site and site settings. Do not copy iOS controls without evidence. | [O]; capture required |
| WEB-03 | Link context | Long-press supports open in new tab. Verify image save/download and other Android menu items in app. | [O] |
| TABS-01 | Tab switcher | One-card-at-a-time horizontal carousel, selected-tab emphasis, predictable fling, selection and swipe-to-dismiss. | [D][O][R]; keyframes pending |
| TABS-02 | Dock gestures | Horizontal swipe on bottom bar switches tabs without stealing page gestures. | [O] |
| TABS-03 | Archive | Only eligible inactive tabs archive; pins are exempt; restore/search/clear behavior persists. Measure Android threshold; don't import desktop timing. | [O][R]; timer pending |
| TABS-04 | Pins | Pin eligible pages, persist across restart, display in the correct reference location. Verify whether AI-created pages are excluded. | [R]; live check pending |
| PRIVATE-01 | Incognito | Distinct visual state and clear status; do not persist private history/autofill/session data; regular state remains unaffected. | Help-source search; verify live |
| SITE-01 | Blocking | Per-site ad/tracker/cookie controls, persistence and clear status. Must not break links, scrolling or consent handling. | [O][R] |
| SITE-02 | Force dark/desktop | Desktop-site preference persists per host; site-level Force Dark interacts correctly with global/system theme. | [O] |
| SETTINGS-01 | Settings hub | Match groups, row heights, switches, separators, navigation and scroll behavior after measuring real settings screens. Only verified Android settings are in scope. | [O][D]; measure pending |
| SETTINGS-02 | Language/archive/export | Preferred language, archive search and open-tab export behavior as documented in Android releases. | [O] |
| SYSTEM-01 | Android integration | Landscape, system bar insets, back/predictive-back, keyboard resize, upload/file picker, downloads, external App Links and sharing. | [O] |
| PERF-01 | Motion/performance | Check 60/90/120Hz, gesture response, bottom-sheet/back behavior, tab-scroll performance and memory pressure. | [O]; benchmark pending |

## 5. Pixel and animation measurement protocol

### 5.1 Capture metadata

For every capture record device model, Android version, Arc version, logical screen size, display density, font scale, refresh rate, navigation mode, orientation, status/navigation insets, theme, incognito state, keyboard state, URL/query and tab count. Label every asset as raw device screenshot, crop, video frame or marketing composite.

Do not treat an unknown-size webpage thumbnail as a dp measurement source. Play Store assets identify components and hierarchy, but are promotional composites.

### 5.2 Geometry

Measure screenshot pixels first, then convert using **dp = px × 160 / displayDensityDpi**. Log absolute px, derived dp and normalized position (% usable viewport).

Record bounding boxes (left, top, right, bottom) for:
- usable viewport and system insets
- search field, icon hit targets, prompt rows and separators
- bottom dock, plus button, tabs control and chevron
- tab card, neighboring-card peeks, gaps and edge margins
- sheet top edge, corners, drag handle, row height and inner padding
- text line boxes/baselines and image/card aspect ratios

Never compare pixel values from different densities without normalizing them.

### 5.3 Colors

Sample at least five interior pixels per surface, avoiding antialiased edges, text and shadows. Record HEX/RGBA, sample coordinates, state/theme and confidence. Avoid sampling gradient or composited marketing artwork as though it were a flat UI token.

### 5.4 Animation capture

Record each interaction with frame timestamps, ideally at the device's supported high refresh rate. Extract frames at consistent intervals and at state transitions. For every animation log:
- trigger, start state and end state
- duration in milliseconds and measurement confidence
- translation, scale, alpha, corner-radius, blur and scrim changes over time
- inferred easing curve based on measured progress, not a generic guess
- interruption/cancellation behavior, swipe velocity thresholds and haptic events
- IME/window-inset movement independent of app content

Required animation IDs:
1. MOT-01: home mark idle motion and restart behavior.
2. MOT-02: tap plus → input focus → keyboard.
3. MOT-03: long-press plus → listening, partial words, stop/cancel/error.
4. MOT-04: suggestion → ordinary search or Browse for Me.
5. MOT-05: tab switcher entry, horizontal fling, card dismissal, activate tab.
6. MOT-06: bottom-bar swipe tab switching.
7. MOT-07: controls sheet open/close and predictive back.
8. MOT-08: page load/progress and browser chrome response.
9. MOT-09: Browse for Me loading → sources/result → follow-up/edit query.
10. MOT-10: archive and restore.
11. MOT-11: normal ↔ incognito.
12. MOT-12: keyboard appearance/dismissal and viewport adjustment.

This baseline does **not** invent exact dp values, hex colors, durations or easing constants. Those values require raw screenshots/recordings.

## 6. Visual comparison loop

1. Capture reference and Xeno on the same device, OS, density, orientation, font scale, navigation mode and keyboard state.
2. Use a deterministic test page for webpage comparisons; live pages vary and poison pixel diffs.
3. Register both images to the same viewport. Mask status-bar content, web text/carets or dynamic media only where they are not the target.
4. Produce a 50% overlay, absolute-difference image and per-region SSIM or equivalent, then list visible discrepancies.
5. Fix largest structural differences first: bounds/alignment → typography/hierarchy → colors/surfaces → icons → shadows → motion.
6. Test interactions separately. Screenshot similarity does not prove working navigation, keyboard, tab management or extraction.
7. Re-test all dependent screens whenever a shared component changes.

A build is “visual-pass” only when the reference state, Xeno state, comparison image and results are linked in the feature branch.

## 7. Acceptance tests

- **AT-01 Launch:** fresh install, launch with/without open tabs, keyboard preference on/off.
- **AT-02 Search:** empty query, full URL, domain/path, punctuation, Indic/CJK script, IME action, back/cancel.
- **AT-03 Voice:** permission allow/deny, silence, partial transcript, recognizer error, long press and fast repeated activation; no duplicate request.
- **AT-04 Browse for Me:** readable page, paywall, JS-only article, blocked/network failure, duplicate sources, empty extraction, offline, rate limit, source-link navigation and language.
- **AT-05 Web:** redirects, TLS warning, download, upload, App Link, desktop-mode persistence, Force Dark override.
- **AT-06 Tabs/archive:** 1/2/5/10+ tabs, quick switching, swipe close, restore, pinned exemption, archive deadline, process death/restart.
- **AT-07 Privacy:** private session never enters normal history/archive; clear-data removes exactly the intended stores.
- **AT-08 Settings:** values persist after restart; host-scoped and global settings stay distinct.
- **AT-09 Android/accessibility:** IME resize, gesture navigation, predictive back, landscape, large text, TalkBack target sizes.
- **AT-10 Performance:** frame pacing, WebView memory, capture cost and low-memory tab eviction.

## 8. Evidence still needed

1. Raw Arc Search Android screenshots and recordings from a real install, preferably on the target Samsung device or a second reference device.
2. Home states: cold/warm launch, keyboard on/off, empty suggestions, typed query, URL entry and voice state.
3. Complete settings and site-settings screens, including scroll positions and each toggle state.
4. Tab carousel with 1, 2, 5 and 10+ tabs; normal, pinned, incognito, archived and restored tabs.
5. Successful and failed Browse for Me sessions, including source clicks.
6. More original videos/screen recordings with timecodes tied to behaviors.
7. Exact pixel colors, geometry, animation timing and easing after capture. Keep all unresolved values marked not measured.

## 9. Source index

- Official Android release notes: https://resources.arc.net/hc/en-us/articles/27306904379031-Arc-Search-for-Android-Release-Notes
- Official Play listing and screenshot gallery: https://play.google.com/store/apps/details?id=company.thebrowser.arc
- Android designer case study: https://nataliealmosa.ca/arc
- Android open-in-new-tab behavior: https://resources.arc.net/hc/en-us/articles/25481689976343-How-Do-I-Open-Links-in-New-Tabs-Using-Arc-Search
- 9to5Google beta coverage: https://9to5google.com/2024/10/04/arc-search-for-android-is-now-available-in-beta/
- 9to5Google Android launch coverage: https://9to5google.com/2024/11/14/arc-search-for-android-leaves-beta-adds-voice-search-widgets-and-more/
- Android Authority review: https://www.androidauthority.com/arc-search-browser-3488152/
- Indian Express walkthrough: https://indianexpress.com/article/technology/tech-news-technology/arc-search-android-ai-features-early-access-9605194/
- TechCrunch launch coverage: https://techcrunch.com/2024/11/14/the-browser-company-makes-arc-search-for-android-generally-available-as-it-thinks-about-other-products/
- Mobile feature video/transcript mirror (platform verification needed): https://glasp.co/youtube/p/we-added-your-top-requests-to-our-new-mobile-browser-arc-search
- Mobile recorded demo (platform verification needed): https://3degreesnorth.com.au/videos/arc-search-mobile-app-demo-and-review/
- Arc lifecycle notice: https://arc.net/


## 10. Original designer asset index and visual observations

These are original portfolio assets linked by the Android designer's case study. They are much stronger references than unrelated Arc iOS screenshots, but still include device mockups, cropping, compositing and promotional backgrounds. Use them to catalogue states and hierarchy; do not derive final dp values directly from the composite.

- **Hero UI / tab carousel:** https://nataliealmosa.ca/_next/image?q=95&url=%2Fimages%2Farc%2Farc-hero-mockup.png&w=3840
- **Browse for Me result and image cards:** https://nataliealmosa.ca/_next/image?q=75&url=%2Fimages%2Farc%2Farc-content-image-1.webp&w=3840
- **Onboarding and app icon choices:** https://nataliealmosa.ca/_next/image?q=75&url=%2Fimages%2Farc%2Farc-content-image-10.webp&w=3840
- **Text-first answer and image/list answer comparison:** https://nataliealmosa.ca/_next/image?q=75&url=%2Fimages%2Farc%2Farc-content-image-13.webp&w=3840
- **Active tab card and neighboring tabs:** https://nataliealmosa.ca/_next/image?q=75&url=%2Fimages%2Farc%2Farc-content-image-15.webp&w=3840
- **Browse for Me image carousel / loading skeleton:** https://nataliealmosa.ca/_next/image?q=75&url=%2Fimages%2Farc%2Farc-content-image-17.webp&w=3840
- **Home, Browse for Me and site settings promo sequence:** https://nataliealmosa.ca/_next/image?q=75&url=%2Fimages%2Farc%2Farc-content-image-21.webp&w=3840

### Direct observations to carry into live capture

- The home screen reference is not just a centered logo: the visible UI includes a soft purple/pink atmosphere, a low/mid-screen search field and suggestion rows with separate Browse for Me affordances. Exact placement varies with keyboard and viewport state and must be captured live.
- Browse for Me is not a plain text-only chat sheet. References show a search/query header, horizontally clipped image cards, strong section titles, content blocks and source cards. Different intents use different answer shapes, including image/list-style results and text-first answers.
- The tab switcher is a layered preview/card surface with neighboring tabs partially visible, not a generic grid of equally sized tab thumbnails.
- Site Settings exposes distinct controls for blocking ads, cookie banners and trackers. The panel's exact sizing, icon design, background, row height and toggle colors still need raw capture.
- Onboarding and the in-app home background should not be conflated with the decorative gradients around marketing mockups. Sample colors only from the app pixels, not the surrounding promo art.
- No exact coordinates or HEX values have been measured from these assets. The pixel-measurement section remains blocked on raw captures from the app or uncomposited screen recordings.


## 11. Additional research leads captured in pass 1

These sources expand the source set beyond the core specification. They are useful for triangulation and for locating more original captures:

- **TechCrunch, Android open beta (2024-10-04):** https://techcrunch.com/2024/10/04/the-browser-company-launches-arc-search-on-android/ — notes that Browse for Me reads multiple pages and builds a new page, and reports the Android ad blocker and automatic archive at launch.
- **Digital Trends, Android announcement (2024-10-03):** https://www.digitaltrends.com/phones/arc-search-iphone-app-now-available-on-android/ — launch-day images and first-use description; useful for identifying the initial Android home and query-entry design.
- **9to5Google, Android arrival announcement (2024-08-26):** https://9to5google.com/2024/08/26/arc-search-browser-officially-coming-to-android/ — links to the brief official social clip teasing the app on a Pixel device.
- **The Verge, Arc discontinuation (2026):** https://www.theverge.com/tech/1016832/arc-browser-shutting-down — secondary reporting that aligns with Arc's official lifecycle notice.
- **Community Android UI comparison thread:** https://www.reddit.com/r/ArcBrowser/comments/1fwig1y — launch-era discussion of differences between Android and iOS UI. Treat comments as anecdotal, but screenshots can be useful for finding details to test.
- **Launch montage analysis lead:** https://refs.video/v/1857084594010403260 — third-party frame/cut analysis of a short Arc Search Android launch montage. This is a lead to the original clip, not a substitute for frame-by-frame app-motion measurement.
- **One-phone feature comparison video lead:** https://refs.video/v/yt-FHA5YZEAYOc — a short video analysis page associated with a phone-based Arc Search comparison. Verify the original footage/platform before using it as Android evidence.

### Why the original designer images are still not a pixel spec

The designer assets establish component hierarchy and reveal content that the code currently must support. However, phone frames, marketing backgrounds, image scaling and overlays contaminate absolute coordinates and color samples. We can use them to decide what state to recreate; we should not convert their coordinates directly into Compose dp values or quote them as measured Android UI dimensions. Absolute measurement remains a capture task.
