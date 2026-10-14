package com.dude555afk.xeno.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
nimport androidx.compose.animation.core.Easing
nimport androidx.compose.animation.core.FastOutSlowInEasing
nimport androidx.compose.animation.core.Spring
nimport androidx.compose.animation.core.spring
nimport androidx.compose.animation.core.tween
nimport androidx.compose.animation.fadeIn
nimport androidx.compose.animation.fadeOut
nimport androidx.compose.animation.scaleIn
nimport androidx.compose.animation.scaleOut
nimport androidx.compose.animation.slideInVertically
nimport androidx.compose.animation.slideOutVertically
nimport androidx.compose.animation.AnimatedContentTransitionScope
nimport androidx.compose.ui.unit.IntOffset

/**
 * Motion specs. Spring entrances, never slides.
 */
object XenoMotion {
    /** Overlay entrance — command palette, peek bubble, sheets. */
    fun <T> overlaySpring() = spring<T>(
        dampingRatio = 0.65f,
        stiffness = 400f
    )

    /** Backdrop fade behind overlays. */
    fun <T> backdropFade() = tween<T>(durationMillis = 180)

    /** Chrome hide/show on scroll. */
    fun <T> chromeReveal() = tween<T>(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )

    /** Sidebar */
    fun <T> pillSpring() = spring<T>(
        dampingRatio = 0.75f,
        stiffness = 600f
    )

    val EmphasisEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

/**
 * Content — screens.
 */
object XenoTransitions {
    /** Space switching */
    fun <S> spaceFlip(): AnimatedContentTransitionScope<S>.() -> ContentTransform? = {
        (scaleIn(initialScale = 0.96f, animationSpec = tween(180)) + fadeIn(tween(180))).togetherWith(
            fadeOut(tween(120))
        )
    }
}
