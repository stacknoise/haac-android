package com.stacknoise.haac.core.common.ui.theme

/** Accent colours that tell instances apart (concept 4.4); the first one is the app accent. */
object InstanceAccents {
    /** ARGB values in the order new instances receive them. */
    val Palette: List<Long> = listOf(
        0xFF4DFF7A,
        0xFF4DC3FF,
        0xFFFFB84D,
        0xFFFF6B8B,
        0xFFB38CFF,
        0xFFFFE14D,
    )

    /** The accent for the instance with position [index], repeating after the last colour. */
    fun forIndex(index: Int): Long = Palette[Math.floorMod(index, Palette.size)]
}
