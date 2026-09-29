package com.stacknoise.haac.feature.layout.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.stacknoise.haac.feature.layout.R
import com.stacknoise.haac.feature.layout.domain.PlaceForm
import com.stacknoise.haac.feature.layout.domain.PlaceKind
import com.stacknoise.haac.core.database.layout.Places

/**
 * Confirms a deletion (concept 6.2): a home lists its levels and rooms; a level with rooms asks whether they stay
 * directly in the home (default) or are deleted too. [onConfirm] gets that choice.
 */
@Composable
internal fun DeleteDialog(form: PlaceForm, places: Places, onConfirm: (Boolean) -> Unit, onDismiss: () -> Unit) {
    var withRooms by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_title, form.name.trim())) },
        text = {
            if (form.kind == PlaceKind.HOME) {
                val levels = places.floorsOf(form.id).size
                val rooms = places.roomsOf(form.id).size
                Text(
                    stringResource(
                        R.string.delete_home_text,
                        form.name.trim(),
                        pluralStringResource(R.plurals.places_levels, levels, levels),
                        pluralStringResource(R.plurals.places_rooms, rooms, rooms),
                    ),
                )
            } else {
                Column {
                    Option(R.string.delete_floor_keep_rooms, selected = !withRooms) { withRooms = false }
                    Option(R.string.delete_floor_with_rooms, selected = withRooms) { withRooms = true }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(withRooms) }) {
                Text(stringResource(R.string.delete_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.delete_cancel)) } },
    )
}

/** One choice of the level dialog. */
@Composable
private fun Option(@StringRes text: Int, selected: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(stringResource(text), style = MaterialTheme.typography.bodyLarge)
    }
}
