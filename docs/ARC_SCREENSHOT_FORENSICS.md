# Arc Search Android — Screenshot Forensics, Pass 2

**Date:** 2026-10-27  
**Scope:** every directly inspected screenshot asset in the first-pass source set  
**Branch:** `feature/xeno-arc-search-e2e-rebuild`  
**Companion data:** `tools/arc-reference/reference_measurements.json`

This pass inspects the visible artwork asset-by-asset. It distinguishes true screen captures from photos and promotional composites, records bounding boxes where a screen capture is visible, and derives layout proportions from those boxes.

## Measurement integrity

The web inspection renderer exposes image previews to the analysis environment, but the original image bytes are not mounted in the local workspace. Therefore:

- The coordinates below are **manual measurements of the rendered preview canvas**, not a claim that the original JPEG/PNG has those exact native dimensions.
- Preview coordinate estimates are generally ±4–8 preview pixels, depending on the edge and image compression. Some bounding boxes are more uncertain due to antialiasing and clipped cards.
- No HEX value is presented as machine-sampled. Any colors described are visual estimates only.
- Exact source-native pixel dimensions, per-pixel RGB histograms, LAB values, edge radii, and frame-level animation timing require the original image/video bytes. The comparison tool is in place for those once raw files are available.
- **Do not convert the preview coordinates directly into Compose dp.** The phone's density, scaling and screenshot export size are not known for these web-rendered previews.

That caveat is not an excuse to stop inspecting. It marks the boundary between observed composition and measurements that would otherwise be fabricated.

## A. Genuine full-screen Android captures

The following are portrait screenshots embedded as original full-screen captures by Android Authority, rather than photographs of a handset. The web renderer shows them at an approximately 428 × 959 preview canvas; that is the **preview coordinate system** used here, not a verified source-file resolution.

### AA-BFM-01 — Browse for Me, top of answer

Source: https://www.androidauthority.com/wp-content/uploads/2024/10/arc-search-browser-browse-for-me-man-united-aston-villa-1.jpg

| Region | Preview box `[left, top, right, bottom)` | Approx. size | Observation |
|---|---:|---:|---|
| Status bar | [0, 0, 428, 34] | 428 × 34 | System chrome occupies a shallow top strip. |
| Search/query pill | [19, 54, 410, 110] | 391 × 56 | 4.4% side margin; nearly full-width capsule. |
| Hero image carousel | [19, 134, 428, 310] | 409 × 176 | Images start 24 px below the query pill. Third card is clipped by viewport edge. |
| Card 1 | [19, 134, 157, 310] | 138 × 176 | Rounded portrait image. |
| Card 2 | [168, 134, 307, 310] | 139 × 176 | 11 px gap after card 1. |
| Card 3, visible crop | [319, 134, 428, 310] | 109 × 176 visible | Card continues beyond the right viewport; must preserve horizontal clipping, not squeeze all cards into equal columns. |
| Answer heading | [19, 365, 412, 416] | 393 × 51 | Two-line bright-blue heading aligned to the 19 px left content margin. |
| First content section | [22, 442, 407, 590] | 385 × 148 | Body text and inline source link; line spacing is generous enough to scan. |
| Source-card carousel | [19, 816, 409, 899] | 390 × 83 | Horizontal source cards sit near bottom, above the dock. |
| Bottom dock | [0, 910, 428, 959] | 428 × 49 | Fixed full-width chrome. Content viewport ends before/under the dock. |
| Tab-switcher affordance | [47, 924, 99, 959] | 52 × 35 visible | Left-side stacked-preview icon; bottom edge meets system gesture area. |
| Centre plus pill | [172, 910, 258, 959] | 86 × 49 visible | Strong center action; top aligns with dock's top edge in this state. |
| Up-chevron target | [378, 925, 400, 948] | 22 × 23 | Right-side control with large surrounding hit area. |

**Derived preview ratios:** query pill width ≈91.4% of canvas width; side margin ≈4.4%; hero image height ≈18.4% of canvas height; dock height ≈5.1% of canvas height. These ratios are provisional because the preview scaling is not source-native.

