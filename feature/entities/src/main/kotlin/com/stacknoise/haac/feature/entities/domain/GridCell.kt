package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.assignment.TileSize

/** Position of a tile in the room grid: column and row, in cells. */
data class GridCell(val column: Int, val row: Int)

/** Fewest columns of the room grid; phones have exactly this many (concept 7.2). */
const val MinGridColumns = 2

/** Narrowest a column may get in dp, and the gap between tiles in dp (concept 15.7). */
private const val MinColumnWidthDp = 152
private const val GridGapDp = 14

/**
 * Number of columns of a room grid that is [widthDp] wide: as many as fit, but at least [MinGridColumns]
 * (concept 15.7). The layout stores no columns, so the number may differ per device.
 */
fun gridColumns(widthDp: Int): Int =
    maxOf(MinGridColumns, (widthDp + GridGapDp) / (MinColumnWidthDp + GridGapDp))

/**
 * Places tiles of [sizes] in their order into the grid of [columns] columns, filling gaps densely (concept 7.2):
 * each tile takes the first free spot, row by row, where it fits.
 */
fun packGrid(sizes: List<TileSize>, columns: Int = MinGridColumns): List<GridCell> {
    val taken = mutableSetOf<GridCell>()
    return sizes.map { size ->
        val cell = generateSequence(0) { it + 1 }
            .flatMap { row -> (0..columns - size.columns).asSequence().map { GridCell(it, row) } }
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
