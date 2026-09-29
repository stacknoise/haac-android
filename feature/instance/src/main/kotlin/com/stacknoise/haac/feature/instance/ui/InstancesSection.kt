package com.stacknoise.haac.feature.instance.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.feature.instance.R
import com.stacknoise.haac.feature.instance.domain.InstanceItem

/** The dialog that is open for one instance of the list. */
private sealed interface InstanceDialog {
    /** The instance the dialog is about. */
    val item: InstanceItem

    /** Name and colour of [item]. */
    data class Edit(override val item: InstanceItem) : InstanceDialog

    /** Confirmation to remove [item]. */
    data class Remove(override val item: InstanceItem) : InstanceDialog
}

/**
 * *Settings > Instances* (concept 4.4): all instances, a tap switches to one, the menu of a row renames, recolours
 * or removes it, *Add instance* runs the sign-in flow again.
 */
@Composable
fun InstancesSection(
    actions: InstanceListActions,
    modifier: Modifier = Modifier,
    viewModel: InstanceSwitcherViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<InstanceDialog?>(null) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.instance_section_title).uppercase(), style = SectionLabelStyle)
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                InstanceRow(
                    item = item,
                    connection = connection,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !item.active) {
                            viewModel.onSelect(item.id) { step -> actions.onSwitched(item.id, step) }
                        }
                        .padding(vertical = 4.dp),
                )
                InstanceMenu(
                    onEdit = { dialog = InstanceDialog.Edit(item) },
                    onRemove = { dialog = InstanceDialog.Remove(item) },
                )
            }
        }
        OutlinedButton(
            onClick = actions.onAdd,
            shape = HaacShapes.Medium,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(stringResource(R.string.instance_add), style = MaterialTheme.typography.titleSmall)
        }
    }
    when (val open = dialog) {
        is InstanceDialog.Edit -> EditInstanceDialog(
            item = open.item,
            onSave = { name, accent ->
                viewModel.onSave(open.item.id, name, accent)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        is InstanceDialog.Remove -> RemoveInstanceDialog(
            item = open.item,
            onConfirm = {
                viewModel.onRemove(open.item.id, actions.onRemoved)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

/** The three-dot menu of one row with *Edit instance* and *Remove*. */
@Composable
private fun InstanceMenu(onEdit: () -> Unit, onRemove: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(painterResource(R.drawable.ic_instance_more), stringResource(R.string.instance_more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.instance_edit)) },
                onClick = {
                    open = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.instance_remove)) },
                onClick = {
                    open = false
                    onRemove()
                },
            )
        }
    }
}
