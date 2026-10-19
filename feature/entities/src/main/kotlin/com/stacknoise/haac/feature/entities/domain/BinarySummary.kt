package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.feature.entities.domain.EntityDomains.OFF
import com.stacknoise.haac.feature.entities.domain.EntityDomains.ON

/** How often a binary sensor changed in the shown range and how long it was `on` (concept 8.6). */
data class BinaryStats(val changes: Int, val onMillis: Long)

/**
 * The [BinaryStats] of a timeline: a change is a switch between `on` and `off` (gaps and a repeated state do not
 * count, the state at the start of the range is no change); the duration adds up the `on` segments.
 */
internal fun binaryStats(segments: List<Segment>): BinaryStats {
    val known = segments.filter { it.state == ON || it.state == OFF }
    val changes = known.zipWithNext().count { (before, after) -> before.state != after.state }
    return BinaryStats(changes, known.filter { it.state == ON }.sumOf { it.end - it.start })
}

/** [millis] as "45 min", "2 h" or "3 h 20 min"; under a minute as "0 min". */
fun durationText(millis: Long): String {
    val minutes = millis / MillisPerMinute
    val hours = minutes / MinutesPerHour
    val rest = minutes % MinutesPerHour
    return when {
        hours == 0L -> "$rest min"
        rest == 0L -> "$hours h"
        else -> "$hours h $rest min"
    }
}

private const val MillisPerMinute = 60_000L
private const val MinutesPerHour = 60L
