package com.dude555afk.xeno.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Everything is rounded. If a corner is sharp, it's a mistake.
 *
 * Cards 28dp, inputs 20dp, buttons/pills 100dp (fully rounded).
 */
val XenoShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/** Fully rounded pill — chips, buttons, tab cards. */
val PillShape = RoundedCornerShape(percent = 50)