**Technical implications:** the answer is a scrollable, structured page rather than a chat bubble list. Horizontal image and source-card carousels are clipped viewports. The dock must remain visually fixed while answer content scrolls; it is not part of the answer's vertical content flow.

### AA-BFM-02 — Browse for Me, lower answer sections

Source: https://www.androidauthority.com/wp-content/uploads/2024/10/arc-search-browser-browse-for-me-man-united-aston-villa-2.jpg

| Region | Preview box | Observation |
|---|---:|---|
| Status bar | [0, 0, 428, 34] | Persists during scroll. |
| “Recent Matches” section heading | [19, 34, 246, 59] | Page content starts near the top when scrolled down. |
| Section body / list | [20, 72, 409, 902] | Repeated icon + bold key + explanatory text rows. Vertical rhythm is consistent across sections. |
| Bottom dock | [0, 910, 428, 959] | Same fixed geometry as AA-BFM-01. |

**Technical implications:** section headings and list items are part of one long native-looking content document. The dock remains anchored while the article body scrolls. Preserve visible source links and distinct bold lead-ins rather than flattening the content into a generic paragraph.

### AA-BFM-03 — Alternate topic / answer continuation

Source: https://www.androidauthority.com/wp-content/uploads/2024/10/arc-search-browser-browse-for-me-man-united-aston-villa-3.jpg

| Region | Preview box | Observation |
|---|---:|---|
| Top image strip, partially scrolled | [19, 38, 428, 153] | Images may be partially clipped by the top viewport during scroll. Do not force the image row back to the top on each scroll. |
| Answer title | [19, 170, 413, 223] | Two-line blue title. |
| Summary rows | [21, 238, 408, 523] | Icon glyph, bold short label, dash separator and body copy form the repeated row grammar. |
| Source cards | [19, 816, 410, 899] | Same general source-card region as the first answer. |
| Bottom dock | [0, 910, 428, 959] | Persistent. |

**Technical implications:** a different query produces the same layout primitives, not a different full-screen template. Result content varies; shared geometry stays stable.

### AA-WEB-01 — Real page in Arc, dock visible

Source: https://i0.wp.com/9to5google.com/wp-content/uploads/sites/4/2024/10/Arc-Search-Android-1-1.jpg?ssl=1

The screenshot shows an actual web page being viewed inside Arc, rather than a phone mockup. The displayed preview is approximately 443 × 960.

| Region | Preview box | Observation |
|---|---:|---|
| Status/system strip | [0, 0, 443, 36] | System indicators remain visible. |
| Web content | [0, 36, 443, 917] | Page content occupies almost all usable height; there is no persistent full-width URL toolbar at the top. |
| Bottom dock | [0, 916, 443, 959] | Dock overlays/occupies a fixed bottom band. |
| Tab switcher icon | [48, 931, 101, 959] | Left aligned. |
| Centre plus pill | [182, 929, 266, 959] | Centre aligned. |
| Up-chevron | [366, 936, 383, 954] | Right aligned. |
| System gesture area | approximately [165, 952, 278, 959] | Must be treated separately from the app hit-target. |

**Technical implication:** browsing chrome is reduced to a three-part bottom dock on the reading surface. When reproducing this, separately measure app dock insets and Android's navigation-gesture inset on the target device.

## B. Full-screen onboarding captures (legacy 2024 early access)

These six captures are actual full-screen app screens, but they are **historical onboarding**. They are evidence for their specific onboarding flow and do not establish the latest app's main-screen layout.

Source pattern:  
`https://www.androidauthority.com/wp-content/uploads/2024/10/arc-search-browser-android-onboarding-{1..6}.jpg`

All six are displayed at approximately 428 × 959 preview size.

### AA-ONB-01 — Intro / “Get what you want twice as fast”

- Mark region: approx. [177, 190, 251, 253].
- Primary headline: approx. [105, 449, 324, 537].
- Bottom “Let's go” action: approx. [329, 918, 389, 941].
- Background: saturated royal purple, visually flat in this capture.
- Geometry: mark centered horizontally around x=214; headline also centered. Large empty separation between mark and text.
- Note: this is a single onboarding page, not proof that the in-app home uses this exact background or mark size.

