package com.stacknoise.haac.feature.entities.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.feature.entities.domain.Tile
import kotlin.math.roundToInt

/**
 * The list arrange mode (M-07): one row per tile with handle, name and pencil, size and remove button. Drag the
 * handle to move a row; it follows the finger and the others make room.
 */
@Composable
internal fun ArrangeList(tiles: List<Tile>, actions: EditActions) {
    val rowHeight = with(LocalDensity.current) { RowHeight.toPx() }
    val labels = moveLabels()
    val current by rememberUpdatedState(tiles)
    var dragged by remember { mutableStateOf<String?>(null) }
    var top by remember { mutableFloatStateOf(0f) }
    Box(modifier = Modifier.fillMaxWidth().height(RowHeight * tiles.size)) {
        tiles.forEachIndexed { index, tile ->
            key(tile.entityId) {
                val lifted = tile.entityId == dragged
                val y = if (lifted) top else index * rowHeight
                ArrangeRow(
                    tile,
                    actions,
                    lifted,
                    onHandleDrag = {
                        detectDragGestures(
                            onDragStart = {
                                dragged = tile.entityId
                                top = current.indexOfFirst { it.entityId == tile.entityId } * rowHeight
                            },
                            onDragEnd = { dragged = null },
                            onDragCancel = { dragged = null },
                        ) { change, amount ->
                            change.consume()
                            top += amount.y
                            val from = current.indexOfFirst { it.entityId == tile.entityId }
                            val to = ((top + rowHeight / 2) / rowHeight).toInt().coerceIn(0, current.lastIndex)
                            if (to != from) actions.onMove(from, to)
                        }
                    },
                    modifier = Modifier
                        .offset { IntOffset(0, y.roundToInt()) }
                        .zIndex(if (lifted) 1f else 0f)
                        .moveActions(index, tiles.size, labels, actions.onMove),
                )
            }
        }
    }
}

/** One row of the list: handle, name with pencil, size and remove; [lifted] while dragged. */
@Composable
private fun ArrangeRow(
    tile: Tile,
    actions: EditActions,
    lifted: Boolean,
    onHandleDrag: suspend PointerInputScope.() -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val frame = if (lifted) {
        Modifier.shadow(8.dp, HaacShapes.Medium).background(colors.surface, HaacShapes.Medium)
            .border(1.dp, colors.primary, HaacShapes.Medium)
    } else {
        Modifier
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().height(RowHeight).then(frame).padding(horizontal = 4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(48.dp).pointerInput(tile.entityId, block = onHandleDrag),
        ) { DragHandle() }
        EditName(tile, actions.onRename, Modifier.weight(1f))
        SizeMenu(tile, actions.onResize)
        RemoveButton(tile, actions.onRemove, Modifier.padding(start = 4.dp))
    }
}

/** Height of one row (touch target of the handle is 48 dp). */
private val RowHeight = 56.dp
