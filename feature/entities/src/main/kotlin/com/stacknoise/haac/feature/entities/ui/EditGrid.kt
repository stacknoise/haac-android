package com.stacknoise.haac.feature.entities.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.database.assignment.TileSize
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.Tile
import kotlin.math.roundToInt

/**
 * The grid preview of the edit layout (M-06): tiles as in the room, each with handle, remove badge, pencil and
 * size. Hold a tile and drag it: it lifts and follows the finger, its old place shows hatched, and the others
 * make room as soon as its centre is over another tile. The *Add entities* tile ends the grid.
 */
@Composable
internal fun EditGrid(tiles: List<Tile>, actions: EditActions) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val grid = with(density) {
            GridGeometry(constraints.maxWidth, GridGap.roundToPx(), GridRowHeight.roundToPx())
        }
        val labels = moveLabels()
        val accent = MaterialTheme.colorScheme.primary
        val rects = grid.rects(tiles.map { it.size } + TileSize.SMALL)
        val current by rememberUpdatedState(tiles)
        val places by rememberUpdatedState(rects)
        var dragged by remember { mutableStateOf<String?>(null) }
        var position by remember { mutableStateOf(Offset.Zero) }
        Box(modifier = Modifier.height(with(density) { grid.height(rects).toDp() })) {
            tiles.forEachIndexed { index, tile ->
                key(tile.entityId) {
                    val rect = rects[index]
                    val lifted = tile.entityId == dragged
                    if (lifted) Box(Modifier.at(rect, density).hatched(accent))
                    EditTile(
                        tile,
                        actions,
                        Modifier
                            .at(rect, density, if (lifted) position else null)
                            .zIndex(if (lifted) 1f else 0f)
                            .moveActions(index, tiles.size, labels, actions.onMove)
                            .pointerInput(tile.entityId) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        dragged = tile.entityId
                                        val start = places[current.indexOfFirst { it.entityId == tile.entityId }]
                                        position = Offset(start.left.toFloat(), start.top.toFloat())
                                    },
                                    onDragEnd = { dragged = null },
                                    onDragCancel = { dragged = null },
                                ) { change, amount ->
                                    change.consume()
                                    position += amount
                                    val from = current.indexOfFirst { it.entityId == tile.entityId }
                                    val to = dropIndex(places.take(current.size), places[from], position)
                                    if (to != null && to != from) actions.onMove(from, to)
                                }
                            },
                        lifted = lifted,
                    )
                }
            }
            AddTile(actions.onAdd, Modifier.at(rects.last(), density))
        }
    }
}

/**
 * The index of the tile whose place contains the centre of the dragged tile (of [dragged]'s size) at
 * [position], or null over no tile.
 */
private fun dropIndex(places: List<IntRect>, dragged: IntRect, position: Offset): Int? {
    val centre = IntOffset(
        (position.x + dragged.width / 2f).roundToInt(),
        (position.y + dragged.height / 2f).roundToInt(),
    )
    return places.indexOfFirst { it.contains(centre) }.takeIf { it >= 0 }
}

/** Places a child at [rect]; while dragged at [dragged] instead (px). */
private fun Modifier.at(rect: IntRect, density: Density, dragged: Offset? = null): Modifier {
    val size = with(density) { Modifier.size(rect.width.toDp(), rect.height.toDp()) }
    return offset { dragged?.let { IntOffset(it.x.roundToInt(), it.y.roundToInt()) } ?: rect.topLeft }.then(size)
}

/** A tile of the edit grid: handle, remove badge, name with pencil and size label; [lifted] while dragged. */
@Composable
private fun EditTile(tile: Tile, actions: EditActions, modifier: Modifier, lifted: Boolean) {
    val colors = MaterialTheme.colorScheme
    val frame = if (lifted) {
        Modifier.shadow(8.dp, HaacShapes.Medium).background(colors.primaryContainer, HaacShapes.Medium)
            .border(1.dp, colors.primary, HaacShapes.Medium)
    } else {
        Modifier.background(colors.surface, HaacShapes.Medium).border(1.dp, colors.outline, HaacShapes.Medium)
    }
    Box(modifier = modifier.then(frame).clip(HaacShapes.Medium)) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DragHandle()
                Spacer(Modifier.weight(1f))
                RemoveButton(tile, actions.onRemove)
            }
            Spacer(Modifier.weight(1f))
            EditName(tile, actions.onRename)
            SizeMenu(tile, actions.onResize)
        }
    }
}

/** The dashed *Add entities* tile at the end of the grid (M-06). */
@Composable
private fun AddTile(onAdd: () -> Unit, modifier: Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(HaacShapes.Medium)
            .border(1.dp, MaterialTheme.colorScheme.outline, HaacShapes.Medium)
            .clickable(onClick = onAdd)
            .padding(12.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Icon(painterResource(R.drawable.ic_entities_add), contentDescription = null)
        Text(
            stringResource(R.string.rooms_add_entities),
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryColor(),
        )
        Spacer(Modifier.weight(1f))
    }
}