### AA-ONB-02 — Feature explanation cards

- Headline: approx. [117, 219, 311, 272].
- Feature stack: approx. [20, 388, 408, 699].
- Feature rows: four stacked rows, each around 75–80 px in the preview; rounded outer container.
- Wavy division / footer start: y≈911.
- Progress indicators: approx. [192, 923, 232, 935].
- Next action: approx. [333, 934, 412, 958].
- The panel uses minimal row gaps; icon, label and muted explanatory line share a common left alignment.

### AA-ONB-03 — Search field and suggestions tutorial

- Headline: approx. [98, 201, 331, 256].
- Example search/suggestions sheet: approx. [32, 583, 398, 913].
- Search field at top of sheet: spans nearly the card's inner width; its lower bound is hard to separate from the white sheet in this rendered capture.
- Suggestion rows include a magnifying-glass icon, query text and a separate Browse for Me pill.
- Footer has a scalloped boundary around y≈912.
- Implication: standard suggestions and Browse for Me actions are separate tap targets, not a single row click.

### AA-ONB-04 — Browse for Me skeleton tutorial

- Success check mark: approx. [194, 208, 235, 249].
- Main answer panel: approx. [32, 354, 398, 913].
- Query field: approx. [50, 371, 378, 423].
- Horizontal image-card row: approx. [50, 449, 398, 657], with first two cards each ≈139 × 208 and a third card clipped at the right edge.
- Answer title: approx. [51, 682, 375, 733].
- Content rows begin below the title. Image skeletons are tall rounded cards, not square tiles.
- Implication: loading state reserves the same geometry as final image content; avoid a spinner that causes all content to jump.

### AA-ONB-05 — Ad-block explainer

- Headline: approx. [97, 185, 332, 231].
- Example website panel: approx. [66, 305, 362, 914].
- Floating “Block ads” control: approx. [16, 722, 412, 807], crossing the website mockup.
- Footer division: y≈911.
- This is a marketing/onboarding composition. Do not reuse its callout's position as the live site-settings sheet geometry.

### AA-ONB-06 — Set default browser

