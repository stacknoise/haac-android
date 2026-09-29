package com.stacknoise.haac.feature.layout.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlaceRowsTest {
    private val places = Places(
        homes = listOf(Home("h1", "Main house"), Home("h2", "Garden house")),
        floors = listOf(Floor("f0", "h1", "Ground floor", 0), Floor("f1", "h1", "First floor", 1)),
        rooms = listOf(
            Room("r1", "h1", null, "Hall"),
            Room("r2", "h1", "f1", "Office"),
            Room("r3", "h2", null, "Shed"),
            Room("r4", "h1", "f0", "Kitchen"),
        ),
    )

    @Test
    fun `homes, then levels, then rooms grouped by home and level`() {
        val rows = places.rows(PlaceFilter.ALL)
        assertEquals(
            listOf("Main house", "Garden house", "Ground floor", "First floor", "Kitchen", "Office", "Hall", "Shed"),
            rows.map { it.name },
        )
        assertEquals(Relation.Levels(2), rows[0].relation)
        assertEquals(Relation.Rooms(1), rows[1].relation)
        assertEquals(Relation.In("Main house"), rows[2].relation)
        assertEquals(Relation.In("Ground floor"), rows[4].relation)
        assertEquals(Relation.In("Main house"), rows[6].relation)
        assertEquals(Relation.In("Garden house"), rows[7].relation)
    }

    @Test
    fun `filters show one kind and count it`() {
        assertEquals(listOf(PlaceKind.FLOOR, PlaceKind.FLOOR), places.rows(PlaceFilter.LEVELS).map { it.kind })
        assertEquals(listOf(8, 2, 2, 4), PlaceFilter.entries.map { places.count(it) })
        assertEquals(listOf("r4", "r2", "r1", "r3"), places.rows(PlaceFilter.ROOMS).map { it.id })
    }
}
