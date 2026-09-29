package com.stacknoise.haac.feature.entities.ui

import com.stacknoise.haac.feature.entities.domain.ValueAxis

/** Pixel geometry of a plot: time [start] … [end] runs left to right, [axis] (if any) bottom to top. */
internal class PlotArea(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val start: Long,
    val end: Long,
    val axis: ValueAxis?,
) {
    /** The x position of time [at]. */
    fun x(at: Long): Float = left + (at - start).toFloat() / (end - start).coerceAtLeast(1) * (right - left)

    /** The y position of [value]; the bottom without an axis. */
    fun y(value: Double): Float {
        val range = axis ?: return bottom
        return bottom - ((value - range.min) / (range.max - range.min)).toFloat() * (bottom - top)
    }

    /** The time at x position [x], kept within the period. */
    fun timeAt(x: Float): Long =
        (start + ((x - left) / (right - left)).coerceIn(0f, 1f) * (end - start)).toLong()
}
