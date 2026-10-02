package com.stacknoise.haac.feature.schedules.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacEmptyState
import com.stacknoise.haac.core.common.ui.theme.HaacScreenTitle
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.haacButtonColors
import com.stacknoise.haac.core.common.ui.theme.haacFilterChipBorder
import com.stacknoise.haac.core.common.ui.theme.haacFilterChipColors
import com.stacknoise.haac.feature.schedules.R
import com.stacknoise.haac.feature.schedules.data.ScheduleView

/**
 * The Schedules tab (M-10, M-11, M-16, concept 19.7): the schedules of the active instance with the *Next up*
 * banner. [onOpen] opens the detail screen of a schedule. Admins also get the filter chips and the owners.
 */
@Composable
fun SchedulesScreen(
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
    viewModel: SchedulesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val error = state.error?.let { stringResource(it.message) + " (" + it.code + ")" }
    LaunchedEffect(state.error) {
        if (error != null) {
            snackbar.showSnackbar(error)
            viewModel.onErrorShown()
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = { AddButton(onCreate) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        SchedulesContent(state, viewModel::onFilter, viewModel::onToggle, onOpen, onCreate, Modifier.padding(padding))
    }
}

/** Stateless layout: title, banner, filter chips and the cards, or the empty state. */
@Composable
fun SchedulesContent(
    state: SchedulesUiState,
    onFilter: (ScheduleFilter) -> Unit,
    onToggle: (ScheduleView, Boolean) -> Unit,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val list = state.schedules ?: return
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HaacScreenTitle(
                stringResource(R.string.schedules_title),
                modifier = Modifier.padding(top = 24.dp),
            )
        }
        if (state.stale) item { StaleHint() }
        if (list.isEmpty()) {
            item { EmptyState(onCreate) }
            return@LazyColumn
        }
        if (state.admin) item { FilterChips(state.filter, onFilter) }
        state.nextUp?.let { next -> item { NextUp(next) } }
        items(state.visible, key = { it.item.id }) { view ->
            ScheduleCard(
                view = view,
                editable = state.editable,
                showOwner = state.admin,
                onOpen = { onOpen(view.item.id) },
                onToggle = { onToggle(view, it) },
                modifier = if (state.stale) Modifier.alpha(StaleAlpha) else Modifier,
            )
        }
    }
}

/** How strongly cached schedules are dimmed while the connection is down (concept 14.1). */
private const val StaleAlpha = 0.55f

/** *No schedules yet* with its explanation (M-11). */
@Composable
private fun EmptyState(onCreate: () -> Unit) {
    HaacEmptyState(
        icon = painterResource(R.drawable.ic_schedules_clock),
        text = stringResource(R.string.schedules_empty_title) + "\n" + stringResource(R.string.schedules_empty_text),
    ) {
        Button(onClick = onCreate, colors = haacButtonColors(), shape = HaacShapes.Button) {
            Icon(painterResource(R.drawable.ic_schedules_add), null, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.schedules_create), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** The round 60 dp button that opens the editor (M-10). */
@Composable
private fun AddButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = HaacColors.Accent,
        contentColor = HaacColors.OnAccent,
        shape = CircleShape,
        modifier = Modifier.size(60.dp),
    ) { Icon(painterResource(R.drawable.ic_schedules_add), stringResource(R.string.schedules_create)) }
}

/** The banner of the schedule that runs next, "Name · Fri 06:45". */
@Composable
private fun NextUp(view: ScheduleView) {
    val at = view.item.nextRun ?: return
    NextUpBanner(stringResource(R.string.schedules_next_up_text, view.item.name, dayAndClock(at)))
}

/** The note that the list is the last known state and cannot be changed (concept 14.1). */
@Composable
private fun StaleHint() {
    Text(
        stringResource(R.string.schedules_stale),
        style = MaterialTheme.typography.bodyMedium,
        color = HaacColors.Danger,
        modifier = Modifier
            .fillMaxWidth()
            .background(HaacColors.DangerContainer, HaacShapes.Small)
            .padding(12.dp),
    )
}

/** *All* and *Mine* for admins (M-16). */
@Composable
private fun FilterChips(selected: ScheduleFilter, onFilter: (ScheduleFilter) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            ScheduleFilter.ALL to R.string.schedules_filter_all,
            ScheduleFilter.MINE to R.string.schedules_filter_mine,
        ).forEach { (filter, label) ->
            FilterChip(
                selected = filter == selected,
                onClick = { onFilter(filter) },
                label = { Text(stringResource(label)) },
                colors = haacFilterChipColors(),
                border = haacFilterChipBorder(enabled = true, selected = filter == selected),
            )
        }
    }
}
