package com.stacknoise.haac.feature.entities.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.sp
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.MonoFontFamily
import com.stacknoise.haac.core.common.ui.theme.haacButtonColors
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.Tile

/**
 * *Rename entity* (M-07, concept 7.3): the local name only; *Use default name* clears it. [onSave] gets the new
 * alias, or null for the default name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RenameDialog(tile: Tile, onSave: (String?) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable(tile.entityId) { mutableStateOf(tile.name) }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = HaacShapes.Dialog, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(modifier = Modifier.padding(start = 26.dp, end = 26.dp, top = 24.dp, bottom = 16.dp)) {
                Text(stringResource(R.string.rename_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.rename_text, tile.defaultName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                    shape = HaacShapes.Medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = HaacColors.Background,
                        unfocusedContainerColor = HaacColors.Background,
                        focusedBorderColor = HaacColors.Accent,
                        unfocusedBorderColor = HaacColors.Accent,
                        cursorColor = HaacColors.Accent,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                )
                Text(
                    tile.entityId,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily, fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                // Saving the default name stores no alias, so a later change of the default shows up (7.3).
                Button(
                    onClick = { onSave(name.trim().takeIf { it.isNotEmpty() && it != tile.defaultName }) },
                    shape = HaacShapes.Button,
                    colors = haacButtonColors(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(top = 24.dp),
                ) { Text(stringResource(R.string.rename_save)) }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    TextButton(onClick = { onSave(null) }) { Text(stringResource(R.string.rename_default)) }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.rename_cancel), color = HaacColors.OnSurfaceVariantStrong)
                    }
                }
            }
        }
    }
}
