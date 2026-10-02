package com.stacknoise.haac.feature.schedules.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacEmptyState
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.feature.schedules.R
import com.stacknoise.haac.feature.schedules.data.ScheduleView
import com.stacknoise.haac.feature.schedules.domain.WhenType
import com.stacknoise.haac.feature.schedules.domain.nextRuns

/**
 * The detail screen of one schedule (M-15, M-17, concept 19.7): the time, weekdays, what it does, the *Enabled*
 * switch, the next runs and the last run, and *Delete schedule* after a confirmation. [onClose] returns to the list.
 */
@Composable
fun ScheduleDetailScreen(onClose: () -> Unit, viewModel: ScheduleDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val error = state.error?.let { stringResource(it.message) + " (" + it.code + ")" }
    LaunchedEffect(state.error) {
        if (error != null) {
            snackbar.showSnackbar(error)
            viewModel.onErrorShown()
        }
    }
    LaunchedEffect(deleted) { if (deleted) onClose() }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val view = state.view
            TopRow(onClose)
            when {
                view != null -> DetailContent(view, state, viewModel::onEnabled, viewModel::onDelete)
                state.loaded -> HaacEmptyState(
                    painterResource(R.drawable.ic_schedules_warning),
                    stringResource(R.string.schedules_not_found),
                )
            }
        }
    }
}

/** Back arrow row at the top. */
@Composable
private fun TopRow(onClose: () -> Unit) {
    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        IconButton(onClick = onClose) {
            Icon(painterResource(R.drawable.ic_schedules_back), stringResource(R.string.schedules_back))
        }
    }
}

/** Everything below the top row. */
@Composable
private fun DetailContent(
    view: ScheduleView,
    state: ScheduleDetailUiState,
    onEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val item = view.item
    var confirming by rememberSaveable { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
            .then(if (state.stale) Modifier.alpha(StaleAlpha) else Modifier),
    ) {
        Text(item.name, style = SectionLabelStyle)
        Text(
            timeText(item),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp, letterSpacing = (-0.035).em),
        )
        if (item.whenType != WhenType.TIME) {
            Text(
                offsetText(item.whenType, item.offsetMin),
                style = MaterialTheme.typography.titleMedium,
                color = HaacColors.Accent,
            )
        }
        Text(repeatText(item.days), style = MaterialTheme.typography.titleMedium, color = HaacColors.Accent)
        WeekdayDots(item.days, Modifier.padding(vertical = 8.dp))
        if (!item.own) OwnerCard(item.ownerName)
        view.entityNames.forEach { name -> ActionCard(name, actionVerb(item.action)) }
        if (!item.own) LockedHint()
        EnabledCard(item.enabled, state.editable, onEnabled)
        NextRuns(item.let { nextRuns(it, System.currentTimeMillis()) })
        LastRunRow(item.lastRun)
        TextButton(onClick = { confirming = true }, enabled = state.editable) {
            Icon(
                painterResource(R.drawable.ic_schedules_delete),
                null,
                tint = HaacColors.Danger,
                modifier = Modifier.size(20.dp),
            )
            Text(
                stringResource(R.string.schedules_delete),
                color = HaacColors.Danger,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
    if (confirming) {
        DeleteDialog(item.name, onConfirm = { confirming = false; onDelete() }, onDismiss = { confirming = false })
    }
}

/** How strongly the cached schedule is dimmed while the connection is down (concept 14.1). */
private const val StaleAlpha = 0.55f

/** Asks before deleting the schedule [name]. */
@Composable
private fun DeleteDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.schedules_delete_title)) },
        text = { Text(stringResource(R.string.schedules_delete_text, name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.schedules_delete), color = HaacColors.Danger, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.schedules_cancel)) } },
        shape = HaacShapes.Dialog,
    )
}
