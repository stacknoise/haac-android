package com.stacknoise.haac.feature.entities.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.ErrorMessage
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.common.ui.theme.haacButtonColors
import com.stacknoise.haac.feature.entities.R

/** *Add to room* for entities of a notification entry (M-09, concept 9.1); [onClose] after adding or on Back. */
@Composable
fun AssignScreen(onClose: () -> Unit, viewModel: AssignViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.status.done) { if (state.status.done) onClose() }
    val groups = state.groups ?: return
    val chosen = groups.flatMap { it.rooms }.firstOrNull { it.id == state.roomId }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
    ) {
        IconButton(onClick = onClose, modifier = Modifier.padding(top = 8.dp)) {
            Icon(painterResource(R.drawable.ic_entities_back), stringResource(R.string.add_back))
        }
        Text(stringResource(R.string.assign_title), style = MaterialTheme.typography.headlineLarge)
        Text(
            state.entries.joinToString(", ") { it.tile.name }.ifEmpty { stringResource(R.string.assign_gone) },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        state.status.error?.let { ErrorMessage(it) }
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (groups.isEmpty()) item { Hint(stringResource(R.string.assign_no_rooms)) }
            groups.forEach { group ->
                item(key = "group-${group.home.id}-${group.floor?.id}") {
                    Text(
                        listOfNotNull(group.home.name, group.floor?.name).joinToString(" · ").uppercase(),
                        style = SectionLabelStyle,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                }
                items(group.rooms, key = { it.id }) { room ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.onSelectRoom(room.id) },
                    ) {
                        RadioButton(selected = room.id == state.roomId, onClick = { viewModel.onSelectRoom(room.id) })
                        Text(room.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        Button(
            onClick = viewModel::onAdd,
            enabled = chosen != null && state.entries.isNotEmpty() && !state.status.busy,
            shape = HaacShapes.Button,
            colors = haacButtonColors(),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(56.dp),
        ) {
            val label = chosen?.let { stringResource(R.string.assign_button, it.name) }
            Text(label ?: stringResource(R.string.assign_choose))
        }
    }
}
