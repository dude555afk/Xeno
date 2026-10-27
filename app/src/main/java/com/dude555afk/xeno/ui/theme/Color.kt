package com.dude555afk.xeno.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens. See docs/DESIGN.md.
 *
 * The palette is intentionally tiny — black, translucent white surfaces, and a
 * per-space accent that bleeds into everything (caret, scroll glow, active
 * pill). Arc's trick is that the accent is never just "a color", it is the
 * identity of the workspace.
 */

// Base — true AMOLED black, never #0A0A0A.
val XenoBlack = Color(0xFF000000)

// Static surfaces. Most surfaces are AccentSurfaces, see [XenoTheme].
val SurfaceCard = Color(0x0FFFFFFF)      // white @ 6%
val SurfaceCardHigh = Color(0x1AFFFFFF)  // white @ 10%
val StrokeSubtle = Color(0x14FFFFFF)     // white @ 8%, 1dp on every card
val TextPrimary = Color(0xFFF2F2F2)
val TextSecondary = Color(0x99FFFFFF)    // white @ 60%
val TextTertiary = Color(0x66FFFFFF)     // white @ 40%
val TextOnAccent = Color(0xFF000000)

/**
 * Space accents. Each space owns one, and the whole UI retints with it.
 */
object SpaceAccents {
    val Violet = Color(0xFF7C5CFF)
    val Cyan = Color(0xFF00D8D8)
    val Coral = Color(0xFFFF6B5C)
    val Lime = Color(0xFFB8FF3D)
    val Amber = Color(0xFFFFB03D)
    val Rose = Color(0xFFFF5CA8)

    val all = listOf(Violet, Cyan, Coral, Lime, Amber, Rose)

    /** Deterministic pick so a space keeps its color across launches. */
    fun forIndex(index: Int): Color = all[((index % all.size) + all.size) % all.size]
}

/** Fallback used before any space is loaded. */
val DefaultAccent = SpaceAccents.Lime
