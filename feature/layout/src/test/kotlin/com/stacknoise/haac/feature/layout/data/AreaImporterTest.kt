package com.stacknoise.haac.feature.layout.data

import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ValidationException
import com.stacknoise.haac.feature.layout.domain.ImportPlan
import com.stacknoise.haac.feature.layout.domain.ImportResult
import com.stacknoise.haac.feature.layout.domain.PlaceForm
import com.stacknoise.haac.feature.layout.domain.PlaceKind
import com.stacknoise.haac.feature.layout.domain.PlannedFloor
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AreaImporterTest {
    private val tables = FakeLayoutTables()
    private var nextId = 0
    private val importer = AreaImporter(tables.daos, tables.transactions, DefaultErrorFactory()) { "id${++nextId}" }
    private val writer = PlaceWriter(tables.daos, tables.transactions, DefaultErrorFactory()) { "w${++nextId}" }

    private val plan = ImportPlan(
        floors = listOf(
            PlannedFloor("Ground floor", 0, null, listOf("Kitchen", "Hall")),
            PlannedFloor("Upstairs", 1, null, listOf("Bedroom")),
        ),
        looseRooms = listOf("Garden"),
    )

    @Test
    fun `a new home receives levels and rooms in order`() = runTest {
        val result = importer.import("s1", ImportTarget(null, " Main "), plan)

        assertEquals(ImportResult(2, 4), result)
        assertEquals(listOf("Main"), tables.homes.value.map { it.name })
        assertEquals(listOf("Ground floor", "Upstairs"), tables.floors.value.map { it.name })
        assertEquals(listOf(0, 1), tables.floors.value.map { it.level })
        assertEquals(listOf("Kitchen", "Hall", "Bedroom", "Garden"), tables.rooms.value.map { it.name })
        val (ground, upstairs) = tables.floors.value.map { it.id }
        assertEquals(listOf(ground, ground, upstairs, null), tables.rooms.value.map { it.floorId })
        assertEquals(listOf(1024, 2048, 3072, 4096), tables.rooms.value.map { it.sortOrder })
    }

    @Test
    fun `an existing home keeps its rooms and a reused level gets the new rooms`() = runTest {
        val home = writer.save("s1", PlaceForm(PlaceKind.HOME, name = "Main", newRooms = listOf("Office")))
        val ground = writer.save("s1", PlaceForm(PlaceKind.FLOOR, name = "Ground floor", homeId = home))

        val reuse = ImportPlan(floors = listOf(PlannedFloor("Ground floor", 0, ground, listOf("Kitchen"))))
        val result = importer.import("s1", ImportTarget(home), reuse)

        assertEquals(ImportResult(0, 1), result)
        assertEquals(listOf("Ground floor"), tables.floors.value.map { it.name })
        assertEquals(listOf("Office", "Kitchen"), tables.rooms.value.map { it.name })
        assertEquals(ground, tables.rooms.value.last().floorId)
        assertEquals(listOf(1024, 2048), tables.rooms.value.map { it.sortOrder })
    }

    @Test
    fun `a missing home or a blank new name is rejected and nothing is written`() = runTest {
        val missing = assertThrows<ValidationException> { importer.import("s1", ImportTarget("nope"), plan) }
        assertEquals(ErrorCode.LAY_PLACE_MISSING, missing.code)
        val blank = assertThrows<ValidationException> { importer.import("s1", ImportTarget(null, " "), plan) }
        assertEquals(ErrorCode.LAY_NAME_MISSING, blank.code)
        assertEquals(0, tables.homes.value.size + tables.floors.value.size + tables.rooms.value.size)
    }
}
