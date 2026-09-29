package com.stacknoise.haac.feature.entities.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.MonoFontFamily
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.Tile

/**
 * *Rename entity* (M-07, concept 7.3): the local name only; *Use default name* clears it. [onSave] gets the new
 * alias, or null for the default name.
 */
@Composable
internal fun RenameDialog(tile: Tile, onSave: (String?) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable(tile.entityId) { mutableStateOf(tile.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.rename_text, tile.defaultName),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    shape = HaacShapes.Small,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
                Text(
                    tile.entityId,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            // Saving the default name stores no alias, so a later change of the default shows up (7.3).
            TextButton(onClick = { onSave(name.trim().takeIf { it.isNotEmpty() && it != tile.defaultName }) }) {
                Text(stringResource(R.string.rename_save))
            }
        },
        dismissButton = {
            TextButton(onClick = { onSave(null) }) { Text(stringResource(R.string.rename_default)) }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.rename_cancel)) }
        },
    )
}
