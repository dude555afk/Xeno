# Arc Search Android — Screenshot Forensics, Pass 2

**Date:** 2026-10-27  
**Scope:** every directly inspected screenshot asset in the first-pass source set  
**Branch:** `feature/xeno-arc-search-e2e-rebuild`  
**Companion data:** `tools/arc-reference/reference_measurements.json`

This pass inspects the visible artwork asset-by-asset. It distinguishes true screen captures from photos and promotional composites, records bounding boxes where a screen capture is visible, and derives layout proportions from those boxes.

## Measurement integrity

The web inspection renderer exposes image previews to the analysis environment, but the original image bytes are not mounted in the local workspace. Therefore:

- The coordinates below are **manual measurements of the rendered coordinate canvas**, not a claim that the original JPEG/PNG has those exact native dimensions.
- Preview coordinate estimates are generally ±4–8 preview pixels, depending on the edge and image compression. Some bounding boxes are more uncertain due to antialiasing and clipped cards.
- No HEX value is presented as machine-sampled. Any colors described are visual estimates only.
- Exact source-native pixel dimensions, per-pixel RGB histograms, LAB values, edge radii, and frame-level animation timing require the original image/video bytes. The comparison tool is in place for those once raw files are available.
- **Do not convert the coordinate-space estimates directly into Compose dp.** The phone's density, scaling and screenshot export size are not known for these web-rendered previews.

That caveat is not an excuse to stop inspecting. It marks the boundary between observed composition and measurements that would otherwise be fabricated.

## A. Genuine full-screen Android captures

The following are portrait screenshots embedded as original full-screen captures by Android Authority, rather than photographs of a handset. The web renderer shows them at an approximately 428 × 959 coordinate canvas; that is the **preview coordinate system** used here, not a verified source-file resolution.

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


## I. High-resolution Google Play assets: coordinate pass

The seven unique promotional files were opened at approximately **506 × 900 rendered-coordinate-space estimates** through the AppRecs image endpoint. The Play Store's own listing repeats these same seven files multiple times in the image gallery; repeated instances were not counted as new references. The image list below has asset-by-asset boxes in the JSON manifest and represents the actual displayed content, not inferred native device resolution.

### PLAY-01 — Home/search suggestions

Source: https://apprecs.org/gp/img/screenshots/webp/900/company.thebrowser.arc/qUGA9A0DeUFksLL68HEtF-y246zdBHFfuqJJPfRpbPo-screenshot-0

- Promotional in-app viewport: [61, 182, 445, 698], approx. 384 × 516 preview pixels.
- Search field: [81, 418, 425, 471], approx. 344 × 53.
- Suggestion row boxes: y=486–523, 540–577, 593–631, 647–685; ~53–54 px between row starts.
- Layout: the query field sits above a list of suggestions. Each visible suggestion places search icon/text on the left and an independent Browse for Me action on the right.
- Confidence: low-to-medium because the screenshot is embedded in a marketing frame and the bottom portion is faded/cropped.

### PLAY-02 — Browse for Me answer

Source: https://apprecs.org/gp/img/screenshots/webp/900/company.thebrowser.arc/qUGA9A0DeUFksLL68HEtF-y246zdBHFfuqJJPfRpbPo-screenshot-1

- In-app viewport estimate: [61, 209, 445, 900].
- Query field: [82, 270, 425, 323], approx. 343 × 53.
- Image carousel: [82, 347, 427, 564], approx. 345 × 217. Third card is clipped at x=427.
- Answer heading: [82, 588, 428, 648], approx. 346 × 60.
- Body visible region: [87, 676, 425, 900].
- The 24 px gap between query-field lower bound (323) and image-carousel start (347) is consistent with the 24 px query-to-carousel gap seen in AA-BFM-01 after allowing for different scaling.
- Confidence: medium-low due to composite crop.

### PLAY-03 — Site Settings

Source: https://apprecs.org/gp/img/screenshots/webp/900/company.thebrowser.arc/qUGA9A0DeUFksLL68HEtF-y246zdBHFfuqJJPfRpbPo-screenshot-2

