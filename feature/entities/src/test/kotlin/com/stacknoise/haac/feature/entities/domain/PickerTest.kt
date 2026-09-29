package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.assignment.TileSize
import com.stacknoise.haac.core.database.layout.Floor
import com.stacknoise.haac.core.database.layout.Home
import com.stacknoise.haac.core.database.layout.Places
import com.stacknoise.haac.core.database.layout.Room
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PickerTest {
    private val places = Places(
        homes = listOf(Home("h1", "Main house"), Home("h2", "Garden house")),
        floors = listOf(Floor("f0", "h1", "Ground floor", 0)),
        rooms = listOf(
            Room("living", "h1", "f0", "Living room"),
            Room("kitchen", "h1", "f0", "Kitchen"),
            Room("hall", "h1", null, "Hall"),
            Room("shed", "h2", null, "Shed"),
        ),
    )

    private fun entry(id: String, name: String, vararg rooms: String) = CatalogEntry(
        id.substringBefore('.'),
        Tile(id, name, TileIcon.SWITCH, TileSize.SMALL, TileContent.Switch(true)),
        rooms.toSet(),
    )

    private val entries = listOf(
        entry("sensor.t", "Temperature"),
        entry("light.x", "Light"),
        entry("switch.coffee", "Coffee machine", "kitchen"),
        entry("switch.lamp", "Floor lamp", "living"),
        entry("climate.r", "Radiator"),
    )

    private fun ids(filter: String) = pickerRows(entries, "living", places, "switch", filter).map { it.entry.entityId }

    @Test
    fun `tabs follow switch, sensor, climate, then other domains`() {
        assertEquals(listOf("switch", "sensor", "climate", "light"), pickerDomains(entries))
    }

    @Test
    fun `rows of a tab show whether an entity is here or in another room`() {
        val rows = pickerRows(entries, "living", places, "switch", "")
        assertEquals(listOf("switch.coffee", "switch.lamp"), rows.map { it.entry.entityId })
        assertEquals("Kitchen", rows[0].otherRoom)
        assertEquals(true, rows[1].inRoom)
        assertNull(rows[1].otherRoom)
    }

    @Test
    fun `the filter matches name or entity id, ignoring case`() {
        assertEquals(listOf("switch.lamp"), ids(" LAMP "))
        assertEquals(listOf("switch.coffee"), ids("coffee"))
    }

    @Test
    fun `room groups list levels, then rooms without a level, and fall back to the first room`() {
        val groups = places.roomGroups()
        assertEquals(listOf("Ground floor", null, null), groups.map { it.floor?.name })
        assertEquals(listOf("living", "kitchen"), groups[0].rooms.map { it.id })
        assertEquals("hall", groups.roomOrFirst("hall")?.id)
        assertEquals("living", groups.roomOrFirst("deleted")?.id)
        assertEquals("Garden house", groups.groupOf("shed")?.home?.name)
    }
}
