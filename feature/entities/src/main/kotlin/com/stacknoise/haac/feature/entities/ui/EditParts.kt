package com.stacknoise.haac.feature.entities.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.database.assignment.TileSize
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.Tile

/** Callbacks of the edit layout (M-06, M-07). */
internal class EditActions(
    /** The tile at the first index moves to the second. */
    val onMove: (Int, Int) -> Unit,
    /** A new size for a tile. */
    val onResize: (String, TileSize) -> Unit,
    /** The remove badge (−). */
    val onRemove: (String) -> Unit,
    /** Tap on a name or the pencil. */
    val onRename: (Tile) -> Unit,
    /** The *Add entities* tile. */
    val onAdd: () -> Unit,
)

/** The name with a pencil; a tap opens *Rename* (M-06, M-07). */
@Composable
internal fun EditName(tile: Tile, onRename: (Tile) -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clip(HaacShapes.Small).clickable { onRename(tile) }.padding(vertical = 4.dp),
    ) {
        Text(
            tile.name,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Icon(
            painterResource(R.drawable.ic_entities_edit),
            contentDescription = stringResource(R.string.detail_rename),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp).size(16.dp),
        )
    }
}

/** The size label ("2×2"); a tap opens the menu with 1×1, 2×1 and 2×2 (concept 7.2). */
@Composable
internal fun SizeMenu(tile: Tile, onResize: (String, TileSize) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Text(
            tile.size.label,
            style = MaterialTheme.typography.labelLarge,
            color = HaacColors.OnSurfaceVariantStrong,
            modifier = Modifier
                .clip(HaacShapes.Small)
                .clickable { open = true }
                .border(1.5.dp, MaterialTheme.colorScheme.outline, HaacShapes.Small)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            TileSize.entries.forEach { size ->
                DropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.edit_size_option, size.label, stringResource(sizeName(size))))
                    },
                    onClick = {
                        open = false
                        onResize(tile.entityId, size)
                    },
                )
            }
        }
    }
}

/** The remove badge (−) of a tile or row. */
@Composable
internal fun RemoveButton(tile: Tile, onRemove: (String) -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = { onRemove(tile.entityId) }, modifier = modifier.size(36.dp)) {
        Icon(
            painterResource(R.drawable.ic_entities_remove_circle),
            contentDescription = stringResource(R.string.edit_remove, tile.name),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The drag handle (dots). */
@Composable
internal fun DragHandle(modifier: Modifier = Modifier) {
    Icon(
        painterResource(R.drawable.ic_entities_drag),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(24.dp),
    )
}

/** The labels of [moveActions]. */
internal class MoveLabels(val earlier: String, val later: String)

/** The labels of [moveActions] in the current language. */
@Composable
internal fun moveLabels(): MoveLabels =
    MoveLabels(stringResource(R.string.edit_move_earlier), stringResource(R.string.edit_move_later))

/**
 * *Move earlier* and *Move later* for screen readers and switch access, since dragging needs a pointer; the tile
 * is at [index] of [count].
 */
internal fun Modifier.moveActions(index: Int, count: Int, labels: MoveLabels, onMove: (Int, Int) -> Unit): Modifier {
    val earlier = labels.earlier
    val later = labels.later
    return semantics {
        customActions = listOfNotNull(
            CustomAccessibilityAction(earlier) {
                onMove(index, index - 1)
                true
            }.takeIf { index > 0 },
            CustomAccessibilityAction(later) {
                onMove(index, index + 1)
                true
            }.takeIf { index < count - 1 },
        )
    }
}

/** The drop target: diagonal stripes in the accent [color] (M-06). */
internal fun Modifier.hatched(color: Color): Modifier =
    border(2.dp, color, HaacShapes.Tile).clip(HaacShapes.Tile).drawBehind {
        val step = 14.dp.toPx()
        var x = -size.height
        while (x < size.width) {
            val stripe = color.copy(alpha = HatchAlpha)
            drawLine(stripe, Offset(x, size.height), Offset(x + size.height, 0f), 6.dp.toPx())
            x += step
        }
    }

/** The word for a tile size, e.g. "wide" (screen readers, menu). */
@StringRes
internal fun sizeName(size: TileSize): Int = when (size) {
    TileSize.SMALL -> R.string.edit_size_small
    TileSize.WIDE -> R.string.edit_size_wide
    TileSize.LARGE -> R.string.edit_size_large
}

/** Opacity of the drop target's stripes. */
private const val HatchAlpha = 0.25f