- Visible screen: [61, 210, 445, 900].
- Bottom sheet: [61, 518, 445, 900], starting around 57.6% of the promo-canvas height.
- Heading: [88, 555, 342, 587].
- Host label: [88, 625, 290, 647].
- Block-ads row: [89, 665, 425, 709].
- Cookie-banner row: [89, 728, 426, 765].
- Tracker row: [89, 789, 426, 829].
- Next section label: [88, 859, 190, 881].
- The three blocker rows have approximate vertical centers 687, 746.5 and 809 px. The first-to-second center distance is ~59.5 px and second-to-third is ~62.5 px in this promotional image. **These are not live dp row heights.**
- The sheet is truncated by the lower image boundary. Its actual expanded height, drag threshold and close motion cannot be measured from this still.

### PLAY-04 — Tab switcher / archive promotion

Source: https://apprecs.org/gp/img/screenshots/webp/900/company.thebrowser.arc/qUGA9A0DeUFksLL68HEtF-y246zdBHFfuqJJPfRpbPo-screenshot-3

- Visible in-app area: [61, 207, 445, 900].
- Left neighbor page: [61, 365, 111, 879], approx. 50 × 514 visible.
- Active foreground card: [123, 367, 383, 879], approx. 260 × 512.
- Right neighbor: [386, 365, 444, 879], approx. 58 × 514.
- Foreground card appears around 67.7% of the promotional in-app viewport width. Adjacent cards are only narrow visible slivers because of overlap/clipping.
- Conclusion: a horizontally navigable, depth-layered card composition, not a uniform grid. Exact transform, z-order depth and fling physics still require a video or live app.

### PLAY-05 — Voice-search promo

Source: https://apprecs.org/gp/img/screenshots/webp/900/company.thebrowser.arc/qUGA9A0DeUFksLL68HEtF-y246zdBHFfuqJJPfRpbPo-screenshot-4

- Prompt cards: [22, 266, 326, 333], [176, 356, 480, 421], [22, 476, 350, 540].
- Listening/wave panel: [61, 530, 445, 900].
- Face mark: [227, 656, 289, 722].
- Greeting: [179, 759, 330, 783].
- Microphone: [248, 869, 262, 890].
- The cards appear at different horizontal offsets and partially extend beyond the in-app viewport. The lower half is dominated by nested curved layers.
- A static promo cannot reveal speech partial-result timing, scale interpolation, easing or haptic synchronization; treat motion as unmeasured.

### PLAY-06 — Android launcher widget (not an in-app screen)

Source: https://apprecs.org/gp/img/screenshots/webp/900/company.thebrowser.arc/qUGA9A0DeUFksLL68HEtF-y246zdBHFfuqJJPfRpbPo-screenshot-5

- Phone/launcher area: [61, 209, 445, 900].
- Widget promo treatment: [30, 579, 480, 645] extends past phone width, confirming it is a composited marketing overlay.
- Arc launcher icon: [91, 604, 189, 715].
- Icon row below: [88, 729, 431, 812].
- Excluded from browser UI design-token measurements because Android launcher UI and marketing treatment are included.

### PLAY-07 — Default-browser onboarding

Source: https://apprecs.org/gp/img/screenshots/webp/900/company.thebrowser.arc/qUGA9A0DeUFksLL68HEtF-y246zdBHFfuqJJPfRpbPo-screenshot-6

- Visible in-app viewport: [61, 209, 445, 900].
- App icon: [165, 400, 343, 580].
- Headline: [119, 609, 387, 698].
- Supporting copy: [121, 733, 383, 783].
- CTA may be outside the visible promo crop; its absence from this image is not proof that the real onboarding lacks it.

### Comparison with full-screen Android Authority captures

The full-screen AA-BFM-01 query field is 391/428 = 91.4% of canvas width. PLAY-02's query field is 343/506 = 67.8% of the full promotional canvas, but 343/384 = 89.3% of the estimated **inner in-app viewport** width. This demonstrates why bounds must be measured against the in-app viewport rather than the outer promotional canvas. The two estimates agree within ~2.1 percentage points despite different sources and image scaling.

## J. Other-platform screen audit: do not mix iOS into Android measurements

