package com.stacknoise.haac.feature.entities.ui

import com.stacknoise.haac.feature.entities.domain.durationText
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** [at] as date and time in the device's format. */
internal fun absoluteTime(at: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(at))

/** A mode or action name from HA such as `fan_only` as "Fan only"; HA sends these names, not texts. */
internal fun modeLabel(mode: String): String =
    mode.replace('_', ' ').replaceFirstChar { it.titlecase(Locale.getDefault()) }

/** Where a "since" line switches from a duration to a date, in milliseconds. */
private const val SinceRelativeUpToMs = 24 * 3_600_000L

/** [at] as a duration up to a day ("2 h"), then as a date and time (concept 8.6). */
internal fun sinceText(at: Long): String {
    val elapsed = (System.currentTimeMillis() - at).coerceAtLeast(0)
    return if (elapsed < SinceRelativeUpToMs) durationText(elapsed) else absoluteTime(at)
}
