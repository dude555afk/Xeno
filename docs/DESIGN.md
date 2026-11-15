# Design language

Arc Search's Android interaction model, translated into Xeno's visual identity.
The implementation below uses the Android-first layout: keyboard-first search,
a bottom three-control dock, and a horizontal tab-card carousel.

## Tokens

```
bg             #000000        true AMOLED black, never #0A0A0A
surface        white @ 6%     elevated layer
surface-hi     white @ 10%    active / pressed
stroke         white @ 8%     1dp, on every card
accent         per-space      bleeds into caret, scroll glow, active pill

radius         28dp  cards
               20dp  inputs
               100dp pills / buttons

type           bold + tight tracking for headings
               comfortable body, min 15sp
```

## Arc Search Android anatomy

| Arc Search Android | Xeno |
|---|---|
| Gradient start surface with central search affordance | Gradient start surface with Xeno's arch mark and search card |
| Search expands into a keyboard-first full-screen view | Full-screen search surface, immediately focused and raised above the IME |
| Bottom-left tab switcher | Bottom-left tab count and horizontal page-card carousel |
| Center plus creates a new search | Center plus creates a new Xeno tab |
| Bottom-right chevron opens page controls | Bottom-right chevron opens page actions |
| Browse for Me reading view with source carousel | Light, editorial reading surface with source cards and inline citations |

The browser renderer remains Android System WebView, which is Chromium-based.
Compose owns the browser chrome; the WebView owns page rendering.

## The three details that sell it

**1. The bottom dock is the navigation.**
Tabs, new search, and page controls stay reachable at the bottom. The web page
gets the space above it.

**2. Keyboard-first search.**
Opening search requests focus and raises the keyboard immediately. Suggestions
stay in the remaining viewport, never behind the IME.

**3. Page cards, not a tab list.**
The tab switcher is horizontal and visual. Real page previews should be used
instead of decorative placeholders whenever a captured preview is available.

## Motion and browser behaviour

- Search opens with focus and the keyboard; closing it returns to the active page.
- The tab switcher is a horizontal card carousel. It captures bounded in-memory page previews before switching.
- Page chrome hides while scrolling down and reappears on upward scroll or at the top of a page.
- The page-controls sheet supports swipe-down dismissal and exposes navigation, reload, copy/share, find, summarize, pin, text zoom, content blocking, and app settings.
- System Back dismisses the top-most surface first, then navigates browser history.
- The settings destination persists theme mode, a global accent override (or the active space accent), startup search, content blocking, and reduced-motion preference.

Motion uses spring-based entrances for full-screen surfaces and short fades/slides for browser chrome. The reduced-motion preference removes overlay transitions and uses immediate tab/answer scrolling instead of animated scrolling.

## Settings map

| Section | Controls |
|---|---|
| Appearance | System / dark / light theme; space accent or six fixed accent colours |
| Search | Focus search and show the keyboard at launch |
| Privacy | Global ad and tracker blocking |
| Motion | Enable or reduce UI animations |
| About | Version, rendering engine, and interface description |

## Non-negotiables

- Material 3 foundation, but the theme is ours — don't ship defaults.
- Translucency needs a real backdrop. Blur nothing and it looks like a bug.
- Everything is rounded. If a corner is sharp, it's a mistake.
