package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.feature.entities.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** The class table, the summary of the timeline and the duration text (concept 8.6). */
class BinaryClassesTest {
    private fun seg(from: Long, to: Long, state: String) = Segment(from, to, state)

    @Test
    fun `the classes of the table have their words, level and icon`() {
        assertEquals(R.string.binary_motion, binaryClass("motion").on)
        assertEquals(R.string.binary_no_motion, binaryClass("motion").off)
        assertEquals(BinaryLevel.ALARM, binaryClass("moisture").level)
        assertEquals(BinaryLevel.ACTIVE, binaryClass("lock").level)
        assertEquals(R.string.binary_unlocked, binaryClass("lock").on)
        assertEquals(BinaryLevel.ALARM, binaryClass("battery").level)
        assertEquals(BinaryLevel.ACTIVE, binaryClass("battery_charging").level)
    }

    @Test
    fun `an unknown or missing class is on and off and active`() {
        listOf(null, "", "something_new").forEach {
            assertEquals(R.string.tile_on, binaryClass(it).on)
            assertEquals(R.string.tile_off, binaryClass(it).off)
            assertEquals(BinaryLevel.ACTIVE, binaryClass(it).level)
        }
    }

    @Test
    fun `changes count switches between on and off and the on time adds up`() {
        val hour = 3_600_000L
        val segments = listOf(
            seg(0, hour, "off"),
            seg(hour, 2 * hour, "on"),
            seg(2 * hour, 3 * hour, "unavailable"),
            seg(3 * hour, 4 * hour, "on"),
            seg(4 * hour, 5 * hour, "off"),
        )
        assertEquals(BinaryStats(changes = 2, onMillis = 2 * hour), binaryStats(segments))
    }

    @Test
    fun `no segments give no changes`() {
        assertEquals(BinaryStats(0, 0), binaryStats(emptyList()))
    }

    @Test
    fun `durations read as minutes, hours or both`() {
        assertEquals("0 min", durationText(30_000))
        assertEquals("45 min", durationText(45 * 60_000L))
        assertEquals("2 h", durationText(2 * 3_600_000L))
        assertEquals("3 h 20 min", durationText(200 * 60_000L))
    }
}
