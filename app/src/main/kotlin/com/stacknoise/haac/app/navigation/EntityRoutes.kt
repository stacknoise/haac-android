package com.stacknoise.haac.app.navigation

import android.net.Uri
import com.stacknoise.haac.feature.entities.ui.AddEntitiesViewModel
import com.stacknoise.haac.feature.entities.ui.AssignViewModel

/** Routes of the entity screens inside the main area (M-04, M-09 actions). */
object EntityRoutes {
    /** *Add entities* for one room. */
    const val ADD = "entities/add/{${AddEntitiesViewModel.ROOM_ARG}}"

    /** *Add to room* for entities of a notification entry. */
    const val ASSIGN = "entities/assign?${AssignViewModel.IDS_ARG}={${AssignViewModel.IDS_ARG}}"

    /** *Add entities* for room [roomId]. */
    fun add(roomId: String): String = "entities/add/${Uri.encode(roomId)}"

    /** *Add to room* for [entityIds]. */
    fun assign(entityIds: List<String>): String =
        "entities/assign?${AssignViewModel.IDS_ARG}=${Uri.encode(entityIds.joinToString(","))}"
}
