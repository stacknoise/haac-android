package com.stacknoise.haac.feature.schedules.domain

import java.time.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScheduleDraftTest {
    private val valid = ScheduleDraft(name = "Morning light", entityIds = listOf("switch.a"))

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
    fun `steppers wrap around the clock and keep the other part`() {
        assertEquals(LocalTime.of(0, 30), LocalTime.of(23, 30).plusHoursWrapped(1))
        assertEquals(LocalTime.of(23, 30), LocalTime.of(0, 30).plusHoursWrapped(-1))
        assertEquals(LocalTime.of(6, 0), LocalTime.of(6, 55).plusMinutesWrapped(5))
        assertEquals(LocalTime.of(6, 55), LocalTime.of(6, 0).plusMinutesWrapped(-5))
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
