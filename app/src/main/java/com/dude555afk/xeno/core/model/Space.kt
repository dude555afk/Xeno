package com.dude555afk.xeno.core.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * A browsing space — Arc's spaces. Owns an accent colour that retints the UI,
 * and holds its own set of tabs.
 *
 * Deliberately tiny: a space is an identity, not a feature. Adding notes,
 * profiles or sync here is how v1 never ships.
 */
data class Space(
    val id: Long = 0L,
    val name: String,
    val accent: Color,
    val position: Int = 0,
    val pinned: Boolean = false
)

/** Placeholder space created on first launch so the UI always has something. */
fun defaultSpace() = Space(
    id = 1L,
    name = "Home",
    accent = com.dude555afk.xeno.ui.theme.DefaultAccent,
    position = 0
)

/** Inverse of [Space.accent] for the Room round-trip. */
fun Space.toEntity(): com.dude555afk.xeno.core.storage.SpaceEntity =
    com.dude555afk.xeno.core.storage.SpaceEntity(
        id = id,
        name = name,
        accentArgb = accent.toArgb(),
        position = position,
        pinned = pinned
    )
