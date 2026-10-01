package com.stacknoise.haac.feature.entities.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.ErrorMessage
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.haacButtonColors
import com.stacknoise.haac.core.common.ui.theme.haacSegmentedColors
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.Tile

/**
 * Edit layout of a room (M-06, M-07, concept 7.2): *Close* discards (after asking when something changed), *Done*
 * saves order, sizes and removals. *Grid* shows the room as it will look, *List* is easier for long rooms.
 * [onAddEntities] opens *Add entities* for this room.
 */
@Composable
fun EditLayoutScreen(
    onClose: () -> Unit,
    onAddEntities: (String) -> Unit,
    viewModel: EditLayoutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var asking by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    val close = { if (state.draft?.changed == true) asking = true else onClose() }
    LaunchedEffect(state.done) { if (state.done) onClose() }
    BackHandler(onBack = close)
    val draft = state.draft
    val actions = EditActions(
        onMove = viewModel::onMove,
        onResize = viewModel::onResize,
        onRemove = viewModel::onRemove,
        onRename = { renaming = it.entityId },
        onAdd = { onAddEntities(viewModel.roomId) },
    )
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(state, onClose = close, onDone = viewModel::onDone)
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            ModeRow(state.mode, viewModel::onMode)
            state.error?.let { ErrorMessage(it) }
            val hint = if (state.mode == ArrangeMode.GRID) R.string.edit_hint_grid else R.string.edit_hint_list
            Text(
                stringResource(hint),
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryColor(),
                modifier = Modifier.padding(vertical = 12.dp),
            )
            if (draft != null && draft.tiles.isEmpty() && state.mode == ArrangeMode.LIST) {
                Text(stringResource(R.string.edit_empty), style = MaterialTheme.typography.bodyLarge)
            }
            draft?.let {
                if (state.mode == ArrangeMode.GRID) EditGrid(it.tiles, actions) else ArrangeList(it.tiles, actions)
            }
        }
    }
    val renamed = draft?.tiles?.firstOrNull { it.entityId == renaming }
    renamed?.let { tile -> Rename(tile, viewModel) { renaming = null } }
    if (asking) DiscardDialog(onDiscard = onClose, onKeep = { asking = false })
}

/** *Close*, "Edit Living room" and *Done*. */
@Composable
private fun TopBar(state: EditLayoutUiState, onClose: () -> Unit, onDone: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        IconButton(onClick = onClose) {
            Icon(painterResource(R.drawable.ic_entities_close), stringResource(R.string.edit_close))
        }
        Text(
            stringResource(R.string.edit_title, state.roomName),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        Button(
            onClick = onDone,
            enabled = !state.saving && state.draft != null,
            shape = HaacShapes.Full,
            colors = haacButtonColors(),
            contentPadding = PaddingValues(horizontal = 18.dp),
            modifier = Modifier.height(40.dp).padding(end = 12.dp),
        ) { Text(stringResource(R.string.edit_done)) }
    }
}

/** *Grid* or *List* (M-07). */
@Composable
private fun ModeRow(mode: ArrangeMode, onMode: (ArrangeMode) -> Unit) {
    val labels = listOf(R.string.edit_grid, R.string.edit_list)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().height(44.dp)) {
        ArrangeMode.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                selected = entry == mode,
                onClick = { onMode(entry) },
                shape = SegmentedButtonDefaults.itemShape(index, ArrangeMode.entries.size, HaacShapes.Small),
                colors = haacSegmentedColors(),
            ) { Text(stringResource(labels[index])) }
        }
    }
}

/** The rename dialog of M-07 for [tile]; saved at once. */
@Composable
private fun Rename(tile: Tile, viewModel: EditLayoutViewModel, onDismiss: () -> Unit) {
    RenameDialog(
        tile = tile,
        onSave = { alias ->
            viewModel.onRename(tile.entityId, alias)
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

/** *Discard changes?* when closing with unsaved changes. */
@Composable
private fun DiscardDialog(onDiscard: () -> Unit, onKeep: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeep,
        title = { Text(stringResource(R.string.edit_discard_title)) },
        text = { Text(stringResource(R.string.edit_discard_text)) },
        confirmButton = { TextButton(onClick = onDiscard) { Text(stringResource(R.string.edit_discard)) } },
        dismissButton = { TextButton(onClick = onKeep) { Text(stringResource(R.string.edit_keep)) } },
    )
}
