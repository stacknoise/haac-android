package com.stacknoise.haac.feature.layout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.ErrorMessage
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.HaacTheme
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.common.ui.theme.haacButtonColors
import com.stacknoise.haac.core.database.layout.Floor
import com.stacknoise.haac.core.database.layout.Home
import com.stacknoise.haac.core.database.layout.Places
import com.stacknoise.haac.core.database.layout.Room
import com.stacknoise.haac.feature.layout.R
import com.stacknoise.haac.feature.layout.domain.PlaceForm
import com.stacknoise.haac.feature.layout.domain.PlaceKind

/**
 * Form for a new or existing home, level or room (M-03, concept 6.1, 6.2); [onClose] after saving, deleting or
 * closing. Deleting a home or a level with rooms asks first.
 */
@Composable
fun PlaceEditorScreen(onClose: () -> Unit, viewModel: PlaceEditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.status.done) { if (state.status.done) onClose() }
    val form = state.form ?: return
    PlaceEditorContent(
        state = state,
        actions = EditorActions(
            onClose = onClose,
            onChange = viewModel::onChange,
            onSave = viewModel::onSave,
            onDelete = {
                val hasRooms = form.id?.let { state.places.roomsOn(it).isNotEmpty() } == true
                val asks = form.kind == PlaceKind.HOME || (form.kind == PlaceKind.FLOOR && hasRooms)
                if (asks) confirmDelete = true else viewModel.onDelete()
            },
        ),
    )
    if (confirmDelete) {
        DeleteDialog(
            form = form,
            places = state.places,
            onConfirm = { withRooms ->
                confirmDelete = false
                viewModel.onDelete(withRooms)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Stateless layout: header, name, the sections of the kind, summary and button. */
@Composable
fun PlaceEditorContent(state: PlaceEditorUiState, actions: EditorActions) {
    val form = state.form ?: return
    val places = state.places
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .padding(horizontal = 20.dp),
    ) {
        Header(form, actions)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            NameField(form.name) { name -> actions.onChange { f, _ -> f.copy(name = name) } }
            IconPicker(form.icon) { icon -> actions.onChange { f, _ -> f.copy(icon = icon) } }
            if (form.kind != PlaceKind.HOME) {
                HomeChips(form, places) { id -> actions.onChange { f, p -> f.withHome(id, p) } }
            }
            when (form.kind) {
                PlaceKind.ROOM -> FloorChips(form, places) { id -> actions.onChange { f, _ -> f.copy(floorId = id) } }
                PlaceKind.FLOOR -> LevelStepper(form.level) { n -> actions.onChange { f, _ -> f.copy(level = n) } }
                PlaceKind.HOME -> Unit
            }
            if (form.kind != PlaceKind.ROOM && (form.kind == PlaceKind.HOME || form.homeId != null)) {
                RoomChecklist(form, places, roomListActions(actions))
            }
            state.status.error?.let { ErrorMessage(it) }
        }
        SaveBar(form, places, state.status, actions.onSave)
    }
}

/** Close button, title and, for an existing place, the delete button. */
@Composable
private fun Header(form: PlaceForm, actions: EditorActions) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        IconButton(onClick = actions.onClose) {
            Icon(painterResource(R.drawable.ic_layout_close), stringResource(R.string.editor_close))
        }
        Text(
            stringResource(editorTitle(form)).uppercase(),
            style = SectionLabelStyle.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize),
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        if (!form.isNew) {
            IconButton(onClick = actions.onDelete) {
                Icon(painterResource(R.drawable.ic_layout_delete), stringResource(R.string.editor_delete))
            }
        }
    }
}

/** Large name field with an accent underline (M-03). */
@Composable
private fun NameField(name: String, onChange: (String) -> Unit) {
    TextField(
        value = name,
        onValueChange = onChange,
        placeholder = {
            Text(
                stringResource(R.string.editor_name_hint),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.outline,
            )
        },
        textStyle = MaterialTheme.typography.headlineLarge,
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.outline,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

/** Summary line for a new place and the primary *Create …* or *Save* button. */
@Composable
private fun SaveBar(form: PlaceForm, places: Places, status: EditorStatus, onSave: () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        if (form.isNew && form.canSave) {
            Text(
                summary(form, places),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
        Button(
            onClick = onSave,
            enabled = form.canSave && !status.busy,
            shape = HaacShapes.Button,
            colors = haacButtonColors(),
            modifier = Modifier.fillMaxWidth().height(58.dp),
        ) {
            if (status.busy) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp))
            } else {
                Text(stringResource(saveLabel(form)))
            }
        }
    }
}

/** The room list callbacks, mapped to form changes. */
private fun roomListActions(actions: EditorActions) = RoomListActions(
    onToggle = { id -> actions.onChange { f, _ -> f.toggleRoom(id) } },
    onAdd = { name -> actions.onChange { f, _ -> f.addNewRoom(name) } },
    onRemoveNew = { index -> actions.onChange { f, _ -> f.removeNewRoom(index) } },
)

/** Preview of a new level with one room to link. */
@Preview
@Composable
private fun PlaceEditorPreview() {
    val places = Places(
        homes = listOf(Home("h1", "Main house"), Home("h2", "Garden house")),
        floors = listOf(Floor("f1", "h1", "First floor", 1)),
        rooms = listOf(Room("r1", "h1", null, "Garage"), Room("r2", "h1", "f1", "Office")),
    )
    val form = PlaceForm(PlaceKind.FLOOR, name = "Attic", homeId = "h1", level = 2, roomIds = setOf("r1"))
    HaacTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            PlaceEditorContent(PlaceEditorUiState(form, places), EditorActions({}, {}, {}, {}))
        }
    }
}
