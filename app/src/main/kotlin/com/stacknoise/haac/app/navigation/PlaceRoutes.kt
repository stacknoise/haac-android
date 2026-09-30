package com.stacknoise.haac.app.navigation

import com.stacknoise.haac.feature.layout.domain.PlaceKind
import com.stacknoise.haac.feature.layout.ui.PlaceEditorViewModel

/** Routes of the place forms inside the main area (M-03). */
object PlaceRoutes {
    /** Form for a place of a kind; the optional id edits an existing place. */
    const val EDITOR = "place/{${PlaceEditorViewModel.KIND_ARG}}?${PlaceEditorViewModel.ID_ARG}=" +
        "{${PlaceEditorViewModel.ID_ARG}}"

    /** The import wizard for Home Assistant areas (concept 6.3). */
    const val IMPORT = "places/import"

    /** The form for place [id] of [kind], or for a new place when [id] is null. */
    fun editor(kind: PlaceKind, id: String?): String =
        if (id == null) "place/${kind.name}" else "place/${kind.name}?${PlaceEditorViewModel.ID_ARG}=$id"
}
