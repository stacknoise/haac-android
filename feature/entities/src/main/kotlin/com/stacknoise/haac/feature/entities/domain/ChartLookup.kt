package com.stacknoise.haac.feature.entities.domain

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** The value of this line at [at]: the last point at or before it within a segment, or null in a gap. */
fun LineSeries.valueAt(at: Long): Double? =
    segments.firstOrNull { it.isNotEmpty() && at >= it.first().at && at <= it.last().at }
        ?.lastOrNull { it.at <= at }?.value

/** The min/max of the statistics period that contains [at]. */
fun List<RangePoint>.rangeAt(at: Long): RangePoint? = lastOrNull { it.at <= at }

/** The bar whose period contains [at]. */
fun List<Bar>.barAt(at: Long): Bar? = firstOrNull { at >= it.start && at < it.end }

/** The segment that contains [at]. */
fun List<Segment>.segmentAt(at: Long): Segment? = firstOrNull { at >= it.start && at < it.end }

/** Axis of a chart: from [min] to [max] with gridlines at [ticks]. */
data class ValueAxis(val min: Double, val max: Double, val ticks: List<Double>)

/**
 * A readable axis around [low] … [high] with about [count] gridlines at round steps (1, 2 or 5 × 10ⁿ); a flat
 * series gets a range of one step around its value. [fromZero] starts at 0 (bars).
 */
fun valueAxis(low: Double, high: Double, count: Int = 4, fromZero: Boolean = false): ValueAxis {
    val bottom = if (fromZero) minOf(0.0, low) else low
    val span = (high - bottom).takeIf { it > 0 } ?: abs(high).coerceAtLeast(1.0)
    val rough = span / count
    val magnitude = 10.0.pow(floor(log10(rough)))
    val step = Steps.map { it * magnitude }.first { it >= rough }
    val min = floor(bottom / step) * step
    val max = ceil(high / step).let { if (it * step <= min) min + step else it * step }
    val ticks = generateSequence(min) { it + step }.takeWhile { it <= max + step / 2 }.toList()
    return ValueAxis(min, max, ticks)
}

/** The round steps of [valueAxis]. */
private val Steps = listOf(1.0, 2.0, 5.0, 10.0)
