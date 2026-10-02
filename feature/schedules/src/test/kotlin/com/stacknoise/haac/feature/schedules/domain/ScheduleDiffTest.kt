package com.stacknoise.haac.feature.schedules.domain

import com.stacknoise.haac.core.database.schedule.ScheduleEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ScheduleDiffTest {
    private fun row(id: String, name: String = id, own: Boolean = true, reason: String? = null, at: Long? = null) =
        ScheduleEntity(
            serverId = "s1",
            scheduleId = id,
            owner = "u1",
            own = own,
            name = name,
            enabled = true,
            whenType = "time",
            time = "06:45",
            days = 31,
            action = "turn_on",
            entityIds = "[]",
            createdAt = 1,
            updatedAt = "2026-10-01T05:12:00+00:00",
            pausedReason = reason,
            pausedAt = at,
            syncedAt = 10,
        )

    @Test
    fun `an empty cache announces nothing, even a paused schedule`() {
        val change = ScheduleDiff.compute(emptyList(), listOf(row("a", reason = "no_entities", at = 5)))
        assertEquals(listOf("a"), change.rows.map { it.scheduleId })
        assertEquals(emptyList<String>(), change.removedNames)
        assertEquals(emptyList<PausedSchedule>(), change.paused)
    }

    @Test
    fun `a removed own schedule is announced by name, a foreign one silently`() {
        val change = ScheduleDiff.compute(
            listOf(row("a", "Morning light"), row("b", "Hers", own = false), row("c")),
            listOf(row("c")),
        )
        assertEquals(listOf("a", "b"), change.removedIds)
        assertEquals(listOf("Morning light"), change.removedNames)
    }

    @Test
    fun `a newly paused own schedule is announced once`() {
        val before = row("a", "Lamp")
        val paused = row("a", "Lamp", reason = "owner_inactive", at = 5)
        val announced = ScheduleDiff.compute(listOf(before), listOf(paused)).paused
        assertEquals(listOf(PausedSchedule("Lamp", "owner_inactive")), announced)
        assertEquals(emptyList<PausedSchedule>(), ScheduleDiff.compute(listOf(paused), listOf(paused)).paused)
        val again = paused.copy(pausedAt = 9)
        assertEquals(1, ScheduleDiff.compute(listOf(paused), listOf(again)).paused.size)
    }

    @Test
    fun `a paused foreign schedule is not announced`() {
        val change = ScheduleDiff.compute(
            listOf(row("a", own = false)),
            listOf(row("a", own = false, reason = "no_entities", at = 5)),
        )
        assertEquals(emptyList<PausedSchedule>(), change.paused)
    }

    @Test
    fun `unchanged rows are not written again, changed and new ones are`() {
        val same = row("a")
        val change = ScheduleDiff.compute(
            listOf(same, row("b")),
            listOf(same.copy(syncedAt = 99), row("b").copy(name = "Renamed"), row("c")),
        )
        assertEquals(listOf("b", "c"), change.rows.map { it.scheduleId })
    }

    @Test
    fun `weekday masks round-trip and ignore values outside the week`() {
        assertEquals(0b0011111, WeekDays.toMask(listOf(0, 1, 2, 3, 4)))
        assertEquals(listOf(5, 6), WeekDays.fromMask(WeekDays.toMask(listOf(6, 5, 7, -1))))
    }
}
