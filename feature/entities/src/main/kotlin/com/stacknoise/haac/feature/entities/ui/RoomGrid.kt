package com.stacknoise.haac.feature.entities.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.feature.entities.domain.GridColumns
import com.stacknoise.haac.feature.entities.domain.Tile
import com.stacknoise.haac.feature.entities.domain.packGrid

/**
 * The two-column room grid (concept 7.2, M-05): tiles in their order, placed densely by [packGrid]; a 1×1 tile
 * is one column wide and one row high. Controls work only while [enabled].
 */
@Composable
internal fun RoomGrid(tiles: List<Tile>, enabled: Boolean, actions: TileActions, modifier: Modifier = Modifier) {
    val cells = remember(tiles) { packGrid(tiles.map { it.size }) }
    Layout(
        content = { tiles.forEach { tile -> TileCard(tile, enabled, actions) } },
        modifier = modifier,
    ) { measurables, constraints ->
        val gap = 12.dp.roundToPx()
        val rowHeight = 112.dp.roundToPx()
        val columnWidth = (constraints.maxWidth - gap * (GridColumns - 1)) / GridColumns
        val placeables = measurables.mapIndexed { index, measurable ->
            val size = tiles[index].size
            val width = columnWidth * size.columns + gap * (size.columns - 1)
            val height = rowHeight * size.rows + gap * (size.rows - 1)
            measurable.measure(Constraints.fixed(width, height))
        }
        val rows = cells.zip(tiles).maxOfOrNull { (cell, tile) -> cell.row + tile.size.rows } ?: 0
        val height = if (rows == 0) 0 else rows * rowHeight + (rows - 1) * gap
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable ->
                placeable.place(cells[index].column * (columnWidth + gap), cells[index].row * (rowHeight + gap))
            }
        }
    }
}