Five modern screenshots found through the Lazyweb gallery were inspected. All use iPhone/iOS-style status indicators, system keyboard and/or ArcMobile2 bundle context, so they are recorded in the manifest as excluded sources rather than Android targets:

- Settings (Auto-Open Keyboard, Archive Inactive Tabs set to one day, Open App Links in, Clear Browsing Data, Sync with Arc Desktop): https://zlfyzdmohcskkucuunmk.supabase.co/storage/v1/object/public/screenshots/uploaded_arc_search/auto/compare/2026-08-17/1787000577941_00_company.thebrowser.ArcMobile2-20260817T202647Z-autocap-16-1786999039.png
- Sync onboarding: https://zlfyzdmohcskkucuunmk.supabase.co/storage/v1/object/public/screenshots/uploaded_arc_search/auto/compare/2026-08-17/1787000475414_00_company.thebrowser.ArcMobile2-20260817T202647Z-cap-tab-sync-1786999157.png
- Incognito search with iOS keyboard: https://zlfyzdmohcskkucuunmk.supabase.co/storage/v1/object/public/screenshots/uploaded_arc_search/tabs/compare/2026-08-17/1787000123088_05_company.thebrowser.ArcMobile2-20260817T202647Z-cap-tab-incognito-1786999273.png
- Archive empty state: https://zlfyzdmohcskkucuunmk.supabase.co/storage/v1/object/public/screenshots/uploaded_arc_search/tabs/compare/2026-08-17/1787000122665_04_company.thebrowser.ArcMobile2-20260817T202647Z-cap-archive-1786999203.png
- Duplicate sync capture: https://zlfyzdmohcskkucuunmk.supabase.co/storage/v1/object/public/screenshots/uploaded_arc_search/tabs/compare/2026-08-17/1787000121877_03_company.thebrowser.ArcMobile2-20260817T202647Z-cap-tab-sync-1786999157.png

The gallery reports more entries, but only these initial five were visible for inspection without login in this pass. Do not claim that all 84 gallery entries were inspected.

## K. Revised evidence accounting

The current manifest contains **30 asset records and 123 measured region boxes**:
- 4 full-screen Android UI screenshots (3 Browse for Me states + one live web-page state).
- 6 full-screen Android onboarding screenshots (historical 2024 flow).
- 7 official-store marketing composites (unique assets; duplicates not counted as unique evidence).
- 7 designer portfolio composites.
- 1 three-state compressed composite.
- 5 iOS-only captures explicitly excluded from Android geometry.

These are manual preview-coordinate estimates, with relative boxes and sizes; they are not raw source-native pixel measurements. Exact RGB sampling, true source resolutions, Android dp conversion, corner-radius radii and animation timings are not claimed in this pass.


## L. High-value four-screen Android montage: direct geometry

Source: Clubic, 2024-10-07, original image delivered at 1200 × 667:  
https://pic.clubic.com/f80fea002247335/1200x667/smart/arc-browser-android.jpg

This is a notably more useful artifact than a phone photograph: four Android screen states are tiled side by side without perspective distortion. The montage can be divided into four approximate 300 × 667 panels. Its panels were inspected directly and are stored in the manifest as `CLUBIC-FOUR-SCREEN-MONTAGE` with 27 region boxes.

### Panel 1: home

- Panel bounds: [0, 0, 300, 667].
- Main mark: [95, 280, 205, 385], approx. 110 × 105 px.
- Plus control: [120, 600, 180, 630], approx. 60 × 30 px.
- Settings affordance: [242, 602, 260, 624], approx. 18 × 22 px.
- The screen is intentionally sparse: no persistent page/address toolbar. The mark sits near the middle-to-upper-middle visual center and the two primary controls sit in the bottom band.
- These dimensions describe this 1200 × 667 publisher montage only. They are not 110 dp mark dimensions.

### Panel 2: search suggestions

- Panel bounds: [300, 0, 600, 667].
- Query capsule: [310, 261, 590, 308], 280 × 47 px, about 93.3% of the 300 px screen width.
- Suggestion list: [310, 322, 592, 636].
- Visible row boxes:
  - row 1: [310, 335, 592, 379]
  - row 2: [310, 387, 592, 431]
  - row 3: [310, 439, 592, 483]
  - row 4: [310, 491, 592, 535]
  - row 5: [310, 543, 592, 587]
  - row 6, partially visible at bottom: [310, 595, 592, 636]
