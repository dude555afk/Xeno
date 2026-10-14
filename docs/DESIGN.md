# Design language

Borrowed from Arc, translated for touch. The rules below are the ones that
actually make the UI feel considered.

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

## The Arc anatomy, translated

| Arc | Xeno |
|---|---|
| Left sidebar tabs | Bottom sheet, compact vertical pill-cards, swipe-to-dismiss |
| Spaces bar | Bottom nav, swipeable workspaces, each retinting the accent |
| Command bar | Full-screen sheet, blurred backdrop, pill input docked at bottom |
| Little Arc | Draggable peek bubble, 28dp radius, shadow + blur |
| Spring physics | `spring(dampingRatio = 0.6f)` on overlay entrances |

## The three details that sell it

**1. Accent-tinted everything.**
Not "themed." The space's color bleeds into the scroll glow, the caret, the
selection handles, the active tab pill. Instant identity switch, and almost
nobody bothers to do it.

**2. Spring entrances, never slides.**
Command palette and peek bubble scale from 0.92 with overshoot while the
backdrop fades over 180ms. Nothing linear. Nothing that slides in from an
edge like an Android dialog.

**3. Chrome hides.**
Scroll down and the bottom bar dissolves to a bare pill. Scroll up and it
blooms back. Content-first, always.

## Motion specs

```kotlin
// overlay entrance
spring(dampingRatio = 0.65f, stiffness = 400f)
durationMillis = 240
enter = scale(0.92f) + fadeIn

// backdrop fade
tween(180)

// chrome hide / show
tween(200, easing = FastOutSlowInEasing)
```

## Non-negotiables

- Material 3 foundation, but the theme is ours — don't ship defaults.
- Translucency needs a real backdrop. Blur nothing and it looks like a bug.
- Everything is rounded. If a corner is sharp, it's a mistake.
