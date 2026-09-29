package com.stacknoise.haac.app.navigation

import android.net.Uri
import com.stacknoise.haac.feature.entities.ui.AddEntitiesViewModel
import com.stacknoise.haac.feature.entities.ui.AssignViewModel
import com.stacknoise.haac.feature.entities.ui.EditLayoutViewModel
import com.stacknoise.haac.feature.entities.ui.EntityDetailViewModel

/** Routes of the entity screens inside the main area (M-04, detail screen, M-09 actions). */
object EntityRoutes {
    /** The detail screen of one entity (concept 8, 15.4). */
    const val DETAIL = "entities/detail/{${EntityDetailViewModel.ENTITY_ARG}}"

    /** *Add entities* for one room. */
    const val ADD = "entities/add/{${AddEntitiesViewModel.ROOM_ARG}}"

    /** *Add to room* for entities of a notification entry. */
    const val ASSIGN = "entities/assign?${AssignViewModel.IDS_ARG}={${AssignViewModel.IDS_ARG}}"

    /** The edit layout of one room (M-06, M-07). */
    const val EDIT = "entities/edit/{${EditLayoutViewModel.ROOM_ARG}}"

    /** The edit layout of room [roomId]. */
    fun edit(roomId: String): String = "entities/edit/${Uri.encode(roomId)}"

    /** The detail screen of [entityId]. */
    fun detail(entityId: String): String = "entities/detail/${Uri.encode(entityId)}"

    /** *Add entities* for room [roomId]. */
    fun add(roomId: String): String = "entities/add/${Uri.encode(roomId)}"

    /** *Add to room* for [entityIds]. */
    fun assign(entityIds: List<String>): String =
        "entities/assign?${AssignViewModel.IDS_ARG}=${Uri.encode(entityIds.joinToString(","))}"
}