- Row starts advance by about 52 px; row height is about 44 px, giving about an 8 px gap between rows.
- Each row contains a left-side search icon/text and its own right-side blue “Browse for me” pill. Those must remain independent click targets.
- The lower suggestion list occupies the bottom half of the screen while the upper half retains the blurred/gradient atmosphere. This is why a simple centered field with no full suggestion state cannot match the reference.

### Panel 3: Browse for Me result

- Panel bounds: [600, 0, 900, 667].
- Query capsule: [610, 46, 890, 91], 280 × 45 px, again about 93.3% of panel width.
- Image carousel: [610, 117, 900, 260], 290 × 143 px; the rightmost image is cropped by the panel edge.
- Answer heading: [610, 270, 889, 314], approx. 279 × 44 px.
- Body: [613, 336, 889, 577], approx. 276 × 241 px.
- Bottom dock: [600, 611, 900, 640], approx. 300 × 29 px. Android's system navigation area occupies the remaining bottom strip.
- Query-to-carousel vertical gap is about 26 px; carousel-to-heading gap is about 10 px; heading-to-body gap is about 22 px.
- This is a compact, vertically continuous answer page with images, heading and structured text. It is not a conversation UI, and the image row is a horizontal carousel with overflow clipping.

### Panel 4: website + expanded controls

- Panel bounds: [900, 0, 1200, 667].
- Website content remains visible above the control surface: [900, 0, 1200, 295].
- Address/navigation row: [912, 293, 1191, 334], approx. 279 × 41 px.
- Main action grid: [913, 339, 1190, 419], approx. 277 × 80 px.
  - Its four visible actions are Translate, Pin, Find and Share.
- Zoom row: [911, 430, 1190, 466].
- Desktop Site row: [910, 472, 1190, 509].
- Site Settings row: [910, 511, 1190, 548].
- Blocked ads/trackers status: [910, 552, 1190, 589].
- The controls layer starts around y=293, roughly 44% of the image canvas down; it does not replace the whole webpage with an unrelated full-screen Settings page.
- The address row has back/forward affordances, domain/URL, link/copy and reload affordances in this frame. Keep the page-controls hierarchy separate from global app settings.

### Why this montage changes the implementation spec

1. **Search field is almost full content width:** in two separate panels its width is roughly 93.3% of the screen panel, with ~10 px side margins in this montage. A narrow centered capsule is wrong.
2. **Search has a dense stateful suggestion list:** there are six visible rows, each around 44 px high with approximately 8 px gaps, each with a dedicated action.
3. **Browse for Me has stable repeated geometry:** query capsule, clipped image carousel, title, body and bottom dock are separate, repeatable surfaces.
4. **Page controls are an anchored bottom layer:** the webpage stays visible above it; the tool grid and rows are stacked in a predictable hierarchy.
5. **Several rows are visible at once:** actions are not all buried behind a multi-level generic menu.
6. **These values are image px, not dp:** to convert a panel coordinate to Android dp, the original device screenshot density and any publisher resize factor must be known. This is still a measurable screenshot geometry reference and is considerably more trustworthy than a perspective-distorted handset photo.

## M. Android settings taxonomy observed in a separate launch-period image

The stable-launch coverage image from Astro Awani shows two Android screen states side-by-side: the quiet home surface and the **actual in-app App Settings page**. Source:
https://img.astroawani.com/2024-11/61731643773_ArcSearch.jpg

The Settings page's order and groups are readable even though the whole artwork also includes a white promotional background. The observable categories are:

- **Global:** Global Site Settings.
- **Defaults:** Set Arc as Default Browser, Change App Icon, Search Engines (with the chosen engine shown beneath), Languages.
- **Storage:** Archive, Archive Inactive Tabs (shown as “After 1 day”), Clear Browsing Data, Downloads.
- **Options:** Auto-Open Keyboard and Always Open App Links, each with a switch.

