package com.stacknoise.haac.feature.entities.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.MonoFontFamily
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.Tile

/** Which sheet or dialog of the room grid is open. */
internal sealed interface RoomSheet {
    /** Long press on an active tile: *Rename*, *Remove from this room*. */
    data class Menu(val tile: Tile) : RoomSheet

    /** Tap on a withdrawn tile (concept 7.4). */
    data class Withdrawn(val tile: Tile) : RoomSheet

    /** *Review* in the banner: all withdrawn tiles of the room. */
    data class Review(val tiles: List<Tile>) : RoomSheet

    /** The rename dialog (M-07). */
    data class Rename(val tile: Tile) : RoomSheet
}

/** Callbacks of the sheets; each closes the sheet. */
internal class SheetActions(
    val onRename: (Tile) -> Unit,
    val onRemoveHere: (List<String>) -> Unit,
    val onRemoveEverywhere: (List<String>) -> Unit,
    val onDismiss: () -> Unit,
)

/** Shows [sheet] as a bottom sheet (menu, withdrawn tile, review). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoomSheetContent(sheet: RoomSheet, actions: SheetActions) {
    if (sheet is RoomSheet.Rename) return
    ModalBottomSheet(onDismissRequest = actions.onDismiss) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            when (sheet) {
                is RoomSheet.Menu -> {
                    Heading(sheet.tile.name, sheet.tile.entityId)
                    SheetButton(R.string.tile_rename) { actions.onRename(sheet.tile) }
                    SheetButton(R.string.tile_remove_here) { actions.onRemoveHere(listOf(sheet.tile.entityId)) }
                }
                is RoomSheet.Withdrawn -> {
                    Heading(stringResource(R.string.tile_removed_title), sheet.tile.entityId)
                    Explanation()
                    val ids = listOf(sheet.tile.entityId)
                    RemoveButtons(ids, R.string.tile_remove_here, R.string.tile_remove_everywhere, actions)
                }
                is RoomSheet.Review -> {
                    Heading(stringResource(R.string.review_title), null)
                    sheet.tiles.forEach { Text(it.name, modifier = Modifier.padding(vertical = 4.dp)) }
                    Explanation()
                    val ids = sheet.tiles.map { it.entityId }
                    RemoveButtons(ids, R.string.review_remove_here, R.string.review_remove_everywhere, actions)
                }
                is RoomSheet.Rename -> Unit
            }
        }
    }
}

/** Title of a sheet with an optional `entity_id` in the mono font. */
@Composable
private fun Heading(title: String, entityId: String?) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    entityId?.let {
        Text(
            it,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

/** Why a tile is inactive (concept 7.4). */
@Composable
private fun Explanation() {
    Text(
        stringResource(R.string.tile_removed_text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 16.dp),
    )
}

/** *Remove from this room* and *Remove from all rooms* for [ids]. */
@Composable
private fun RemoveButtons(ids: List<String>, @StringRes here: Int, @StringRes everywhere: Int, actions: SheetActions) {
    SheetButton(here) { actions.onRemoveHere(ids) }
    SheetButton(everywhere, color = MaterialTheme.colorScheme.error) { actions.onRemoveEverywhere(ids) }
}

/** Full-width outlined button of a sheet. */
@Composable
private fun SheetButton(@StringRes text: Int, color: Color = Color.Unspecified, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = HaacShapes.Medium,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Text(stringResource(text), color = color)
    }
}
