# Xeno Design Language

Xeno follows **Arc Search's mobile interaction model**, adapted for Android and Xeno's own branding. This is not the desktop Arc sidebar squeezed onto a phone. The interface should feel quiet, immediate, and built around one central action: search.

## Core visual rules

- **Content first.** Browser pages use the full viewport. Hide unnecessary chrome while reading.
- **Quiet new-tab canvas.** No giant logo, hero gradient, or dashboard of cards. A new tab opens directly into a focused search field.
- **Three-part floating dock.** Tabs on the left, a compact search pill in the centre, and an upward chevron on the right for page controls.
- **Soft, neutral surfaces.** AMOLED dark surfaces are the default for Xeno, with restrained contrast and thin borders. No glowing neon outlines around every element.
- **Pills are purposeful.** Use fully rounded geometry for the search field and dock controls; use 20–28dp corners for larger sheets and cards.
- **Motion conveys state.** Overlays scale and fade with a spring. Avoid large, mechanical edge-slides or bouncy movement that delays interaction.

## Main screen anatomy

| Surface | Required behaviour |
|---|---|
| New tab | Quiet, nearly empty canvas; dock remains the visual anchor |
| Search | Opens from the centre dock action, focuses the field, and raises the keyboard |
| Search intent | URLs navigate; questions can be sent to Ask without changing to a separate app |
| Page dock | Tabs / search / page-controls chevron, with no permanent address toolbar |
| Page controls | Up-chevron expands address, back/forward, reload, reader and more actions |
| Scrolling | Dock fades and scales away while scrolling down, then returns when scrolling up |
| Tab switcher | Native-feeling horizontal carousel with in-memory page previews |
| Ask result | Sources and answer appear progressively; source taps navigate in the same browser |

## Motion specs

Use Compose-native transitions with predictable interruption and cancellation:

```kotlin
// Dock appearance: restrained scale + fade
enter = fadeIn(tween(170)) + scaleIn(
    initialScale = 0.94f,
    animationSpec = tween(210)
)
exit = fadeOut(tween(130)) + scaleOut(
    targetScale = 0.97f,
    animationSpec = tween(150)
)

// Search and sheet overlays should use a spring where appropriate.
spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
```

Keep transitions short. Search focus and keyboard opening must not wait for a decorative animation to finish.

## Interaction details

1. Tapping the centre search pill creates a fresh tab and opens the focused search field.
2. Back dismisses the top-most surface before navigating back or leaving the app.
3. The chevron reveals browser tools without pinning an address bar over every page.
4. A failed navigation must show a useful retry action instead of leaving a blank, unresponsive screen.
5. Web content must continue receiving keyboard input and touch gestures; app chrome must not steal either.

## Non-negotiables

- Don't put a large Xeno logo in the centre of the new-tab page.
- Don't substitute animation polish for working navigation, extraction, or search.
- Preserve Android-native keyboard, back, permission and intent behaviour.
- Keep page previews in memory only. Never silently create a screenshot history.
- Keep the implementation light: System WebView, Compose, and existing dependencies.
