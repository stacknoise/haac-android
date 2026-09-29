package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.assignment.TileSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GridPackingTest {
    @Test
    fun `a tall climate tile sits left, small tiles fill the gaps (M-05)`() {
        val sizes = listOf(TileSize.LARGE) + List(6) { TileSize.SMALL }
        val expected = listOf(
            GridCell(0, 0),
            GridCell(1, 0),
            GridCell(1, 1),
            GridCell(0, 2),
            GridCell(1, 2),
            GridCell(0, 3),
            GridCell(1, 3),
        )
        assertEquals(expected, packGrid(sizes))
    }

    @Test
    fun `a wide tile moves to the next free row, later small tiles fill the gap before it`() {
        val sizes = listOf(TileSize.SMALL, TileSize.WIDE, TileSize.SMALL, TileSize.LARGE)
        assertEquals(listOf(GridCell(0, 0), GridCell(0, 1), GridCell(1, 0), GridCell(0, 2)), packGrid(sizes))
    }
}
