package com.dude555afk.xeno.ui.theme

import androidx.compose.ui.graphics.Color

/** Dark slate and electric-violet chrome inspired by Arc Search on Android. */
val XenoBlack = Color(0xFF191821)
val SurfaceCard = Color(0xFF25232D)
val SurfaceCardHigh = Color(0xFF302D39)
val StrokeSubtle = Color(0xFF3D3948)
val TextPrimary = Color(0xFFF7F4FC)
val TextSecondary = Color(0xFFB1ABBF)
val TextTertiary = Color(0xFF817A91)
val TextOnAccent = Color(0xFFFFFFFF)

object SpaceAccents {
    val Violet = Color(0xFF5D4BE8)
    val Cyan = Color(0xFF39B8D3)
    val Coral = Color(0xFFF07888)
    val Lime = Color(0xFFD5EF72)
    val Amber = Color(0xFFF5B85D)
    val Rose = Color(0xFFE79AC8)
    val all = listOf(Violet, Cyan, Coral, Lime, Amber, Rose)
    fun forIndex(index: Int): Color = all[((index % all.size) + all.size) % all.size]
}

val DefaultAccent = SpaceAccents.Violet
