package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.layout.Floor
import com.stacknoise.haac.core.database.layout.Home
import com.stacknoise.haac.core.database.layout.Places
import com.stacknoise.haac.core.database.layout.Room

/** A level of a home, or the rooms of a home without a level ([floor] null), with its rooms (M-05 chips). */
data class RoomGroup(val home: Home, val floor: Floor?, val rooms: List<Room>)

/**
 * The groups the room header offers, in display order: per home its levels, then its rooms without a level
 * (concept 6.1). Groups without rooms are left out.
 */
fun Places.roomGroups(): List<RoomGroup> = homes.flatMap { home ->
    floorsOf(home.id).map { RoomGroup(home, it, roomsOn(it.id)) } + RoomGroup(home, null, roomsWithoutFloor(home.id))
}.filter { it.rooms.isNotEmpty() }

/** The group that holds room [roomId], or null. */
fun List<RoomGroup>.groupOf(roomId: String?): RoomGroup? = firstOrNull { group -> group.rooms.any { it.id == roomId } }

/** Room [selected] if it still exists, else the first room of the first group. */
fun List<RoomGroup>.roomOrFirst(selected: String?): Room? =
    groupOf(selected)?.rooms?.first { it.id == selected } ?: firstOrNull()?.rooms?.firstOrNull()