This is a functional taxonomy reference, not a pixel-measurement reference: the actual settings panel is scaled inside a combined editorial image. The one-day archive value is what this *historical screenshot* shows, not an assertion that every Android version or user configuration always uses that value.

## N. Interaction facts corroborated by contemporary reporting

Clubic's accompanying article says that pressing Enter on a typed query performs an ordinary Google search, while the distinct “Browse for me” action invokes the multi-source answer generator. It describes Incognito inside the search bar and a quick-controls section with Translate, Pin, Find, Zoom, Desktop Site and Site Settings. Source:
https://www.clubic.com/actualite-539771-arc-search-le-navigateur-qui-veut-reinventer-l-experience-web-debarque-sur-android.html

This separates two explicit intents that Xeno must not accidentally merge:
- **Search submit:** navigate to the configured search engine.
- **Browse for Me action:** invoke multi-source retrieval and render the structured answer.


## O. Six individually opened Android beta onboarding captures (9to5Google)

Source set: https://9to5google.com/2024/10/04/arc-search-for-android-is-now-available-in-beta/  
Each image is served at `w=460`, so the measurement coordinate system used here is a **460 × 1000 publisher-resized image canvas**. They are full-screen Android screenshots with no phone perspective; unlike product-marketing composites, the tutorial elements occupy the screen directly. They are still resized images, not raw device framebuffer pixels.

### 9TO5-ONB-01 — intro

Source: https://9to5google.com/wp-content/uploads/sites/4/2024/10/Arc-Search-for-Android-beta-1.png?w=460

- Mark: [190, 180, 266, 245], approx. 76 × 65 px.
- Headline: [94, 425, 360, 535], approx. 266 × 110 px, three lines centered.
- “Let's go” action: [347, 926, 411, 951], small right-aligned bottom action.
- System gesture bar: [170, 986, 279, 994].
- Large empty gap between the brand mark and headline is intentional in this onboarding state; do not treat it as the live new-tab layout.

### 9TO5-ONB-02 — feature explainer stack

Source: https://9to5google.com/wp-content/uploads/sites/4/2024/10/Arc-Search-for-Android-beta-2.png?w=460

- Headline: [116, 195, 345, 258].
- Four-row feature panel: [20, 365, 430, 701], approx. 410 × 336 px.
- Row boxes: [20,365,430,448], [20,450,430,531], [20,533,430,615], [20,617,430,701].
- The rows are approximately 82 px tall in this resized image with thin inter-row seams. Each row aligns an icon, bold label and lower-contrast description.
- The wavy footer begins near y=890, followed by progress dots and a bright “Next” control at [355,912,435,959].
- Engineering implication: build a row component with a fixed height/insets and consistent icon/text columns; do not lay each row out independently.

### 9TO5-ONB-03 — query suggestions and Browse for Me

Source: https://9to5google.com/wp-content/uploads/sites/4/2024/10/Arc-Search-for-Android-beta-3.png?w=460

- Main headline: [83, 195, 377, 259].
- Instruction label “Try it out!”: [171, 473, 286, 495].
- Example search panel: [34,574,417,893], approx. 383 × 319 px.
- Search field: [45,592,405,650], 360 × 58 px.
- Browse for Me example action: [269,668,408,725], 139 × 57 px.
- Three visible row areas: [52,732,404,782], [52,786,404,837], [52,840,404,891].
- The annotation arrow connects the tutorial text to the specific Browse for Me chip. That design demonstrates separate intent/actions: selecting a suggestion and invoking Browse for Me should not be implemented as one undifferentiated row tap.

### 9TO5-ONB-04 — Browse for Me answer and skeleton state

Source: https://9to5google.com/wp-content/uploads/sites/4/2024/10/Arc-Search-for-Android-beta-4.png?w=460

- Answer panel: [33,345,417,894].
- Query field: [51,361,403,420], 352 × 59 px.
- Portrait cards: first [52,441,185,638], second [197,441,330,638], third visible crop [341,441,414,638].
- The first two cards are roughly 133 × 197 px; the third is clipped at the right edge.
- Answer heading: [52,665,410,721]; body region: [53,748,411,892].
- Vertical intervals from query field to cards, cards to heading, and heading to body are consistent and content-bearing space is reserved before loading completes.
- Implementation implication: skeleton and final-answer layouts must share the same geometry to prevent a jump when images/text resolve. A central spinner is not equivalent.