- App icon: approx. [126, 252, 302, 436].
- Headline: approx. [111, 784, 317, 854].
- Full-width default-browser action: approx. [17, 789, 412, 840] (overlapping the headline estimate indicates this image's visual hierarchy is atypical; raw-pixel inspection is needed before resolving exact bounds).
- Footer / progress area: y≈913 onward.
- This screen is a conversion step, not the main app Settings screen.

**Onboarding color note:** visible blue, coral, cyan and peach fills vary by onboarding page. Those are page-specific artwork colors, not one global Arc theme. The source data should be sampled before any HEX token is promoted to a design-system value.

## C. Screenshots embedded in photos of a device

These assets were inspected but are **not valid for direct coordinate measurements** because the screen is angled, perspective-distorted, cropped or partly occluded.

| Asset | Source | What it proves visually | Why coordinates are excluded |
|---|---|---|---|
| Android search-provider settings | https://www.androidauthority.com/wp-content/uploads/2024/10/arc-search-browser-search-providers-1.jpg | Separate Search Engines settings surface; list includes Google, Browse for Me, Bing, DuckDuckGo, Ecosia, Perplexity and Kagi in this historical build. | Perspective and camera angle invalidate screen-pixel bounds. Also historical provider set may differ from current version. |
| Tab switcher photographed at angle | https://www.androidauthority.com/wp-content/uploads/2024/10/arc-search-browser-tab-switcher-1.jpg | Active card is centered and raised above background; neighboring cards peek at edges; close/new-tab/settings affordances. | Rotation, glass reflections and perspective. |
| Three browsers / tab comparison | https://www.androidauthority.com/wp-content/uploads/2024/10/arc-search-browser-tab-switcher-2.jpg | Arc's tab preview is one card at a time and horizontally navigable, distinct from Chrome/Firefox grids. | Camera perspective and multiple-device framing. |
| Foldable / wide Browse for Me | https://www.androidauthority.com/wp-content/uploads/2024/10/arc-search-browser-browse-for-me-2.jpg | Answer surface adapts into a wider view with text and source snippets. | Angled handset, reflections and partially cropped UI. |
| Browse for Me on photographed phone | https://www.androidauthority.com/wp-content/uploads/2024/10/arc-search-browser-browse-for-me-1.jpg | Shows source-heavy answer content and bottom controls, including a colored thin progress/indicator line above the dock in that state. | Angled handset and perspective; no dependable pixel geometry. |
| 9to5Google phone-on-table home screen | https://9to5google.com/wp-content/uploads/sites/4/2024/08/Arc-Search-Android.jpg?quality=82&strip=all&w=1600 | Splash/home mark against purple/blue/pink field; small settings affordance and bottom controls. | Phone is diagonally photographed; screen itself has perspective. |
| 9to5Google phone-on-laptop home screen | https://9to5google.com/wp-content/uploads/sites/4/2024/11/Arc-Search-for-Android-3.jpg?quality=82&strip=all&w=1024 | Splash/home mark and bottom controls in later launch-period footage/photo. | Perspective and image processing; do not use to sample colors. |

## D. Official Google Play screenshot set

Source: https://play.google.com/store/apps/details?id=company.thebrowser.arc  
Seven assets were opened individually. They render as tall promotional cards with app content surrounded by explanatory text/background art; the device/screen content is not an unmodified, full-bleed screenshot.

| Asset | Observed screen/features | Measurement disposition |
|---|---|---|
| Play 01 | “Fastest way to search” — search field, suggestion list and separate Browse for Me actions; pastel gradient/soft white screen treatment. | Component inventory only; promo card and phone screen are rescaled. |
| Play 02 | Browse for Me: query, horizontal food-photo cards, blue heading and structured benefits list. | Confirms image-card proportion and answer hierarchy. No native dp. |
| Play 03 | Site Settings sheet with Block ads, Block Cookie Banners and Block Trackers switches. | Confirms grouping/action names; sheet coordinates are composited. |
| Play 04 | Tab preview / auto-archive. | Confirms one raised preview card over partially visible neighboring pages. |
| Play 05 | Voice-search promotion with example prompts and a face/listening animation. | Motion must be derived from video or live use; a static promo frame cannot specify timing. Verify that the shown flow is Android-specific. |
| Play 06 | Android home-screen widget and app icons. | Launcher/widget material; not an in-app browser screen. |
| Play 07 | Default-browser onboarding. | Onboarding/CTA; not Settings. |

## E. Designer's original Android case-study assets

Source and context: https://nataliealmosa.ca/arc  
The designer explicitly says Android UI needed its own visual treatment, replaced some brand gradients with textures, leaned into shapes, used a horizontally scrolling Android tab pattern and a Material carousel for Browse for Me. The images below are composed portfolio artwork; measure relative composition only, not source-device dp.

| Asset/source URL | Preview observation | Geometry recorded for comparison |
|---|---|---|
| Hero mockup: https://nataliealmosa.ca/_next/image?q=95&url=%2Fimages%2Farc%2Farc-hero-mockup.png&w=3840 | Three overlapping phone states: search/home left, Browse for Me center, website/tab state right. | Canvas preview ~1000 × 563. Center phone screen is clipped by lower canvas boundary; query pill spans most of the inner width; two large portrait images and a partially clipped third image form the carousel. |
| Content image 1: https://nataliealmosa.ca/_next/image?q=75&url=%2Fimages%2Farc%2Farc-content-image-1.webp&w=3840 | Same three-state story; center answer has two large image cards and a clipped third card. | Screen/composite ratio unsuitable for dp. Confirms carousel card ratio around 0.8 height-to-width in displayed artwork. |
| Content image 10: https://nataliealmosa.ca/_next/image?q=75&url=%2Fimages%2Farc%2Farc-content-image-10.webp&w=3840 | Intro, feature explainer cards, app icon picker. | Marketing/onboarding panels only. |
| Content image 13: https://nataliealmosa.ca/_next/image?q=75&url=%2Fimages%2Farc%2Farc-content-image-13.webp&w=3840 | Text-first answer beside an image/card-oriented answer. | Important: answer layout is query-dependent; source cards are a horizontal row, while summary paragraphs use a different information hierarchy. |
| Content image 15: https://nataliealmosa.ca/_next/image?q=75&url=%2Fimages%2Farc%2Farc-content-image-15.webp&w=3840 | Tab carousel with a raised foreground web card and adjacent pages peeking at left/right. | Foreground card centered; side previews partly occluded. Requires horizontal pager + z-order, not equal-width grid. |
| Content image 17: https://nataliealmosa.ca/_next/image?q=75&url=%2Fimages%2Farc%2Farc-content-image-17.webp&w=3840 | Browse for Me in loading state, search pill, horizontal images, title and content skeletons. | Loading skeleton reserves title/body space after the image carousel. This is a stateful layout transition, not a full-screen spinner. |
| Content image 21: https://nataliealmosa.ca/_next/image?q=75&url=%2Fimages%2Farc%2Farc-content-image-21.webp&w=3840 | Three marketing panels for home, answer, site settings. | Repeats the same functional hierarchy across surfaces; exterior gradients/captions must not be sampled as in-app colors. |

## F. Comss three-panel image

Source: https://cdn.comss.net/img/092024/arcsearch_android-screen1.png

This square composite shows three vertically arranged screens side by side at only about 170 preview pixels per screen:
1. Blue/coral onboarding tutorial with the search field and Browse for Me button.
2. Dark search suggestions with multiple query rows; every visible row has a distinct Browse for Me pill on the right.
3. Expanded site-controls panel over a webpage, with a two-column action grid at top and per-site controls below.

Because all three screen states are compressed into one square image, this source is used as cross-check evidence only. It is not suitable for measuring individual row height, icon hit targets, corner radius or colors.

## G. Official Play and designer assets: measured conclusions

1. **Dock invariants:** both raw full-screen Browse for Me captures and a separate raw Arc webpage capture show the three-part bottom dock: tab stack left, plus capsule center, upward chevron right. The controls are anchored along the bottom; use Android insets separately.
2. **Image carousel:** at least two raw Browse for Me captures show roughly equal portrait cards, small gaps and a third card clipped at the right edge. The marketing assets corroborate this pattern. It should be implemented as a horizontal lazy row or pager with fixed aspect ratio and edge clipping, not a responsive grid.
3. **Answer hierarchy:** title is blue and prominent; body uses repeated icon/label/explanation rows; source tiles can form a lower horizontal row. The output is not a conversation bubble stream.
4. **Scrolling:** lower answer screenshot preserves the same bottom dock and continues with section headings and body rows. The answer body scrolls behind/above the fixed dock.
5. **Settings taxonomy:** actual/promotional references expose three distinct blocking toggles: ads, cookie banners, trackers. Don't collapse them into a single “adblock” switch.
6. **Tab-switcher depth:** raised active card + partially revealed neighboring previews are consistent between original designer image and photographed actual devices. Use as geometry/z-order evidence, but don't infer exact transforms from photographed images.
7. **Theme:** references include light results, dark results and highly saturated onboarding pages. Colors are state/theme-specific. A single universal palette inferred from one dark screenshot would be wrong.
8. **Motion:** one frame can identify the states involved but never determine duration/easing. Voice search, tab movement, bottom-sheet transitions and the loading skeleton need recordings for quantitative motion analysis.

## H. Still not measurable without raw binaries / screen recordings

- Exact native image resolution and DPI.
- Sampled HEX/RGBA values, channel histograms and LAB differences.
- Exact Compose dp dimensions and clickable hit-target sizes.
- Corner radii to sub-pixel accuracy.
- Animation duration, easing curve, velocity thresholds, overshoot, haptic timing and cancellation behavior.
- The current app version's full Settings screens and current dynamic states not shown in public images.

These are explicit gaps, not values to fill with visual guesses. The source-image manifest in `tools/arc-reference/reference_measurements.json` gives the corresponding screenshot identities and coordinate boxes.
