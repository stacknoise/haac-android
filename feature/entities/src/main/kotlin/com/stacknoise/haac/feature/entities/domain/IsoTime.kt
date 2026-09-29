package com.stacknoise.haac.feature.entities.domain

import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** HA's ISO time such as `2026-09-26T07:12:03.123456+00:00` in milliseconds, or null. */
internal fun isoEpochMillis(iso: String?): Long? = try {
    iso?.let { OffsetDateTime.parse(it).toInstant().toEpochMilli() }
} catch (_: DateTimeParseException) {
    null
}
