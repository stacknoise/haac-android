package com.stacknoise.haac.feature.entities.ui

import android.text.format.DateUtils
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** [at] relative to now, e.g. "5 minutes ago" or "In 3 hours" (concept 8.3). */
internal fun relativeTime(at: Long): String =
    DateUtils.getRelativeTimeSpanString(at, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

/** [at] as date and time in the device's format. */
internal fun absoluteTime(at: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(at))

/** A mode or action name from HA such as `fan_only` as "Fan only"; HA sends these names, not texts. */
internal fun modeLabel(mode: String): String =
    mode.replace('_', ' ').replaceFirstChar { it.titlecase(Locale.getDefault()) }
