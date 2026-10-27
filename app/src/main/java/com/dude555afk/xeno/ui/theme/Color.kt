package com.dude555afk.xeno.ui.theme

import androidx.compose.ui.graphics.Color

/** Light Arc-inspired browser chrome; website rendering remains site-controlled. */
val XenoBlack = Color(0xFFF8F6FB)
val SurfaceCard = Color(0xFFFFFFFF)
val SurfaceCardHigh = Color(0xFFF2EFF7)
val StrokeSubtle = Color(0xFFE5E0EA)
val TextPrimary = Color(0xFF292532)
val TextSecondary = Color(0xFF777180)
val TextTertiary = Color(0xFFA49DAD)
val TextOnAccent = Color(0xFFFFFFFF)

object SpaceAccents {
    val Violet = Color(0xFF6656D9)
    val Cyan = Color(0xFF278F9B)
    val Coral = Color(0xFFD45D72)
    val Lime = Color(0xFF6B8D39)
    val Amber = Color(0xFFB77725)
    val Rose = Color(0xFFC05A91)
    val all = listOf(Violet, Cyan, Coral, Lime, Amber, Rose)
    fun forIndex(index: Int): Color = all[((index % all.size) + all.size) % all.size]
}

val DefaultAccent = SpaceAccents.Violet
