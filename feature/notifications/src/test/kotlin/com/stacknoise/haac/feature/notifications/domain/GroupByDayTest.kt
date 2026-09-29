package com.stacknoise.haac.feature.notifications.domain

import com.stacknoise.haac.core.database.notification.NotificationType
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GroupByDayTest {
    private val zone = ZoneId.of("Europe/Vienna")

    private fun at(day: Int, hour: Int, id: Long) = NotificationItem(
        id = id,
        type = NotificationType.ADDED,
        createdAt = ZonedDateTime.of(2026, 9, day, hour, 0, 0, 0, zone).toInstant().toEpochMilli(),
        unread = true,
        resolved = false,
    )

    @Test
    fun `entries are grouped by local day and keep their order`() {
        val days = groupByDay(listOf(at(29, 9, 3), at(29, 0, 2), at(28, 23, 1)), zone)
        assertEquals(listOf(LocalDate.of(2026, 9, 29), LocalDate.of(2026, 9, 28)), days.map { it.date })
        assertEquals(listOf(3L, 2L), days.first().items.map { it.id })
    }
}
