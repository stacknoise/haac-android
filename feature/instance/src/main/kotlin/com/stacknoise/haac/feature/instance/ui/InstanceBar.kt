package com.stacknoise.haac.feature.instance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.feature.instance.R
import com.stacknoise.haac.feature.instance.domain.SwitchStep

/**
 * The instance switcher above the main area (concept 4.4): the active instance with a dropdown of all instances
 * and *Add instance*. Nothing is shown while only one instance exists; *Settings > Instances* adds the second
 * one. [onSwitched] receives the chosen instance and what follows the switch.
 */
@Composable
fun InstanceBar(
    onSwitched: (String, SwitchStep) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InstanceSwitcherViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    val current = items.firstOrNull { it.active }
    if (items.size < 2 || current == null) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable { open = true }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(Color(current.accent)))
        Text(current.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp))
        Icon(
            painterResource(R.drawable.ic_instance_expand),
            stringResource(R.string.instance_switcher),
            Modifier.padding(start = 4.dp),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { InstanceRow(item, connection) },
                    onClick = {
                        open = false
                        viewModel.onSelect(item.id) { step -> onSwitched(item.id, step) }
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.instance_add)) },
                onClick = {
                    open = false
                    onAdd()
                },
            )
        }
    }
}
