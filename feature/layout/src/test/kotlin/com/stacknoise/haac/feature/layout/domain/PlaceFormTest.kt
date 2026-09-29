package com.stacknoise.haac.feature.layout.domain

import com.stacknoise.haac.core.database.layout.Floor
import com.stacknoise.haac.core.database.layout.Home
import com.stacknoise.haac.core.database.layout.Places
import com.stacknoise.haac.core.database.layout.Room
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlaceFormTest {
    private val places = Places(
        homes = listOf(Home("h1", "Main house"), Home("h2", "Garden house")),
        floors = listOf(Floor("f0", "h1", "Ground floor", 0), Floor("f1", "h1", "First floor", 1)),
        rooms = listOf(
            Room("r1", "h1", "f1", "Office"),
            Room("r2", "h1", null, "Garage"),
            Room("r3", "h2", null, "Shed"),
        ),
    )

    @Test
    fun `a home is preselected only when there is exactly one`() {
        assertNull(PlaceForm.create(PlaceKind.ROOM, places).homeId)
        val single = places.copy(homes = places.homes.take(1))
        val level = PlaceForm.create(PlaceKind.FLOOR, single)
        assertEquals("h1", level.homeId)
        assertEquals(2, level.level)
        assertNull(PlaceForm.create(PlaceKind.HOME, single).homeId)
    }

    @Test
    fun `levels and rooms need a name and a home`() {
        val room = PlaceForm(PlaceKind.ROOM, name = "Kitchen")
        assertFalse(room.canSave)
        assertTrue(room.withHome("h1", places).canSave)
        assertFalse(room.copy(name = " ", homeId = "h1").canSave)
        assertTrue(PlaceForm(PlaceKind.HOME, name = "Flat").canSave)
    }

    @Test
    fun `another home clears a room's level and a new level's rooms`() {
        val room = checkNotNull(PlaceForm.edit(PlaceKind.ROOM, "r1", places))
        assertEquals("f1", room.withHome("h1", places).floorId)
        assertNull(room.withHome("h2", places).floorId)

        val level = PlaceForm(PlaceKind.FLOOR, name = "Attic", homeId = "h1", level = 2, roomIds = setOf("r2"))
        val moved = level.withHome("h2", places)
        assertEquals(emptySet<String>(), moved.roomIds)
        assertEquals(0, moved.level)
    }

    @Test
    fun `editing a level starts with its rooms, a missing place gives no form`() {
        val form = checkNotNull(PlaceForm.edit(PlaceKind.FLOOR, "f1", places))
        assertEquals(setOf("r1"), form.roomIds)
        assertEquals(setOf("r1", "r2"), form.toggleRoom("r2").roomIds)
        assertEquals(emptySet<String>(), form.toggleRoom("r1").roomIds)
        assertNull(PlaceForm.edit(PlaceKind.ROOM, "gone", places))
    }

    @Test
    fun `new rooms are trimmed, blank ones ignored and removable`() {
        val form = PlaceForm(PlaceKind.HOME).addNewRoom(" Pool ").addNewRoom("  ").addNewRoom("Shed")
        assertEquals(listOf("Pool", "Shed"), form.newRooms)
        assertEquals(listOf("Shed"), form.removeNewRoom(0).newRooms)
    }
}
