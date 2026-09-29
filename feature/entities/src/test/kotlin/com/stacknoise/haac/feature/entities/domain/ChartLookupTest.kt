package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.feature.entities.ui.customWindow
import com.stacknoise.haac.feature.entities.ui.presetWindow
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Axis steps, lookups for the readout and the history windows. */
class ChartLookupTest {
    @Test
    fun `the axis uses round steps around the values`() {
        assertEquals(ValueAxis(18.0, 22.0, listOf(18.0, 19.0, 20.0, 21.0, 22.0)), valueAxis(18.4, 21.7))
        assertEquals(0.0, valueAxis(0.4, 3.1, fromZero = true).min)
        val flat = valueAxis(20.0, 20.0)
        assertEquals(true, flat.max > flat.min)
    }

    @Test
    fun `lookups find the value at a time`() {
        val points = listOf(ChartPoint(0, 1.0), ChartPoint(10, 2.0), ChartPoint(20, 2.0))
        val line = LineSeries(SeriesKind.VALUE, listOf(points))
        assertEquals(1.0, line.valueAt(5))
        assertEquals(2.0, line.valueAt(15))
        assertNull(line.valueAt(25))
        assertEquals(Bar(10, 20, 3.0), listOf(Bar(0, 10, 1.0), Bar(10, 20, 3.0)).barAt(10))
        assertEquals("on", listOf(Segment(0, 10, "off"), Segment(10, 20, "on")).segmentAt(12)?.state)
    }

    @Test
    fun `windows cover the presets and whole custom days`() {
        val week = presetWindow(HistoryPreset.WEEK, 1_000_000_000)
        assertEquals(HistoryWindow(1_000_000_000 - 604_800_000, 1_000_000_000, HistoryPreset.WEEK), week)
        val zone = ZoneId.of("Europe/Berlin")
        val sep27 = 1_790_467_200_000L // 2026-09-27T00:00Z, as the picker passes the day
        val sep28 = sep27 + 86_400_000
        val now = ZonedDateTime.of(2026, 9, 29, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        val window = customWindow(sep27, sep28, now, zone)
        assertEquals(ZonedDateTime.of(2026, 9, 27, 0, 0, 0, 0, zone).toInstant().toEpochMilli(), window.start)
        assertEquals(ZonedDateTime.of(2026, 9, 29, 0, 0, 0, 0, zone).toInstant().toEpochMilli(), window.end)
        assertEquals(now, customWindow(sep28, sep28 + 86_400_000, now, zone).end)
    }
}
