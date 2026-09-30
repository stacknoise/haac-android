package com.stacknoise.haac.feature.layout.domain

import com.stacknoise.haac.core.database.layout.Floor
import com.stacknoise.haac.core.database.layout.Home
import com.stacknoise.haac.core.database.layout.Places
import com.stacknoise.haac.core.database.layout.Room
import com.stacknoise.haac.feature.layout.data.parseAreas
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AreaImportTest {
    private val areas = HaAreas(
        floors = listOf(HaFloor("g", "Ground floor", 0), HaFloor("u", "Upstairs", 1)),
        areas = listOf(
            HaArea("kitchen", "Kitchen", "g", 3),
            HaArea("hall", "Hall", "g", 1),
            HaArea("bed", "Bedroom", "u", 2),
            HaArea("garden", "Garden", null, 1),
            HaArea("cellar", "Cellar", "gone", 1),
        ),
    )
    private val all = areas.areas.map { it.id }.toSet()

    @Test
    fun `areas group by floor and the rest has no level`() {
        assertEquals(listOf("Kitchen", "Hall"), areas.areasOn("g").map { it.name })
        assertEquals(listOf("Garden", "Cellar"), areas.areasOn(null).map { it.name })
    }

    @Test
    fun `a new home gets every selected area, levels with their rooms and the rest loose`() {
        val plan = areas.plan(all - "hall", Places(), homeId = null)

        assertEquals(
            listOf(
                PlannedFloor("Ground floor", 0, null, listOf("Kitchen")),
                PlannedFloor("Upstairs", 1, null, listOf("Bedroom")),
            ),
            plan.floors,
        )
        assertEquals(listOf("Garden", "Cellar"), plan.looseRooms)
        assertEquals(2, plan.newLevels)
        assertEquals(4, plan.newRooms)
        assertEquals(0, plan.skipped)
    }

    @Test
    fun `an unchecked level is not created and nothing selected is an empty plan`() {
        val plan = areas.plan(setOf("bed"), Places(), homeId = null)
        assertEquals(listOf("Upstairs"), plan.floors.map { it.name })
        assertTrue(areas.plan(emptySet(), Places(), homeId = null).isEmpty)
    }

    @Test
    fun `rooms that exist in the home are skipped and a level with the same name is reused`() {
        val places = Places(
            homes = listOf(Home("h", "Main")),
            floors = listOf(Floor("f1", "h", "ground FLOOR", 0)),
            rooms = listOf(Room("r1", "h", "f1", "kitchen"), Room("r2", "h", null, "Garden")),
        )

        val plan = areas.plan(all, places, homeId = "h")

        assertEquals(PlannedFloor("Ground floor", 0, "f1", listOf("Hall")), plan.floors.first())
        assertEquals(listOf("Cellar"), plan.looseRooms)
        assertEquals(2, plan.skipped)
        assertEquals(1, plan.newLevels)
    }

    @Test
    fun `the bridge answer is parsed, a floor without level keeps a null level`() {
        val json = Json.parseToJsonElement(
            """{"floors":[{"floor_id":"g","name":"Ground","level":null}],
               "areas":[{"area_id":"k","name":"Kitchen","floor_id":"g","entity_count":2},
                        {"area_id":"s","name":"Shed","floor_id":null,"entity_count":1}]}""",
        )

        val parsed = parseAreas(json)

        assertEquals(listOf(HaFloor("g", "Ground", null)), parsed.floors)
        assertEquals(listOf(HaArea("k", "Kitchen", "g", 2), HaArea("s", "Shed", null, 1)), parsed.areas)
    }
}
