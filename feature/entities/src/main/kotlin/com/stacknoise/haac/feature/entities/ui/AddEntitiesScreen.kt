package com.stacknoise.haac.feature.entities.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.ErrorMessage
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.PickerRow

/** *Add entities* for one room (M-04, concept 7.1, 7.2); [onClose] after adding or on Back. */
@Composable
fun AddEntitiesScreen(onClose: () -> Unit, viewModel: AddEntitiesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.status.done) { if (state.status.done) onClose() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.ic_entities_back), stringResource(R.string.add_back))
            }
            Text(
                state.roomName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(stringResource(R.string.add_title), style = MaterialTheme.typography.headlineLarge)
        DomainTabs(state, viewModel::onDomain)
        OutlinedTextField(
            value = state.input.filter,
            onValueChange = viewModel::onFilter,
            placeholder = { Text(stringResource(R.string.add_filter)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_entities_search), contentDescription = null) },
            singleLine = true,
            shape = HaacShapes.Small,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
        state.status.error?.let { ErrorMessage(it) }
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (state.rows.isEmpty()) item { Hint(stringResource(R.string.add_nothing)) }
            items(state.rows, key = { it.entry.entityId }) { row ->
                PickerRowItem(row, row.entry.entityId in state.input.picked) { viewModel.onToggle(row.entry.entityId) }
            }
        }
        AddButton(state, viewModel::onAdd)
    }
}

/** *Switch 12 · Sensor 24 · Climate 3* (M-04). */
@Composable
private fun DomainTabs(state: AddEntitiesUiState, onSelect: (String) -> Unit) {
    if (state.domains.isEmpty()) return
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        state.domains.forEachIndexed { index, domain ->
            SegmentedButton(
                selected = domain == state.input.domain,
                onClick = { onSelect(domain) },
                shape = SegmentedButtonDefaults.itemShape(index, state.domains.size),
                label = { Text("${domainLabel(domain)} ${state.counts[domain] ?: 0}", maxLines = 1) },
            )
        }
    }
}

/** Checkbox, name and live state; entities in this room are checked and disabled, others show their room. */
@Composable
private fun PickerRowItem(row: PickerRow, picked: Boolean, onToggle: () -> Unit) {
    val enabled = !row.inRoom
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onToggle).padding(vertical = 4.dp),
    ) {
        Checkbox(checked = picked || row.inRoom, onCheckedChange = { onToggle() }, enabled = enabled)
        Text(
            row.entry.tile.name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onBackground else secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        val hint = when {
            row.inRoom -> stringResource(R.string.add_here)
            row.otherRoom != null -> stringResource(R.string.add_in_room, row.otherRoom)
            else -> stateText(row.entry.tile)
        }
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            color = secondary,
            maxLines = 1,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** *Add N entities* with the picks per domain below (M-04). */
@Composable
private fun AddButton(state: AddEntitiesUiState, onAdd: () -> Unit) {
    val total = state.pickedByDomain.values.sum()
    OutlinedButton(
        onClick = onAdd,
        enabled = total > 0 && !state.status.busy,
        shape = HaacShapes.Medium,
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(64.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val label = if (total == 0) {
                stringResource(R.string.add_select)
            } else {
                pluralStringResource(R.plurals.add_button, total, total)
            }
            Text(label)
            if (total > 0) Text(pickedSummary(state.pickedByDomain), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** "2 switches · 1 sensor · 1 climate". */
@Composable
private fun pickedSummary(picked: Map<String, Int>): String {
    val parts = picked.entries.map { (domain, count) ->
        when (domain) {
            "switch" -> pluralStringResource(R.plurals.add_switches, count, count)
            "sensor" -> pluralStringResource(R.plurals.add_sensors, count, count)
            "climate" -> pluralStringResource(R.plurals.add_climates, count, count)
            else -> "$count $domain"
        }
    }
    return parts.joinToString(" · ")
}

/** Tab label of [domain]; domains beyond v1 show their id. */
@Composable
private fun domainLabel(domain: String): String = when (domain) {
    "switch" -> stringResource(R.string.add_domain_switch)
    "sensor" -> stringResource(R.string.add_domain_sensor)
    "climate" -> stringResource(R.string.add_domain_climate)
    else -> domain
}

/** Secondary hint text in a list. */
@Composable
internal fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 16.dp),
    )
}
