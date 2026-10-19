package com.stacknoise.haac.feature.schedules.domain

import java.time.LocalDate
import java.time.YearMonth
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RepeatTest {
    private val today = LocalDate.of(2026, 10, 19)

    @Test
    fun `frequencies are looked up by the bridge name and an unknown one reads as weekly`() {
        assertEquals(RepeatFreq.MONTHLY, RepeatFreq.of("monthly"))
        assertEquals(RepeatFreq.WEEKLY, RepeatFreq.of("fortnightly"))
    }

    @Test
    fun `only daily, weekly and monthly have an interval stepper with the limits of the bridge`() {
        assertEquals(
            listOf(false, true, true, true, false),
            RepeatFreq.entries.map { it.hasInterval },
        )
        assertEquals(listOf(1, 365, 52, 12, 1), RepeatFreq.entries.map { it.maxInterval })
    }

    @Test
    fun `a one-off rule needs a date and no end, an end is not before the start`() {
        assertFalse(RepeatRule(RepeatFreq.ONCE).valid)
        assertTrue(RepeatRule(RepeatFreq.ONCE, 1, today).valid)
        assertFalse(RepeatRule(RepeatFreq.ONCE, 1, today, today.plusDays(1)).valid)
        assertTrue(RepeatRule(RepeatFreq.DAILY).valid)
        assertTrue(RepeatRule(RepeatFreq.DAILY, 1, today, today).valid)
        assertFalse(RepeatRule(RepeatFreq.DAILY, 1, today, today.minusDays(1)).valid)
    }

    @Test
    fun `a rule has no upper limit for its end date`() {
        assertTrue(RepeatRule(RepeatFreq.YEARLY, 1, today, today.plusYears(50)).valid)
    }

    @Test
    fun `switching to once gives tomorrow, keeps a later date and drops the end`() {
        val repeating = RepeatRule(RepeatFreq.WEEKLY, 3, today.minusDays(5), today.plusDays(30))
        assertEquals(RepeatRule(RepeatFreq.ONCE, 1, today.plusDays(1)), repeating.switchedTo(RepeatFreq.ONCE, today))
        val later = repeating.copy(startDate = today.plusDays(9))
        assertEquals(today.plusDays(9), later.switchedTo(RepeatFreq.ONCE, today).startDate)
        assertNull(later.switchedTo(RepeatFreq.ONCE, today).endDate)
    }

    @Test
    fun `switching between repeating frequencies keeps the dates and fits the interval`() {
        val daily = RepeatRule(RepeatFreq.DAILY, 100, today, today.plusDays(9))
        val weekly = RepeatRule(RepeatFreq.WEEKLY, 52, today, today.plusDays(9))
        assertEquals(weekly, daily.switchedTo(RepeatFreq.WEEKLY, today))
        assertEquals(12, daily.switchedTo(RepeatFreq.MONTHLY, today).interval)
        assertEquals(1, daily.switchedTo(RepeatFreq.YEARLY, today).interval)
    }

    @Test
    fun `the interval stays between 1 and the limit`() {
        val weekly = RepeatRule(RepeatFreq.WEEKLY, 2)
        assertEquals(3, weekly.steppedBy(1).interval)
        assertEquals(1, weekly.steppedBy(-5).interval)
        assertEquals(52, weekly.steppedBy(100).interval)
    }

    @Test
    fun `a new start date drops an end date before it`() {
        val rule = RepeatRule(RepeatFreq.DAILY, 1, today, today.plusDays(10))
        assertEquals(today.plusDays(10), rule.startingOn(today.plusDays(3)).endDate)
        assertNull(rule.startingOn(today.plusDays(11)).endDate)
    }

    @Test
    fun `dates of the bridge are read, bad ones are none`() {
        assertEquals(today, parseDate("2026-10-19"))
        assertNull(parseDate("19.10.2026"))
        assertNull(parseDate(null))
    }

    @Test
    fun `the calendar of a month starts on Monday with empty cells before the first day`() {
        val october = monthCells(YearMonth.of(2026, 10))
        assertEquals(listOf(null, null, null, LocalDate.of(2026, 10, 1)), october.take(4))
        assertEquals(3 + 31, october.size)
        assertEquals(LocalDate.of(2026, 10, 31), october.last())
        assertEquals(LocalDate.of(2026, 6, 1), monthCells(YearMonth.of(2026, 6)).first())
    }

    @Test
    fun `days can be picked within the limits`() {
        val min = today
        val max = today.plusDays(10)
        assertFalse(isPickable(today.minusDays(1), min, max))
        assertTrue(isPickable(min, min, max))
        assertTrue(isPickable(max, min, max))
        assertFalse(isPickable(max.plusDays(1), min, max))
        assertTrue(isPickable(today.plusYears(30), min, null))
    }

    @Test
    fun `the ordinal of a day follows the last digit except for 11 to 13`() {
        val first = listOf(1, 21, 31)
        assertEquals(first, (1..31).filter { ordinalOf(it) == Ordinal.FIRST })
        assertEquals(listOf(2, 22), (1..31).filter { ordinalOf(it) == Ordinal.SECOND })
        assertEquals(listOf(3, 23), (1..31).filter { ordinalOf(it) == Ordinal.THIRD })
        assertEquals(listOf(11, 12, 13), (11..13).filter { ordinalOf(it) == Ordinal.OTHER })
        assertEquals(Ordinal.OTHER, ordinalOf(15))
    }
}
