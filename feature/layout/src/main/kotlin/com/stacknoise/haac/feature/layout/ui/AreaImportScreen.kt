package com.stacknoise.haac.feature.layout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.ErrorMessage
import com.stacknoise.haac.core.common.ui.theme.HaacCheckbox
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.common.ui.theme.haacButtonColors
import com.stacknoise.haac.core.common.ui.theme.haacTextFieldColors
import com.stacknoise.haac.feature.layout.R
import com.stacknoise.haac.feature.layout.domain.HaArea

/** The import wizard: choose the home and the Home Assistant areas to take over (concept 6.3). */
@Composable
fun AreaImportScreen(onClose: () -> Unit, viewModel: AreaImportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val close by rememberUpdatedState(onClose)
    LaunchedEffect(state.done) { if (state.done) close() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.ic_layout_close), stringResource(R.string.editor_close))
            }
            Text(
                stringResource(R.string.import_title).uppercase(),
                style = SectionLabelStyle.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        when {
            state.loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator() }
            state.areas == null -> ImportFailure(state, viewModel::load)
            else -> ImportBody(state, viewModel, Modifier.weight(1f))
        }
        ImportBar(state, viewModel::onImport)
    }
}

/** The error of loading the areas with *Try again*. */
@Composable
private fun ImportFailure(state: AreaImportUiState, onRetry: () -> Unit) {
    Column(Modifier.padding(top = 24.dp)) {
        state.error?.let { ErrorMessage(it) }
        TextButton(onClick = onRetry) { Text(stringResource(R.string.import_try_again)) }
    }
}

/** Home choice, then the areas grouped by level with a check box each. */
@Composable
private fun ImportBody(state: AreaImportUiState, viewModel: AreaImportViewModel, modifier: Modifier) {
    val areas = state.areas ?: return
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.import_intro), style = MaterialTheme.typography.bodyMedium)
        if (areas.areas.isEmpty()) {
            Text(
                stringResource(R.string.import_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
            return@Column
        }
        SectionLabel(R.string.import_home_section)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.places.homes.forEach { home ->
                Choice(home.name, home.id == state.homeId) { viewModel.onHome(home.id) }
            }
            Choice(stringResource(R.string.import_new_home), state.homeId == null) { viewModel.onHome(null) }
        }
        if (state.homeId == null) {
            OutlinedTextField(
                value = state.newHomeName,
                onValueChange = viewModel::onNewHomeName,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.import_new_home_hint)) },
                shape = HaacShapes.Medium,
                colors = haacTextFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        SectionLabel(R.string.import_areas_section)
        TextButton(onClick = viewModel::onToggleAll) { Text(stringResource(R.string.import_toggle_all)) }
        areas.floors.forEach { floor ->
            AreaGroup(floor.name, areas.areasOn(floor.id), state, viewModel::onToggle)
        }
        val loose = areas.areasOn(null)
        if (loose.isNotEmpty()) AreaGroup(stringResource(R.string.import_no_level), loose, state, viewModel::onToggle)
        Spacer(Modifier.height(16.dp))
    }
}

/** One level of Home Assistant (or *No level*) with its areas. */
@Composable
private fun AreaGroup(title: String, areas: List<HaArea>, state: AreaImportUiState, onToggle: (String) -> Unit) {
    if (areas.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
    areas.forEach { area ->
        val checked = area.id in state.selected
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle(area.id) }),
        ) {
            HaacCheckbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
            Text(area.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                pluralStringResource(R.plurals.import_entities, area.entityCount, area.entityCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Summary of what the import adds, the error if it failed, and the *Import* button. */
@Composable
private fun ImportBar(state: AreaImportUiState, onImport: () -> Unit) {
    val plan = state.plan
    Column(Modifier.padding(vertical = 12.dp)) {
        if (state.areas?.areas?.isNotEmpty() == true) {
            val levels = pluralStringResource(R.plurals.import_new_levels, plan.newLevels, plan.newLevels)
            val rooms = pluralStringResource(R.plurals.import_new_rooms, plan.newRooms, plan.newRooms)
            Text("$levels · $rooms", style = MaterialTheme.typography.bodyMedium)
            if (plan.skipped > 0) {
                Text(
                    pluralStringResource(R.plurals.import_skipped, plan.skipped, plan.skipped),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.error?.let { ErrorMessage(it) }
            Button(
                onClick = onImport,
                enabled = state.canImport,
                shape = HaacShapes.Button,
                colors = haacButtonColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(56.dp),
            ) {
                Text(stringResource(R.string.import_action), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
