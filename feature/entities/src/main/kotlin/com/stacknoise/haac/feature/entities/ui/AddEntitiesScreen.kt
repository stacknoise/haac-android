package com.stacknoise.haac.feature.entities.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.ErrorMessage
import com.stacknoise.haac.core.common.ui.LocalWindowWidthClass
import com.stacknoise.haac.core.common.ui.WindowWidthClass
import com.stacknoise.haac.core.common.ui.theme.HaacCheckbox
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacScreenTitle
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.common.ui.theme.haacButtonColors
import com.stacknoise.haac.core.common.ui.theme.haacFilterChipBorder
import com.stacknoise.haac.core.common.ui.theme.haacFilterChipColors
import com.stacknoise.haac.core.common.ui.theme.haacTextFieldColors
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.DomainLabels
import com.stacknoise.haac.feature.entities.domain.PickerRow

/**
 * *Add entities* for one room (M-04, concept 7.1, 7.2); [onClose] after adding or on Back. From the medium class
 * it is a centred dialog of at most 720 × 640 dp (concept 15.7).
 */
@Composable
fun AddEntitiesScreen(onClose: () -> Unit, viewModel: AddEntitiesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.status.done) { if (state.status.done) onClose() }
    if (LocalWindowWidthClass.current == WindowWidthClass.Compact) {
        val background = MaterialTheme.colorScheme.background
        AddEntitiesContent(state, viewModel, onClose, Modifier.fillMaxSize().background(background))
    } else {
        Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                color = HaacColors.Surface,
                shape = RoundedCornerShape(DialogCorner),
                modifier = Modifier
                    .fillMaxWidth(DialogFill)
                    .widthIn(max = DialogWidth)
                    .fillMaxHeight(DialogFill)
                    .heightIn(max = DialogHeight),
            ) {
                AddEntitiesContent(state, viewModel, onClose, Modifier.fillMaxSize())
            }
        }
    }
}

/** Width of the add-entities dialog (concept 15.7). */
private val DialogWidth = 720.dp

/** Height of the add-entities dialog (concept 15.7). */
private val DialogHeight = 640.dp

/** Share of a small window the add-entities dialog may fill. */
private const val DialogFill = 0.9f

/** Corner radius of the add-entities dialog. */
private val DialogCorner = 28.dp

/** Title, room, kind tabs, filter, the entities to pick and the *Add* button. */
@Composable
private fun AddEntitiesContent(
    state: AddEntitiesUiState,
    viewModel: AddEntitiesViewModel,
    onClose: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier.imePadding().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.ic_entities_back), stringResource(R.string.add_back))
            }
            Text(state.roomName.uppercase(), style = SectionLabelStyle)
        }
        HaacScreenTitle(stringResource(R.string.add_title))
        DomainTabs(state, viewModel::labels, viewModel::onDomain)
        OutlinedTextField(
            value = state.input.filter,
            onValueChange = viewModel::onFilter,
            placeholder = { Text(stringResource(R.string.add_filter)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_entities_search), contentDescription = null) },
            singleLine = true,
            shape = HaacShapes.Medium,
            colors = haacTextFieldColors(),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
        state.status.error?.let { ErrorMessage(it) }
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (state.rows.isEmpty()) item { Hint(stringResource(R.string.add_nothing)) }
            items(state.rows, key = { it.entry.entityId }) { row ->
                PickerRowItem(row, row.entry.entityId in state.input.picked) { viewModel.onToggle(row.entry.entityId) }
            }
        }
        AddButton(state, viewModel::labels, viewModel::onAdd)
    }
}

/**
 * *Switch 12 · Sensor 24 · Climate 3* (M-04). With only one kind of entity there is nothing to switch between, so
 * the chips give way to a plain caption *Switch · 2*.
 */
@Composable
private fun DomainTabs(
    state: AddEntitiesUiState,
    labels: (String) -> DomainLabels?,
    onSelect: (String) -> Unit,
) {
    val single = state.domains.singleOrNull()
    if (single != null) {
        Text(
            "${domainLabel(single, labels)} · ${state.counts[single] ?: 0}".uppercase(),
            style = SectionLabelStyle,
            modifier = Modifier.padding(top = 16.dp),
        )
        return
    }
    if (state.domains.isEmpty()) return
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    ) {
        items(state.domains) { domain ->
            val selected = domain == state.input.domain
            FilterChip(
                selected = selected,
                onClick = { onSelect(domain) },
                label = { Text("${domainLabel(domain, labels)} ${state.counts[domain] ?: 0}", maxLines = 1) },
                colors = haacFilterChipColors(),
                border = haacFilterChipBorder(enabled = true, selected = selected),
                modifier = Modifier.height(40.dp),
            )
        }
    }
}

/** Checkbox, name and live state; entities in this room are checked and disabled, others show their room. */
@Composable
private fun PickerRowItem(row: PickerRow, picked: Boolean, onToggle: () -> Unit) {
    val enabled = !row.inRoom
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val checked = picked || row.inRoom
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(HaacShapes.Medium)
            .background(if (picked) HaacColors.AccentTint else Color.Transparent)
            .toggleable(checked, enabled, Role.Checkbox) { onToggle() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        HaacCheckbox(checked = checked, onCheckedChange = null, enabled = enabled)
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
private fun AddButton(state: AddEntitiesUiState, labels: (String) -> DomainLabels?, onAdd: () -> Unit) {
    val total = state.pickedByDomain.values.sum()
    Button(
        onClick = onAdd,
        enabled = total > 0 && !state.status.busy,
        shape = HaacShapes.Button,
        colors = haacButtonColors(),
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).heightIn(min = 58.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val label = if (total == 0) {
                stringResource(R.string.add_select)
            } else {
                pluralStringResource(R.plurals.add_button, total, total)
            }
            Text(label)
            if (total > 0) Text(pickedSummary(state.pickedByDomain, labels), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** "2 switches · 1 sensor · 1 climate". */
@Composable
private fun pickedSummary(picked: Map<String, Int>, labels: (String) -> DomainLabels?): String {
    val parts = picked.entries.map { (domain, count) ->
        labels(domain)?.let { pluralStringResource(it.count, count, count) } ?: "$count $domain"
    }
    return parts.joinToString(" · ")
}

/** Tab label of [domain]; domains beyond v1 show their id. */
@Composable
private fun domainLabel(domain: String, labels: (String) -> DomainLabels?): String =
    labels(domain)?.let { stringResource(it.name) } ?: domain

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