### 9TO5-ONB-05 — ad-block explanation

Source: https://9to5google.com/wp-content/uploads/sites/4/2024/10/Arc-Search-for-Android-beta-5.png?w=460

- Headline: [81,176,385,243].
- Website card: [66,309,393,889].
- Floating blocker card: [16,706,432,790], approximately 416 × 84 px.
- The blocker card is overlaid above website content with a purple switch and white thumb in this light tutorial state.
- It is an explainer overlay, not the actual site-controls sheet. Reusing its bounds as the site-controls panel would be a category error.

### 9TO5-ONB-06 — default-browser onboarding

Source: https://9to5google.com/wp-content/uploads/sites/4/2024/10/Arc-Search-for-Android-beta-6.png?w=460

- App icon: [140,232,317,401].
- Headline: [99,491,361,583].
- Supporting copy: [65,611,390,674].
- Main full-width button: [16,773,433,825].
- Footer wave starts near y=890; progress dots are around [202,934,242,945]; Skip is [380,931,410,948].
- Onboarding has one dominant CTA and a secondary Skip action. This differs structurally from global App Settings, which is a scrollable list.

### Cross-check against the other onboarding set

The same six tutorial topics also appear as smaller rendered captures from Android Authority, which is a useful cross-source check. The 9to5Google images make the search panel, skeleton cards, feature row stack and bottom action area easier to estimate. Because both sets are publisher-resized, their coordinates do not prove native density. Treat their close alignment as corroboration for layout ratios, not an exact dp source.

## P. Revised evidence count

The current JSON manifest now contains **37 source-asset records and 208 named region boxes**:
- 4 standalone full-screen Android interface captures (Browse for Me and a real webpage).
- 6 Android Authority onboarding captures.
- 6 9to5Google Android beta onboarding captures.
- 7 official Play Store marketing composites.
- 7 designer portfolio composites.
- 1 four-panel horizontal Android UI montage.
- 1 three-panel compressed UI montage.
- 5 iOS-only screenshots explicitly excluded from Android geometry.

The manifest boxes were checked for bounds and internal width/height/normalized-coordinate consistency. Coordinates remain manually read from displayed image canvases, with uncertainty; they are not exact raw-image RGB or dp measurements.


## Q. Raw encoded-file pixel analysis (not preview-only estimates)

The source-pixel runner successfully downloaded and decoded **31 of 32 unique public image URLs** from the manifest. One source, `AA-WEB-01` (the 9to5Google image relayed by i0.wp.com), now returns HTTP 404 and has no pixel claims. Excluded iOS gallery captures are not attempted unless explicitly requested.

The successful image decodes generated **191 region-level ROI measurements** and, after explicit probe coordinates were added, **45 exact single-pixel samples** across 18 image assets. The analysis records exact encoded dimensions, content type, downloaded byte count, final URL and SHA-256 for each file. Region-level outputs include the sampled box, center pixel RGB/HEX, inset-core median/mean/standard deviation, p10 and p90, plus the quantized 12-colour representative palette.

### Q1. Exact samples from raw decoded image bytes

These are RGB values returned by Pillow on GitHub Actions from the downloaded bytes, not color names guessed from the rendered browser preview. Coordinates are the encoded file's own pixel grid.

