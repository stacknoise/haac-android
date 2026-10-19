package com.stacknoise.haac.feature.schedules.domain

import java.time.LocalDate
import java.time.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScheduleDraftTest {
    private val valid = ScheduleDraft(name = "Morning light", entityIds = listOf("switch.a"))
    private val start = LocalDate.of(2026, 10, 19)

    @Test
    fun `a draft needs a name, a weekday and, for the owner, one to twenty entities`() {
        assertTrue(valid.isValid(withEntities = true))
        assertFalse(valid.copy(name = "   ").isValid(true))
        assertFalse(valid.copy(name = "x".repeat(61)).isValid(true))
        assertFalse(valid.copy(days = emptySet()).isValid(true))
        assertFalse(valid.copy(entityIds = emptyList()).isValid(true))
        assertFalse(valid.copy(entityIds = List(21) { "switch.$it" }).isValid(true))
        assertTrue(valid.copy(entityIds = emptyList()).isValid(withEntities = false))
    }

    @Test
    fun `only a weekly rule needs a weekday and a one-off rule needs its date`() {
        val monthly = valid.copy(days = emptySet(), repeat = RepeatRule(RepeatFreq.MONTHLY, 1, start))
        assertTrue(monthly.isValid(true))
        assertFalse(monthly.copy(repeat = monthly.repeat.copy(endDate = start.minusDays(1))).isValid(true))
        assertFalse(valid.copy(repeat = RepeatRule(RepeatFreq.ONCE)).isValid(true))
        assertTrue(valid.copy(repeat = RepeatRule(RepeatFreq.ONCE, 1, start)).isValid(true))
        assertFalse(valid.copy(repeat = RepeatRule(RepeatFreq.ONCE, 1, start, start.plusDays(1))).isValid(true))
    }

    @Test
    fun `the weekdays count for the trigger of a weekly rule only`() {
        val daily = valid.copy(repeat = RepeatRule(RepeatFreq.DAILY))
        assertFalse(daily.triggerDiffers(daily.copy(days = setOf(5))))
        assertTrue(valid.triggerDiffers(valid.copy(days = setOf(5))))
    }

    @Test
    fun `switching the frequency fixes the date rule and brings back weekdays for weekly`() {
        val once = valid.withFreq(RepeatFreq.ONCE, start)
        assertEquals(RepeatRule(RepeatFreq.ONCE, 1, start.plusDays(1)), once.repeat)
        val weekly = valid.copy(days = emptySet()).withFreq(RepeatFreq.WEEKLY, start)
        assertEquals(ScheduleDraft.WEEKDAYS, weekly.days)
        val yearly = valid.copy(repeat = RepeatRule(RepeatFreq.DAILY, 100)).withFreq(RepeatFreq.YEARLY, start)
        assertEquals(1, yearly.repeat.interval)
    }

    @Test
    fun `a reload keeps a changed rule and a changed notify switch`() {
        val mine = valid.copy(repeat = RepeatRule(RepeatFreq.DAILY, 2, start), notify = true)
        val fresh = valid.copy(repeat = RepeatRule(RepeatFreq.YEARLY, 1, start), notify = false)
        val merged = mine.rebase(valid, fresh)
        assertEquals(mine.repeat, merged.repeat)
        assertTrue(merged.notify)
        assertEquals(fresh.repeat, valid.rebase(valid, fresh).repeat)
    }

    @Test
    fun `steppers wrap around the clock and keep the other part`() {
        assertEquals(LocalTime.of(0, 30), LocalTime.of(23, 30).plusHoursWrapped(1))
        assertEquals(LocalTime.of(23, 30), LocalTime.of(0, 30).plusHoursWrapped(-1))
        assertEquals(LocalTime.of(6, 0), LocalTime.of(6, 59).plusMinutesWrapped(ScheduleDraft.MINUTE_STEP))
        assertEquals(LocalTime.of(6, 59), LocalTime.of(6, 0).plusMinutesWrapped(-ScheduleDraft.MINUTE_STEP))
    }

    @Test
    fun `the offset moves in steps of five and stops at 180`() {
        assertEquals(5, stepOffset(0, 1))
        assertEquals(-5, stepOffset(0, -1))
        assertEquals(180, stepOffset(180, 1))
        assertEquals(-180, stepOffset(-178, -1))
    }

    @Test
    fun `a reload keeps what the user changed and takes the rest from the server`() {
        val original = valid
        val mine = original.copy(name = "Evening light")
        val fresh = original.copy(name = "Renamed elsewhere", time = LocalTime.of(8, 0), entityIds = listOf("switch.b"))
        val merged = mine.rebase(original, fresh)
        assertEquals("Evening light", merged.name)
        assertEquals(LocalTime.of(8, 0), merged.time)
        assertEquals(listOf("switch.b"), merged.entityIds)
    }

    @Test
    fun `the trigger differs by time for fixed times and by offset for sun events`() {
        assertFalse(valid.triggerDiffers(valid.copy(offsetMin = 30)))
        assertTrue(valid.triggerDiffers(valid.copy(time = LocalTime.of(8, 0))))
        val sun = valid.copy(whenType = WhenType.SUNRISE)
        assertFalse(sun.triggerDiffers(sun.copy(time = LocalTime.of(8, 0))))
        assertTrue(sun.triggerDiffers(sun.copy(offsetMin = 30)))
    }
}
