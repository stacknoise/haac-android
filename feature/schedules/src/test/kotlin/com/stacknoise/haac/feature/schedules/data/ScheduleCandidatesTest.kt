package com.stacknoise.haac.feature.schedules.data

import com.stacknoise.haac.core.database.assignment.EntityAlias
import com.stacknoise.haac.core.database.assignment.RoomAssignment
import com.stacknoise.haac.core.database.assignment.TileSize
import com.stacknoise.haac.core.database.entity.EntityStatus
import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.database.entity.byEntityId
import com.stacknoise.haac.core.database.layout.Room
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/** The entity names of the schedule picker follow the local names (concept 7.3, 19.7). */
class ScheduleCandidatesTest {
    private val lamp = ExposedEntity("s1", "switch.lamp", "switch", "Floor lamp plug")
    private val fan = ExposedEntity("s1", "switch.fan", "switch", "Fan", configuredName = "Ceiling fan")
    private val rooms = listOf(Room("r1", "h1", null, "Kitchen"))

    private fun names(vararg pairs: Pair<String, String>) =
        pairs.map { EntityAlias("s1", it.first, it.second) }.byEntityId()

    @Test
    fun `a local name replaces the HA name`() {
        val result = candidatesOf(listOf(lamp), names("switch.lamp" to "Reading light"), rooms, emptyList())
        assertEquals("Reading light", result.entities.single().name)
        assertEquals("Reading light", result.nameOf("switch.lamp"))
    }

    @Test
    fun `without a local name the configured name and then the HA name are used`() {
        val result = candidatesOf(listOf(lamp, fan), names(), rooms, emptyList())
        assertEquals(setOf("Ceiling fan", "Floor lamp plug"), result.entities.map { it.name }.toSet())
    }

    @Test
    fun `a blank local name is ignored`() {
        val result = candidatesOf(listOf(lamp), names("switch.lamp" to "  "), rooms, emptyList())
        assertEquals("Floor lamp plug", result.entities.single().name)
    }

    @Test
    fun `the list is sorted by the names the user sees`() {
        val result = candidatesOf(listOf(lamp, fan), names("switch.lamp" to "Aquarium"), rooms, emptyList())
        assertEquals(listOf("Aquarium", "Ceiling fan"), result.entities.map { it.name })
    }

    @Test
    fun `only switches are offered and withdrawn ones are not active`() {
        val sensor = ExposedEntity("s1", "sensor.temp", "sensor", "Temperature")
        val gone = fan.copy(status = EntityStatus.WITHDRAWN)
        val result = candidatesOf(listOf(sensor, gone), names(), rooms, emptyList())
        assertEquals(listOf("switch.fan"), result.entities.map { it.entityId })
        assertFalse(result.entities.single().active)
    }

    @Test
    fun `a candidate knows the rooms it is placed in`() {
        val placed = listOf(
            RoomAssignment("r1", "switch.lamp", 0, TileSize.SMALL, 0L),
            RoomAssignment("gone", "switch.lamp", 1, TileSize.SMALL, 0L),
        )
        val result = candidatesOf(listOf(lamp), names(), rooms, placed)
        assertEquals(listOf("Kitchen"), result.entities.single().rooms.map { it.name })
    }
}
