package com.stacknoise.haac.feature.entities.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.database.assignment.TileSize
import com.stacknoise.haac.feature.entities.domain.GridColumns
import com.stacknoise.haac.feature.entities.domain.Tile
import com.stacknoise.haac.feature.entities.domain.packGrid

/**
 * The two-column room grid (concept 7.2, M-05): tiles in their order, placed densely by [packGrid]; a 1×1 tile
 * is one column wide and one row high. Controls work only while [enabled].
 */
@Composable
internal fun RoomGrid(tiles: List<Tile>, enabled: Boolean, actions: TileActions, modifier: Modifier = Modifier) {
    val sizes = remember(tiles) { tiles.map { it.size } }
    Layout(
        content = { tiles.forEach { tile -> TileCard(tile, enabled, actions) } },
        modifier = modifier,
    ) { measurables, constraints ->
        val grid = GridGeometry(constraints.maxWidth, GridGap.roundToPx(), GridRowHeight.roundToPx())
        val rects = grid.rects(sizes)
        val placeables = measurables.mapIndexed { index, measurable ->
            measurable.measure(Constraints.fixed(rects[index].width, rects[index].height))
        }
        layout(constraints.maxWidth, grid.height(rects)) {
            placeables.forEachIndexed { index, placeable -> placeable.place(rects[index].left, rects[index].top) }
        }
    }
}

/** Pixel geometry of the room grid of [width] with [gap] between tiles and rows of [rowHeight]. */
internal class GridGeometry(private val width: Int, private val gap: Int, private val rowHeight: Int) {
    /** Width of one column. */
    private val columnWidth: Int get() = (width - gap * (GridColumns - 1)) / GridColumns

    /** The rectangle of every tile of [sizes], packed densely in their order (concept 7.2). */
    fun rects(sizes: List<TileSize>): List<IntRect> = packGrid(sizes).zip(sizes) { cell, size ->
        val left = cell.column * (columnWidth + gap)
        val top = cell.row * (rowHeight + gap)
        IntRect(
            left,
            top,
            left + columnWidth * size.columns + gap * (size.columns - 1),
            top + rowHeight * size.rows + gap * (size.rows - 1),
        )
    }

    /** Height of the grid holding [rects]. */
    fun height(rects: List<IntRect>): Int = rects.maxOfOrNull { it.bottom } ?: 0
}

/** Gap between tiles and height of one grid row (M-05). */
internal val GridGap = 14.dp
internal val GridRowHeight = 168.dp
