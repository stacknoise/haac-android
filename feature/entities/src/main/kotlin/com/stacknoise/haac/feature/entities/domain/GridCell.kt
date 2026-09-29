package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.assignment.TileSize

/** Position of a tile in the room grid: column (0 or 1) and row, in cells. */
data class GridCell(val column: Int, val row: Int)

/** Number of columns of the room grid (concept 7.2). */
const val GridColumns = 2

/**
 * Places tiles of [sizes] in their order into the two-column grid, filling gaps densely (concept 7.2): each
 * tile takes the first free spot, row by row, where it fits.
 */
fun packGrid(sizes: List<TileSize>): List<GridCell> {
    val taken = mutableSetOf<GridCell>()
    return sizes.map { size ->
        val cell = generateSequence(0) { it + 1 }
            .flatMap { row -> (0..GridColumns - size.columns).asSequence().map { GridCell(it, row) } }
            .first { start -> cells(start, size).none(taken::contains) }
        taken += cells(cell, size)
        cell
    }
}

/** The cells a tile of [size] covers when it starts at [start]. */
private fun cells(start: GridCell, size: TileSize): List<GridCell> =
    (0 until size.rows).flatMap { dy ->
        (0 until size.columns).map { dx -> GridCell(start.column + dx, start.row + dy) }
    }
