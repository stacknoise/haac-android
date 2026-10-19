package com.stacknoise.haac.feature.schedules.domain

import com.stacknoise.haac.core.database.schedule.ScheduleEntity
import com.stacknoise.haac.feature.schedules.data.ScheduleView
import java.time.LocalDate
import java.time.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScheduleItemTest {
    private fun row(
        days: Int = 0b0011111,
        whenType: String = "time",
        time: String? = "06:45",
        nextRun: Long? = null,
        paused: String? = null,
        enabled: Boolean = true,
        blockedBy: String = "[]",
        offReason: String? = null,
    ) = ScheduleEntity(
        serverId = "s1",
        scheduleId = "a",
        owner = "u1",
        ownerName = "Anton",
        own = true,
        name = "Morning light",
        enabled = enabled,
        whenType = whenType,
        time = time,
        days = days,
        offsetMin = if (whenType == "time") null else 30,
        action = "turn_on",
        entityIds = """["switch.a","switch.b"]""",
        createdAt = 1,
        updatedAt = "2026-10-01T05:12:00+00:00",
        pausedReason = paused,
        pausedAt = paused?.let { 5 },
        lastRunAt = 7,
        lastRunResult = "partial",
        lastRunCode = "HAB-SCH-002",
        nextRun = nextRun,
        syncedAt = 10,
        blockedBy = blockedBy,
        offReason = offReason,
    )

    @Test
    fun `a cache row becomes an item with typed fields`() {
        val item = row().toItem()
        assertEquals(WhenType.TIME, item.whenType)
        assertEquals(LocalTime.of(6, 45), item.time)
        assertEquals(listOf(0, 1, 2, 3, 4), item.days)
        assertEquals(ScheduleAction.TURN_ON, item.action)
        assertEquals(listOf("switch.a", "switch.b"), item.entityIds)
        assertFalse(item.lastRun?.ok ?: true)
        assertFalse(item.muted)
    }

    @Test
    fun `a deactivated entity blocks the schedule and its name is shown`() {
        val item = row(enabled = false, blockedBy = """["switch.b"]""", offReason = "entity_disabled").toItem()
        assertTrue(item.blocked)
        assertEquals(listOf("switch.b"), item.blockedBy)
        assertEquals("entity_disabled", item.offReason)
        assertEquals(listOf("Fan"), ScheduleView(item, listOf("Lamp", "Fan")).blockedNames)
        assertFalse(row().toItem().blocked)
    }

    @Test
    fun `off or paused schedules are muted`() {
        assertTrue(row(enabled = false).toItem().muted)
        assertTrue(row(paused = "no_entities").toItem().paused)
        assertTrue(row(paused = "no_entities").toItem().muted)
    }

    @Test
    fun `unknown values from a newer bridge and damaged ids do not crash`() {
        val item = row(whenType = "moon", time = "bad").copy(action = "dim", entityIds = "oops").toItem()
        assertEquals(WhenType.TIME, item.whenType)
        assertEquals(null, item.time)
        assertEquals(ScheduleAction.TOGGLE, item.action)
        assertEquals(emptyList<String>(), item.entityIds)
    }

    @Test
    fun `weekdays are classified for the summary`() {
        assertEquals(DayPattern.EVERY_DAY, dayPatternOf((0..6).toList()))
        assertEquals(DayPattern.WEEKDAYS, dayPatternOf(listOf(4, 3, 2, 1, 0)))
        assertEquals(DayPattern.WEEKEND, dayPatternOf(listOf(5, 6)))
        assertEquals(DayPattern.NONE, dayPatternOf(emptyList()))
        assertEquals(DayPattern.CUSTOM, dayPatternOf(listOf(0, 2, 4)))
    }

    @Test
    fun `a row without a repeat rule is weekly every week, without notify and not expired`() {
        val item = row().toItem()
        assertEquals(RepeatRule(RepeatFreq.WEEKLY, 1), item.repeat)
        assertFalse(item.notify)
        assertFalse(item.expired)
        assertEquals(emptyList<Long>(), item.nextRuns)
    }

    @Test
    fun `the repeat rule, notify, expired and next runs of a row are typed`() {
        val monthly = row().copy(
            repeatFreq = "monthly",
            repeatInterval = 2,
            startDate = "2026-10-15",
            endDate = "2026-12-31",
            notify = true,
            expired = true,
            nextRuns = """["2026-10-15T06:45:00+02:00","broken"]""",
        ).toItem()
        val rule = RepeatRule(RepeatFreq.MONTHLY, 2, LocalDate.of(2026, 10, 15), LocalDate.of(2026, 12, 31))
        assertEquals(rule, monthly.repeat)
        assertTrue(monthly.notify)
        assertTrue(monthly.expired)
        assertEquals(listOf(1_792_039_500_000L), monthly.nextRuns)
    }

    @Test
    fun `an unknown frequency from a newer bridge reads as weekly and bad dates as none`() {
        val item = row().copy(repeatFreq = "fortnightly", startDate = "soon", endDate = "").toItem()
        assertEquals(RepeatRule(RepeatFreq.WEEKLY, 1, null, null), item.repeat)
    }

    @Test
    fun `the next runs of the bridge are used, those in the past dropped`() {
        val item = row(nextRun = 5_000).toItem().copy(nextRuns = listOf(1_000L, 5_000L, 9_000L, 12_000L, 20_000L))
        assertEquals(listOf(5_000L, 9_000L, 12_000L), nextRuns(item, now = 2_000))
        assertEquals(listOf(20_000L), nextRuns(item, now = 15_000))
    }

    @Test
    fun `next runs continue on the following matching days at the same time`() {
        // Friday 2026-10-02 06:45 at UTC+2 is 04:45 UTC; Monday to Friday follows on Monday and Tuesday.
        val friday = 1_790_916_300_000L
        val item = row(nextRun = friday).toItem()
        val runs = nextRuns(item, now = friday - 3_600_000)
        assertEquals(listOf(friday, friday + 3 * 86_400_000, friday + 4 * 86_400_000), runs)
    }

    @Test
    fun `nothing is listed without a next run, sun events list only the first`() {
        assertEquals(emptyList<Long>(), nextRuns(row().toItem(), now = 0))
        val sun = row(whenType = "sunrise", time = null, nextRun = 5_000).toItem()
        assertEquals(listOf(5_000L), nextRuns(sun, now = 1_000))
    }

    @Test
    fun `the tab shows with the feature or with cached schedules, not otherwise`() {
        assertTrue(com.stacknoise.haac.feature.schedules.data.tabShown(setOf("schedules"), hasCached = false))
        assertTrue(com.stacknoise.haac.feature.schedules.data.tabShown(emptySet(), hasCached = true))
        assertFalse(com.stacknoise.haac.feature.schedules.data.tabShown(emptySet(), hasCached = false))
    }
}
