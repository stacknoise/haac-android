package com.stacknoise.haac.feature.schedules.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.haacButtonColors
import com.stacknoise.haac.core.common.ui.theme.haacTextFieldColors
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.feature.schedules.R
import com.stacknoise.haac.feature.schedules.domain.WhenType

/**
 * The editor of a schedule (M-12, M-18, concept 19.7): name, time, weekdays, action and entities with a live summary.
 * It opens the *Run at* sheet (M-13) and the entity picker (M-14). *Save* is online-only; [onClose] leaves the editor,
 * also after a successful save.
 */
@Composable
fun ScheduleEditorScreen(onClose: () -> Unit, viewModel: ScheduleEditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var picking by rememberSaveable { mutableStateOf(false) }
    val error = state.form.error?.takeIf { it != ErrorCode.SCH_CONFLICT }
        ?.let { stringResource(it.message) + " (" + it.code + ")" }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.form.saved) { if (state.form.saved) onClose() }
    LaunchedEffect(error) {
        if (error != null) {
            snackbar.showSnackbar(error)
            viewModel.onErrorShown()
        }
    }
    if (picking) {
        EntityPicker(
            candidates = state.candidates,
            selected = state.form.draft.entityIds,
            editing = state.form.original != null,
            onDone = { ids ->
                viewModel.onEntities(ids)
                picking = false
            },
            onBack = { picking = false },
        )
        return
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.imePadding(),
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val isNew = state.form.original == null
            TopBar(isNew, state.canSave, onClose = onClose, onSave = viewModel::onSave)
            if (state.form.loaded) EditorBody(state, viewModel, onPick = { picking = true })
        }
    }
    if (state.form.error == ErrorCode.SCH_CONFLICT) {
        ConflictDialog(onReload = viewModel::onReload, onKeep = viewModel::onErrorShown)
    }
}

/** Close, the title and the *Save* pill (enabled while the draft is valid and the connection is open). */
@Composable
private fun TopBar(isNew: Boolean, canSave: Boolean, onClose: () -> Unit, onSave: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
        IconButton(onClick = onClose) {
            Icon(painterResource(R.drawable.ic_schedules_close), stringResource(R.string.schedules_close))
        }
        Text(
            stringResource(if (isNew) R.string.schedules_new_title else R.string.schedules_edit_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = onSave,
            enabled = canSave,
            colors = haacButtonColors(),
            shape = HaacShapes.Full,
            modifier = Modifier.padding(end = 8.dp),
        ) { Text(stringResource(R.string.schedules_save)) }
    }
}

/** All sections, scrolling: name, time, repeat, action, entities and the note that it runs on the server. */
@Composable
private fun EditorBody(state: ScheduleEditorUiState, viewModel: ScheduleEditorViewModel, onPick: () -> Unit) {
    val draft = state.form.draft
    var sheet by rememberSaveable { mutableStateOf(false) }
    val names = draft.entityIds.map { state.candidates.nameOf(it) }
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
    ) {
        state.owner?.let { OwnerBanner(it) }
        EditorLabel(R.string.schedules_name_label)
        OutlinedTextField(
            value = draft.name,
            onValueChange = viewModel::onName,
            placeholder = { Text(stringResource(R.string.schedules_name_hint)) },
            singleLine = true,
            shape = HaacShapes.Medium,
            colors = haacTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        EditorLabel(R.string.schedules_time_label)
        TimeCard(
            time = if (draft.whenType == WhenType.TIME) draft.time.toString() else sunName(draft.whenType),
            subtitle = if (draft.whenType == WhenType.TIME) null else offsetText(draft.whenType, draft.offsetMin),
            onChange = { sheet = true },
        )
        EditorLabel(R.string.schedules_repeat_label)
        WeekdayButtons(draft.days, viewModel::onDay)
        PresetChips(draft.days, viewModel::onDays)
        SummaryCard(summary(state, names))
        EditorLabel(R.string.schedules_action_label)
        ActionSegments(draft.action, viewModel::onAction)
        EditorLabel(R.string.schedules_entities_label)
        names.forEachIndexed { index, name ->
            val id = draft.entityIds[index]
            EntityRow(name, locked = !state.ownEntities, onRemove = { viewModel.onRemoveEntity(id) })
        }
        if (state.ownEntities) AddEntitiesTile(onPick)
        Text(
            stringResource(R.string.schedules_runs_on_server),
            style = MaterialTheme.typography.bodyMedium,
            color = HaacColors.OnSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    if (sheet) {
        TimeSheet(
            draft = draft,
            onConfirm = { type, time, offset ->
                viewModel.onType(type)
                viewModel.onTime(time)
                viewModel.onOffset(offset)
                sheet = false
            },
            onDismiss = { sheet = false },
        )
    }
}

/** The live summary: "Every weekday at 06:45 · Lamp turns on", or what is still missing. */
@Composable
private fun summary(state: ScheduleEditorUiState, names: List<String>): String {
    val draft = state.form.draft
    val incomplete = draft.days.isEmpty() || (state.ownEntities && names.isEmpty())
    if (incomplete) return stringResource(R.string.schedules_summary_none)
    val repeat = repeatText(draft.days.toList())
    val target = targetText(names, draft.action)
    return if (draft.whenType == WhenType.TIME) {
        stringResource(R.string.schedules_summary_time, repeat, draft.time.toString(), target)
    } else {
        val sun = offsetText(draft.whenType, draft.offsetMin).replaceFirstChar { it.lowercase() }
        stringResource(R.string.schedules_summary_sun, repeat, sun, target)
    }
}

/** The schedule changed on the server while it was edited (HAAC-SCH-004): reload it and keep the edits on top. */
@Composable
private fun ConflictDialog(onReload: () -> Unit, onKeep: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeep,
        title = { Text(stringResource(R.string.schedules_conflict_title)) },
        text = { Text(stringResource(R.string.schedules_conflict_text)) },
        confirmButton = { TextButton(onClick = onReload) { Text(stringResource(R.string.schedules_reload)) } },
        dismissButton = { TextButton(onClick = onKeep) { Text(stringResource(R.string.schedules_keep_editing)) } },
        shape = HaacShapes.Dialog,
    )
}
