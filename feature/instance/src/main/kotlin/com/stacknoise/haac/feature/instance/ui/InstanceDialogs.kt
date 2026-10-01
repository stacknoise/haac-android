package com.stacknoise.haac.feature.instance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.InstanceAccents
import com.stacknoise.haac.core.common.ui.theme.haacTextFieldColors
import com.stacknoise.haac.feature.instance.R
import com.stacknoise.haac.feature.instance.domain.InstanceItem

/** Dialog for the local name and accent colour of [item] (concept 4.4); [onSave] receives both. */
@Composable
fun EditInstanceDialog(item: InstanceItem, onSave: (String, Long) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(item.name) }
    var accent by remember { mutableLongStateOf(item.accent) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.instance_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.instance_name)) },
                    singleLine = true,
                    shape = HaacShapes.Medium,
                    colors = haacTextFieldColors(),
                )
                AccentChoices(selected = accent, onSelect = { accent = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, accent) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.instance_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.instance_cancel)) } },
    )
}

/** One circle per accent colour of the palette; the [selected] one has a ring. */
@Composable
private fun AccentChoices(selected: Long, onSelect: (Long) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InstanceAccents.Palette.forEach { colour ->
            val ring = if (colour == selected) MaterialTheme.colorScheme.onSurface else Color.Transparent
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(colour))
                    .border(2.dp, ring, CircleShape)
                    .clickable { onSelect(colour) },
            )
        }
    }
}

/** Confirmation before [item] is removed together with its layout and cache (concept 4.4). */
@Composable
fun RemoveInstanceDialog(item: InstanceItem, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.instance_remove_title, item.name)) },
        text = { Text(stringResource(R.string.instance_remove_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.instance_remove), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.instance_cancel)) } },
    )
}
