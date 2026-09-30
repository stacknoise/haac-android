package com.stacknoise.haac.feature.layout.domain

import com.stacknoise.haac.core.database.layout.Home
import com.stacknoise.haac.core.database.layout.Places
import com.stacknoise.haac.core.database.layout.Room

/**
 * The input of the form for a new or existing home, level or room (M-03, concept 6.1, 6.2).
 *
 * - Home: [roomIds] are rooms of other homes that move into it (they lose their level).
 * - Level: [homeId] is required; [roomIds] are the rooms of that home on the level, [level] sorts the levels.
 * - Room: [homeId] is required, [floorId] is a level of that home or null (directly in the home).
 *
 * [newRooms] are names of rooms the form creates in the home, on the level for a level form. [icon] is the key of
 * a [com.stacknoise.haac.core.common.ui.PlaceIcon] or null for none.
 */
data class PlaceForm(
    val kind: PlaceKind,
    val id: String? = null,
    val name: String = "",
    val homeId: String? = null,
    val floorId: String? = null,
    val level: Int = 0,
    val roomIds: Set<String> = emptySet(),
    val newRooms: List<String> = emptyList(),
    val icon: String? = null,
) {
    /** True while the form creates a place instead of editing one. */
    val isNew: Boolean get() = id == null

    /** Name set and, for levels and rooms, a home chosen (concept 6.1: a home is mandatory). */
    val canSave: Boolean get() = name.isNotBlank() && (kind == PlaceKind.HOME || homeId != null)

    /**
     * Chooses home [homeId]. A room keeps its level only if it belongs to the new home; a new level starts
     * without rooms and above the highest level of the home.
     */
    fun withHome(homeId: String, places: Places): PlaceForm = when {
        homeId == this.homeId -> this
        kind == PlaceKind.ROOM ->
            copy(homeId = homeId, floorId = floorId?.takeIf { places.floor(it)?.homeId == homeId })
        kind == PlaceKind.FLOOR -> copy(homeId = homeId, roomIds = emptySet(), level = nextLevel(places, homeId))
        else -> this
    }

    /** Checks or unchecks room [roomId]. */
    fun toggleRoom(roomId: String): PlaceForm =
        copy(roomIds = if (roomId in roomIds) roomIds - roomId else roomIds + roomId)

    /** Adds a room [name] the form creates; a blank name adds nothing. */
    fun addNewRoom(name: String): PlaceForm =
        if (name.isBlank()) this else copy(newRooms = newRooms + name.trim())

    /** Removes the new room at [index]. */
    fun removeNewRoom(index: Int): PlaceForm = copy(newRooms = newRooms.filterIndexed { i, _ -> i != index })

    /** Forms for new and existing places. */
    companion object {
        /** An empty form for a new place of [kind]; the home is preselected when there is only one (concept 6.1). */
        fun create(kind: PlaceKind, places: Places): PlaceForm {
            val home = if (kind == PlaceKind.HOME) null else places.homes.singleOrNull()?.id
            return PlaceForm(kind = kind, homeId = home, level = home?.let { nextLevel(places, it) } ?: 0)
        }

        /** The form of the existing place [id] of [kind], or null if it no longer exists. */
        fun edit(kind: PlaceKind, id: String, places: Places): PlaceForm? = when (kind) {
            PlaceKind.HOME -> places.home(id)?.let { PlaceForm(kind, id, it.name, icon = it.icon) }
            PlaceKind.FLOOR -> places.floor(id)?.let { floor ->
                val onFloor = places.roomsOn(id).map { it.id }.toSet()
                PlaceForm(kind, id, floor.name, floor.homeId, level = floor.level, roomIds = onFloor, icon = floor.icon)
            }
            PlaceKind.ROOM -> places.room(id)?.let {
                PlaceForm(kind, id, it.name, it.homeId, it.floorId, icon = it.icon)
            }
        }

        /** One above the highest level of home [homeId], or 0 for its first level. */
        private fun nextLevel(places: Places, homeId: String): Int =
            places.floorsOf(homeId).maxOfOrNull { it.level + 1 } ?: 0
    }
}
