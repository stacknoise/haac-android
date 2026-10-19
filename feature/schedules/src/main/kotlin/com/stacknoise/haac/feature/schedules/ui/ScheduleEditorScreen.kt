package com.stacknoise.haac.feature.schedules.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.stacknoise.haac.feature.schedules.domain.RepeatRule
import com.stacknoise.haac.feature.schedules.domain.ScheduleDraft
import com.stacknoise.haac.feature.schedules.domain.WhenType
import java.time.LocalDate

/**
 * The editor of a schedule (M-12, M-18, M-23, M-24, concept 19.7, 19.10): name, time, date rule, action and entities
 * with a live summary. It opens the *Run at* sheet (M-13), the date sheet and the entity picker (M-14). *Save* is
 * online-only; [onClose] leaves the editor, also after a successful save.
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

/** Which date the date sheet edits: the start of a repeating schedule or the date of a one-off one, or the end. */
private enum class DateTarget { START, END }

/** All sections, scrolling: name, time, repeat, action, entities and the note that it runs on the server. */
@Composable
private fun EditorBody(state: ScheduleEditorUiState, viewModel: ScheduleEditorViewModel, onPick: () -> Unit) {
    val draft = state.form.draft
    var sheet by rememberSaveable { mutableStateOf(false) }
    var dateTarget by rememberSaveable { mutableStateOf<DateTarget?>(null) }
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
        if (state.features.repeat) {
            RepeatSection(draft, viewModel) { dateTarget = it }
        } else {
            WeekdaySection(draft.days, viewModel)
        }
        SummaryCard(summary(state, names), firstRun(state))
        EditorLabel(R.string.schedules_action_label)
        ActionSegments(draft.action, viewModel::onAction)
        EditorLabel(R.string.schedules_entities_label)
        names.forEachIndexed { index, name ->
            val id = draft.entityIds[index]
            EntityRow(name, locked = !state.ownEntities, onRemove = { viewModel.onRemoveEntity(id) })
        }
        if (state.ownEntities) AddEntitiesTile(onPick)
        if (state.showNotify) NotifyRow(draft.notify, viewModel::onNotify)
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
    dateTarget?.let { target -> EditorDateSheet(target, draft.repeat, viewModel) { dateTarget = null } }
}

/** The weekday buttons and presets of a bridge without date rules. */
@Composable
private fun WeekdaySection(days: Set<Int>, viewModel: ScheduleEditorViewModel) {
    WeekdayButtons(days, viewModel::onDay)
    PresetChips(days, viewModel::onDays)
}

/**
 * The date rule (M-23, M-24): *Once* with its date, or *Repeat* with the frequency, the interval, the weekdays of a
 * weekly rule and the rows *From* and *Ends*. [onDate] opens the date sheet for a date.
 */
@Composable
private fun RepeatSection(draft: ScheduleDraft, viewModel: ScheduleEditorViewModel, onDate: (DateTarget) -> Unit) {
    val rule = draft.repeat
    val today = remember { LocalDate.now() }
    ModeSegments(rule.once, viewModel::onOnce)
    if (rule.once) {
        val text = stringResource(R.string.schedules_run_on_row, fullDate(rule.startDate ?: today))
        DateRow(text, stringResource(R.string.schedules_change)) { onDate(DateTarget.START) }
        return
    }
    FreqChips(rule.freq, viewModel::onFreq)
    if (rule.freq.hasInterval) IntervalRow(rule, viewModel::onInterval)
    if (draft.weekly) WeekdaySection(draft.days, viewModel)
    val from = stringResource(R.string.schedules_from_row, shortDate(rule.startDate ?: today))
    DateRow(from, stringResource(R.string.schedules_change)) { onDate(DateTarget.START) }
    val end = rule.endDate
    val endText = end?.let { stringResource(R.string.schedules_ends_row, shortDate(it)) }
        ?: stringResource(R.string.schedules_ends_never)
    val endAction = stringResource(if (end == null) R.string.schedules_set else R.string.schedules_change)
    DateRow(endText, endAction) { onDate(DateTarget.END) }
}

/** The date sheet for [target]: the date of a one-off schedule, the start or the end of a repeating one. */
@Composable
private fun EditorDateSheet(
    target: DateTarget,
    rule: RepeatRule,
    viewModel: ScheduleEditorViewModel,
    onClose: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val start = rule.startDate ?: today
    if (target == DateTarget.END) {
        val min = maxOf(start, today)
        DateSheet(
            spec = DateSheetSpec(R.string.schedules_date_end, R.string.schedules_date_end_hint, min, null),
            initial = rule.endDate ?: min,
            onConfirm = { viewModel.onEndDate(it); onClose() },
            onDismiss = onClose,
            onClear = { viewModel.onEndDate(null); onClose() },
        )
        return
    }
    val spec = if (rule.once) {
        val last = today.plusDays(OnceRange)
        DateSheetSpec(R.string.schedules_date_run_on, R.string.schedules_date_once_hint, today, last)
    } else {
        DateSheetSpec(R.string.schedules_date_start, null, today, null)
    }
    DateSheet(spec, start, onConfirm = { viewModel.onStartDate(it); onClose() }, onDismiss = onClose)
}

/** How far ahead the bridge accepts the date of a one-off schedule, in days (concept 19.10). */
private const val OnceRange = 366L

/** The live summary: "Every weekday at 06:45 · Lamp turns on", or what is still missing. */
@Composable
private fun summary(state: ScheduleEditorUiState, names: List<String>): String {
    val draft = state.form.draft
    val incomplete = !draft.daysValid || !draft.repeat.valid || (state.ownEntities && names.isEmpty())
    if (incomplete) return stringResource(R.string.schedules_summary_none)
    val phrase = whenPhrase(draft.whenType, draft.time, draft.offsetMin)
    return repeatSummary(draft.repeat, draft.days.toList(), phrase) + " · " + targetText(names, draft.action)
}

/** "First run: Thu, 22 Oct" from the preview of the bridge, or null while there is none. */
@Composable
private fun firstRun(state: ScheduleEditorUiState): String? =
    state.form.preview.firstOrNull()?.let { stringResource(R.string.schedules_first_run, shortDate(dateOf(it))) }

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
