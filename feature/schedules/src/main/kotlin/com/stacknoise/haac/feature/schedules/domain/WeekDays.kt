package com.stacknoise.haac.feature.schedules.domain

import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** The weekdays of a schedule as the bitmask of the cache: bit 0 is Monday, bit 6 Sunday (concept 12, 19.2). */
object WeekDays {
    /** Number of days of a week. */
    const val COUNT = 7

    /** The bitmask of [days], each 0 (Monday) to 6 (Sunday); values outside that range are ignored. */
    fun toMask(days: Collection<Int>): Int =
        days.filter { it in 0 until COUNT }.fold(0) { mask, day -> mask or (1 shl day) }

    /** The days of [mask], ascending. */
    fun fromMask(mask: Int): List<Int> = (0 until COUNT).filter { mask and (1 shl it) != 0 }
}

/** The bridge's ISO time such as `2026-10-01T05:12:00+00:00` in milliseconds, or null if it is missing or malformed. */
fun isoEpochMillis(iso: String?): Long? = try {
    iso?.let { OffsetDateTime.parse(it).toInstant().toEpochMilli() }
} catch (_: DateTimeParseException) {
    null
}
