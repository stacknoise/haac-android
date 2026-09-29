package com.stacknoise.haac.feature.layout.data

import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.feature.layout.domain.PlaceForm
import com.stacknoise.haac.feature.layout.domain.PlaceKind
import com.stacknoise.haac.feature.layout.domain.Places
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PlaceTrashTest {
    private val tables = FakeLayoutTables()
    private val errors = DefaultErrorFactory()
    private var nextId = 0
    private var now = 1_000_000L
    private val writer = PlaceWriter(tables.daos, tables.transactions, errors) { "id${++nextId}" }
    private val trash = PlaceTrash(tables.daos, tables.trashDao, tables.transactions, errors) { now }
    private val repository = PlaceRepository(tables.homeDao, tables.floorDao, tables.roomDao, errors)

    private suspend fun places(): Places = repository.places("s1").first()

    /** Main house with level Ground holding Kitchen, and Hall directly in the home. */
    private suspend fun mainHouse(): Places {
        val main = writer.save("s1", PlaceForm(PlaceKind.HOME, name = "Main house", newRooms = listOf("Hall")))
        writer.save("s1", PlaceForm(PlaceKind.FLOOR, name = "Ground", homeId = main, newRooms = listOf("Kitchen")))
        return places()
    }

    @Test
    fun `deleting a home hides everything in it until undo restores it`() = runTest {
        val before = mainHouse()
        trash.deleteHome(before.homes.single())

        assertEquals(Places(), places())
        assertEquals("Main house", trash.pending.value?.name)
        trash.undo(checkNotNull(trash.pending.value))
        assertEquals(before, places())
        assertNull(trash.pending.value)
    }

    @Test
    fun `a deleted level keeps its rooms in the home, or takes them along`() = runTest {
        val before = mainHouse()
        trash.deleteFloor(before.floors.single(), withRooms = false)
        assertEquals(listOf(null, null), places().rooms.map { it.floorId })

        now += 1
        trash.expire(checkNotNull(trash.pending.value))
        assertEquals(emptyList<Any>(), tables.floors.value)
        assertEquals(listOf("Hall", "Kitchen"), tables.rooms.value.map { it.name })
        assertEquals(listOf(null, null), tables.rooms.value.map { it.floorId })

        val ground = writer.save("s1", PlaceForm(PlaceKind.FLOOR, name = "Ground", homeId = before.homes.single().id))
        tables.rooms.value = tables.rooms.value.map { it.copy(floorId = ground) }
        trash.deleteFloor(places().floors.single(), withRooms = true)
        trash.expire(checkNotNull(trash.pending.value))
        assertEquals(emptyList<Any>(), tables.rooms.value)
    }

    @Test
    fun `a new deletion ends the previous one, which can no longer be undone`() = runTest {
        val before = mainHouse()
        trash.deleteRoom(before.rooms.first { it.name == "Hall" })
        val first = checkNotNull(trash.pending.value)
        trash.deleteRoom(before.rooms.first { it.name == "Kitchen" })

        trash.undo(first)
        assertEquals(emptyList<Any>(), places().rooms)
        trash.undo(checkNotNull(trash.pending.value))
        assertEquals(listOf("Kitchen"), places().rooms.map { it.name })
        assertEquals(listOf("Kitchen"), tables.rooms.value.map { it.name })
    }

    @Test
    fun `purging expired deletions keeps the pending one`() = runTest {
        val before = mainHouse()
        trash.deleteRoom(before.rooms.first())
        // Left over from a deletion whose window ended while the app was closed.
        tables.homes.value += tables.homes.value.single().copy(id = "old", deletedAt = now - 60_000)

        trash.purgeExpired()
        assertEquals(listOf(before.homes.single().id), tables.homes.value.map { it.id })
        assertEquals(2, tables.rooms.value.size)
    }
}
