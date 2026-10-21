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

    @Test
    fun `more columns take more small tiles per row, a wide tile still spans two`() {
        val sizes = listOf(TileSize.SMALL, TileSize.SMALL, TileSize.WIDE, TileSize.SMALL, TileSize.SMALL)
        val expected = listOf(GridCell(0, 0), GridCell(1, 0), GridCell(2, 0), GridCell(0, 1), GridCell(1, 1))
        assertEquals(expected, packGrid(sizes, columns = 4))
    }

    @Test
    fun `a tall tile of a four column grid leaves its neighbours free`() {
        val sizes = listOf(TileSize.LARGE, TileSize.SMALL, TileSize.SMALL, TileSize.SMALL, TileSize.SMALL)
        val expected = listOf(GridCell(0, 0), GridCell(1, 0), GridCell(2, 0), GridCell(3, 0), GridCell(1, 1))
        assertEquals(expected, packGrid(sizes, columns = 4))
    }

    @Test
    fun `the column count follows the width and never drops below two`() {
        assertEquals(2, gridColumns(371))
        assertEquals(2, gridColumns(200))
        assertEquals(3, gridColumns(508))
        assertEquals(4, gridColumns(680))
        assertEquals(5, gridColumns(880))
        assertEquals(6, gridColumns(1144))
    }
}
