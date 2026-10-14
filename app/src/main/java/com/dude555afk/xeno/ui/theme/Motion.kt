package com.dude555afk.xeno.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Motion specs. Overlays spring, they never slide in from an edge like a dialog.
 *
 * See docs/DESIGN.md — "spring entrance on every overlay".
 */
object XenoMotion {

    /** Overlay entrance — command palette, peek bubble, bottom sheets. */
    fun <T> overlaySpring() = spring<T>(
        dampingRatio = 0.65f,
        stiffness = Spring.StiffnessLow
    )

    /** Smaller, snappier spring for pills and chips. */
    fun <T> pillSpring() = spring<T>(
        dampingRatio = 0.75f,
        stiffness = Spring.StiffnessMedium
    )

    /** Bottom sheets — a little less overshoot so they don't bounce about. */
    fun <T> sheetSpring() = spring<T>(
        dampingRatio = 0.82f,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Backdrop fade behind overlays. */
    fun <T> backdropFade() = tween<T>(durationMillis = 180)

    /** Chrome hide / show on scroll. */
    fun <T> chromeReveal() = tween<T>(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )

    /** Typewriter caret blink. */
    fun <T> caretBlink() = tween<T>(durationMillis = 530)
}