| Asset / probe | File coordinate | Exact RGB / HEX | Interpretation |
|---|---:|---|---|
| AA-BFM-01 status-bar background | (20, 20) | RGB (30, 33, 40) / `#1E2128` | Top/system strip. |
| AA-BFM-01 gap between query and carousel | (540, 320) | RGB (30, 33, 40) / `#1E2128` | Flat dark region between surfaces. |
| AA-BFM-01 main content left margin | (20, 1200) | RGB (29, 32, 39) / `#1D2027` | Reading-canvas background. |
| AA-BFM-01 query capsule right-interior probe | (1000, 180) | RGB (66, 69, 76) / `#42454C` | Exact pixel within capsule near its right edge. |
| AA-BFM-01 bottom dock left inset | (20, 2470) | RGB (45, 46, 50) / `#2D2E32` | Dock background. |
| AA-BFM-01 bottom dock right inset | (1060, 2470) | RGB (45, 46, 50) / `#2D2E32` | Same exact sample as the opposite inset. |
| AA-BFM-02 bottom dock left inset | (20, 2470) | RGB (45, 46, 50) / `#2D2E32` | Cross-capture consistency. |
| AA-BFM-03 main content left margin | (20, 900) | RGB (29, 32, 39) / `#1D2027` | Cross-capture canvas sample. |
| Clubic montage search-screen lower background | (305, 600) | RGB (45, 46, 50) / `#2D2E32` | Repeated dark surface. |
| Clubic montage Browse for Me gutter | (605, 300) | RGB (30, 31, 36) / `#1E1F24` | Dark answer-page canvas. |
| Clubic montage controls left gutter | (905, 620) | RGB (45, 46, 50) / `#2D2E32` | Site-controls surface. |
| Clubic montage controls right gutter | (1190, 620) | RGB (45, 46, 50) / `#2D2E32` | Same exact sample as the opposite gutter. |

### Q2. Historical onboarding pixels independently reproduced by two sources

| State | Android Authority exact sample | 9to5Google exact sample |
|---|---|---|
| Intro blue | `#2403AA` | `#2404AA` |
| Coral feature tutorial | `#E1686F` | `#E1686E` |
| Indigo search tutorial | `#3339F1` | `#3339F1` |
| Light suggestion sheet | `#ECECEE` | `#ECECEE` |
| Cyan ad-block tutorial | `#6EBBD9` | `#6DBAD9` |
| White default-browser state | `#FFFFFF` | `#FFFFFF` |
| Peach footer | `#FEE2DF` | `#FEE2E0` |

Differences of one channel level across two separately published JPEG/WebP encodes are expected; they do not prove the original design tokens differ. The key result is that the sampled states are reproducible across independent publishers. They are onboarding-page colors, not a universal current-app theme.

### Q3. Encoded-file provenance examples

| Asset | Encoded format and dimensions | Bytes | SHA-256 |
|---|---|---:|---|
| AA-BFM-01 | JPEG, 1080 × 2520 | 313,228 | `50e1fe89b3e616ee86f2164eb4de6b8bbaef759d8dae334a13fbd495a63912fa` |
| AA-BFM-02 | JPEG, 1080 × 2520 | 299,672 | `d81d1b3b445f212f4f75b61817db6f9cd87938b096c70f9f9fa3102a86fea344` |
| AA-BFM-03 | JPEG, 1080 × 2520 | 296,117 | `46a1e013edfc0f617c9448414094840f401673b7be30205e027055a70a542311` |
| 9TO5-ONB-01 | WebP, 460 × 1024 | 15,864 | `988eba9b7df87e09c887dbfe74f7cb4370503bab49839d4e620b34a07a13cbe5` |
| 9TO5-ONB-04 | WebP, 460 × 1024 | 58,362 | `b09f06dac06472a89e26fa56439d60db10903cbd0ce1744c3b3fa2361fb81e89` |
| Clubic four-screen montage | JPEG, 1200 × 667 | 110,397 | `1415d7c90a726d8edf0da0bafc45dce51bff41a140ab39d9056d94ff7ebc2552` |

### Q4. Reproducible data files

The latest source-analysis run is [Analyze Arc Reference Pixels, commit f1378086](https://github.com/dude555afk/Xeno/actions/runs/40559673319). Its short-retention GitHub Actions artifact contains the complete JSON results with all exact image metadata, point samples, ROI statistics and per-image palette data. The repository includes the fetch/measurement script and manifest so the data can be regenerated.

The analysis does **not** claim that all 191 ROI rectangles represent pure design tokens: full-screen regions can include text, icons, clipping edges or images. Use the dedicated exact point probes and low-variance surface interiors for token investigation. Exact Android dp measurements still require device density / display metadata; animation duration/easing requires a screen recording with known frame timing.
