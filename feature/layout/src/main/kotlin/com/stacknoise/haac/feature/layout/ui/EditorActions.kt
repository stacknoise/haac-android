package com.stacknoise.haac.feature.layout.ui

import com.stacknoise.haac.feature.layout.domain.PlaceForm
import com.stacknoise.haac.core.database.layout.Places

/** Callbacks of the place form; [onChange] applies a change to the form with the current places. */
class EditorActions(
    val onClose: () -> Unit,
    val onChange: ((PlaceForm, Places) -> PlaceForm) -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
)

/** Callbacks of the room list of a home or level form. */
internal class RoomListActions(
    val onToggle: (String) -> Unit,
    val onAdd: (String) -> Unit,
    val onRemoveNew: (Int) -> Unit,
)
