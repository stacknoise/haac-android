package com.stacknoise.haac.feature.layout.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.stacknoise.haac.feature.layout.R
import com.stacknoise.haac.feature.layout.domain.PlaceForm
import com.stacknoise.haac.feature.layout.domain.PlaceKind
import com.stacknoise.haac.feature.layout.domain.Places

/** "New level", "Edit room" … for the header of the form. */
@StringRes
internal fun editorTitle(form: PlaceForm): Int = when (form.kind) {
    PlaceKind.HOME -> if (form.isNew) R.string.editor_title_new_home else R.string.editor_title_edit_home
    PlaceKind.FLOOR -> if (form.isNew) R.string.editor_title_new_level else R.string.editor_title_edit_level
    PlaceKind.ROOM -> if (form.isNew) R.string.editor_title_new_room else R.string.editor_title_edit_room
}

/** *Create home / level / room*, or *Save* for an existing place. */
@StringRes
internal fun saveLabel(form: PlaceForm): Int = when {
    !form.isNew -> R.string.editor_save
    form.kind == PlaceKind.HOME -> R.string.editor_create_home
    form.kind == PlaceKind.FLOOR -> R.string.editor_create_level
    else -> R.string.editor_create_room
}

/** The summary above the button of a new place: "Saving creates Attic in Main house with 1 room." (M-03). */
@Composable
internal fun summary(form: PlaceForm, places: Places): String {
    val name = form.name.trim()
    val home = places.home(form.homeId)?.name.orEmpty()
    val rooms = form.roomIds.size + form.newRooms.size
    return when (form.kind) {
        PlaceKind.HOME -> if (rooms == 0) {
            stringResource(R.string.editor_summary_home, name)
        } else {
            pluralStringResource(R.plurals.editor_summary_home_rooms, rooms, name, rooms)
        }
        PlaceKind.FLOOR -> if (rooms == 0) {
            stringResource(R.string.editor_summary_level, name, home)
        } else {
            pluralStringResource(R.plurals.editor_summary_level_rooms, rooms, name, home, rooms)
        }
        PlaceKind.ROOM -> stringResource(R.string.editor_summary_room, name, places.floor(form.floorId)?.name ?: home)
    }
}
