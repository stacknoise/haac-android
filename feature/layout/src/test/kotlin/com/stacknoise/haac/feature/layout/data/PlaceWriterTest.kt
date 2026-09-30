package com.stacknoise.haac.feature.layout.data

import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ValidationException
import com.stacknoise.haac.feature.layout.domain.PlaceForm
import com.stacknoise.haac.feature.layout.domain.PlaceKind
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PlaceWriterTest {
    private val tables = FakeLayoutTables()
    private var nextId = 0
    private val writer = PlaceWriter(tables.daos, tables.transactions, DefaultErrorFactory()) { "id${++nextId}" }

    private suspend fun home(name: String) = writer.save("s1", PlaceForm(PlaceKind.HOME, name = name))

    private fun room(name: String) = tables.rooms.value.single { it.name == name }

    @Test
    fun `the icon is stored on create and changed or cleared on edit`() = runTest {
        val main = writer.save("s1", PlaceForm(PlaceKind.HOME, name = "Main", icon = "home"))
        val kitchen = writer.save("s1", PlaceForm(PlaceKind.ROOM, name = "Kitchen", homeId = main, icon = "kitchen"))
        val ground = writer.save("s1", PlaceForm(PlaceKind.FLOOR, name = "Ground", homeId = main, icon = "layers"))
        assertEquals(listOf("home"), tables.homes.value.map { it.icon })
        assertEquals("kitchen", room("Kitchen").icon)
        assertEquals("layers", tables.floors.value.single().icon)

        writer.save("s1", PlaceForm(PlaceKind.ROOM, kitchen, "Kitchen", main, icon = "dining"))
        writer.save("s1", PlaceForm(PlaceKind.FLOOR, ground, "Ground", main))
        writer.save("s1", PlaceForm(PlaceKind.HOME, main, "Main", icon = "cabin"))
        assertEquals("dining", room("Kitchen").icon)
        assertEquals(null, tables.floors.value.single().icon)
        assertEquals("cabin", tables.homes.value.single().icon)
    }

    @Test
    fun `a new home with new rooms puts them directly in the home, sorted with gaps`() = runTest {
        home("Main house")
        val id = writer.save("s1", PlaceForm(PlaceKind.HOME, name = " Garden ", newRooms = listOf("Shed", " ", "Pool")))

        assertEquals("Garden", tables.homes.value.single { it.id == id }.name)
        assertEquals(listOf(1024, 2048), tables.homes.value.map { it.sortOrder })
        assertEquals(listOf("Shed", "Pool"), tables.rooms.value.map { it.name })
        assertEquals(listOf(id, id), tables.rooms.value.map { it.homeId })
        assertEquals(listOf(1024, 2048), tables.rooms.value.map { it.sortOrder })
    }

    @Test
    fun `a level links its checked rooms and releases unchecked ones`() = runTest {
        val main = writer.save("s1", PlaceForm(PlaceKind.HOME, name = "Main", newRooms = listOf("Garage", "Office")))
        val first = writer.save("s1", PlaceForm(PlaceKind.FLOOR, name = "First", homeId = main, level = 1))
        writer.save("s1", PlaceForm(PlaceKind.ROOM, room("Office").id, "Office", main, first))

        val attic = writer.save(
            "s1",
            PlaceForm(
                PlaceKind.FLOOR,
                name = "Attic",
                homeId = main,
                level = 2,
                roomIds = setOf(room("Garage").id, room("Office").id),
                newRooms = listOf("Storage"),
            ),
        )
        assertEquals(listOf(attic, attic, attic), listOf("Garage", "Office", "Storage").map { room(it).floorId })

        val officeOnly = setOf(room("Office").id)
        writer.save("s1", PlaceForm(PlaceKind.FLOOR, attic, "Attic", main, level = 3, roomIds = officeOnly))
        assertEquals(null, room("Garage").floorId)
        assertEquals(null, room("Storage").floorId)
        assertEquals(3, tables.floors.value.single { it.id == attic }.level)
    }

    @Test
    fun `a new home takes rooms of another home without their level`() = runTest {
        val main = writer.save("s1", PlaceForm(PlaceKind.HOME, name = "Main", newRooms = listOf("Garage")))
        val garage = setOf(room("Garage").id)
        val ground = writer.save("s1", PlaceForm(PlaceKind.FLOOR, name = "Ground", homeId = main, roomIds = garage))
        assertEquals(ground, room("Garage").floorId)

        val garden = writer.save("s1", PlaceForm(PlaceKind.HOME, name = "Garden", roomIds = setOf(room("Garage").id)))
        assertEquals(garden, room("Garage").homeId)
        assertEquals(null, room("Garage").floorId)
    }

    @Test
    fun `a level of another home or a missing place is rejected and nothing changes`() = runTest {
        val main = home("Main")
        val garden = home("Garden")
        val ground = writer.save("s1", PlaceForm(PlaceKind.FLOOR, name = "Ground", homeId = main))

        val foreign = assertThrows<ValidationException> {
            writer.save("s1", PlaceForm(PlaceKind.ROOM, name = "Shed", homeId = garden, floorId = ground))
        }
        assertEquals(ErrorCode.LAY_PLACE_MISSING, foreign.code)
        val gone = assertThrows<ValidationException> {
            writer.save("s1", PlaceForm(PlaceKind.ROOM, id = "nope", name = "Shed", homeId = garden))
        }
        assertEquals(ErrorCode.LAY_PLACE_MISSING, gone.code)
        val noHome = assertThrows<ValidationException> { writer.save("s1", PlaceForm(PlaceKind.ROOM, name = "Shed")) }
        assertEquals(ErrorCode.LAY_PLACE_MISSING, noHome.code)
        assertEquals(emptyList<Any>(), tables.rooms.value)
    }

    @Test
    fun `a blank name is rejected`() = runTest {
        val error = assertThrows<ValidationException> { writer.save("s1", PlaceForm(PlaceKind.HOME, name = "  ")) }
        assertEquals(ErrorCode.LAY_NAME_MISSING, error.code)
        assertEquals(emptyList<Any>(), tables.homes.value)
    }
}
