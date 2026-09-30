package com.stacknoise.haac.feature.layout.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.feature.layout.R
import com.stacknoise.haac.feature.layout.domain.PlaceForm
import com.stacknoise.haac.feature.layout.domain.PlaceKind
import com.stacknoise.haac.core.database.layout.Places
import com.stacknoise.haac.core.database.layout.Room

/**
 * *Rooms on this level* (the rooms of the level's home) or *Rooms in this home* (the home's own rooms, fixed,
 * then the rooms of other homes, which move), each with its current link; below them the new rooms and
 * *New room on …* (M-03).
 */
@Composable
internal fun RoomChecklist(form: PlaceForm, places: Places, actions: RoomListActions) {
    val onLevel = form.kind == PlaceKind.FLOOR
    val candidates = if (onLevel) places.roomsOf(form.homeId) else places.rooms.filter { it.homeId != form.id }
    val own = if (onLevel) emptyList() else places.roomsOf(form.id)
    SectionLabel(if (onLevel) R.string.editor_rooms_on_level else R.string.editor_rooms_in_home)
    own.forEach { room -> CheckRow(room.name, ownLink(room, places), checked = true, enabled = false) {} }
    candidates.forEach { room ->
        CheckRow(room.name, currentLink(room, form, places), room.id in form.roomIds) { actions.onToggle(room.id) }
    }
    form.newRooms.forEachIndexed { index, name ->
        CheckRow(name, stringResource(R.string.editor_room_new), checked = true) { actions.onRemoveNew(index) }
    }
    val target = form.name.trim().ifEmpty {
        stringResource(if (onLevel) R.string.editor_this_level else R.string.editor_this_home)
    }
    NewRoomInput(
        label = stringResource(if (onLevel) R.string.editor_new_room_on else R.string.editor_new_room_in, target),
        onAdd = actions.onAdd,
    )
}

/** Where a room is now: "directly in Main house", "moves from First floor", or nothing if it stays. */
@Composable
private fun currentLink(room: Room, form: PlaceForm, places: Places): String? {
    val home = places.home(room.homeId)?.name.orEmpty()
    return when {
        form.kind == PlaceKind.HOME -> stringResource(R.string.editor_room_moves_from, home)
        room.floorId != null && room.floorId == form.id -> null
        room.floorId == null -> stringResource(R.string.editor_room_directly_in, home)
        else -> stringResource(R.string.editor_room_moves_from, places.floor(room.floorId)?.name.orEmpty())
    }
}

/** Where a room of the edited home is: on its level, or directly in the home. */
@Composable
private fun ownLink(room: Room, places: Places): String {
    val floor = places.floor(room.floorId)
    return if (floor == null) {
        stringResource(R.string.editor_room_directly_in, places.home(room.homeId)?.name.orEmpty())
    } else {
        stringResource(R.string.editor_room_on, floor.name)
    }
}

/** Checkbox, name and hint of one room; a room that cannot be changed is [enabled] = false. */
@Composable
private fun CheckRow(name: String, hint: String?, checked: Boolean, enabled: Boolean = true, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onToggle),
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() }, enabled = enabled)
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        hint?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** *+ New room on …*; opens a name field whose rooms the form creates on saving. */
@Composable
private fun NewRoomInput(label: String, onAdd: (String) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    if (!open) {
        TextButton(onClick = { open = true }) {
            Icon(painterResource(R.drawable.ic_layout_add), contentDescription = null)
            Text(label, modifier = Modifier.padding(start = 8.dp))
        }
        return
    }
    val add = {
        onAdd(name)
        name = ""
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text(stringResource(R.string.editor_new_room_hint)) },
            singleLine = true,
            shape = HaacShapes.Small,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { add() }),
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = add, enabled = name.isNotBlank()) { Text(stringResource(R.string.editor_add)) }
    }
}
