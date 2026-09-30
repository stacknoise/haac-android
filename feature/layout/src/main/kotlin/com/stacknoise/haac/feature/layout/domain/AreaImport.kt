package com.stacknoise.haac.feature.layout.domain

import com.stacknoise.haac.core.database.layout.Places

/** A floor of Home Assistant that holds areas with exposed entities (concept 6.3). */
data class HaFloor(val id: String, val name: String, val level: Int?)

/** An area of Home Assistant with [entityCount] exposed entities; [floorId] is null without a floor. */
data class HaArea(val id: String, val name: String, val floorId: String?, val entityCount: Int)

/** The floors and areas `haac_bridge/areas` returned, floors by level and areas by name. */
data class HaAreas(val floors: List<HaFloor>, val areas: List<HaArea>) {
    /** The areas on floor [floorId]; null gives the areas without a floor (or on a floor that is not listed). */
    fun areasOn(floorId: String?): List<HaArea> =
        areas.filter { area -> (area.floorId?.takeIf { id -> floors.any { it.id == id } }) == floorId }
}

/** A level the import creates, or reuses when [existingId] is set, with the rooms it adds. */
data class PlannedFloor(val name: String, val level: Int, val existingId: String?, val rooms: List<String>)

/** What an import adds to a home: [floors] with their rooms, [looseRooms] directly in the home. */
data class ImportPlan(
    val floors: List<PlannedFloor> = emptyList(),
    val looseRooms: List<String> = emptyList(),
    val skipped: Int = 0,
) {
    /** Levels that are new. */
    val newLevels: Int get() = floors.count { it.existingId == null }

    /** Rooms that are new. */
    val newRooms: Int get() = floors.sumOf { it.rooms.size } + looseRooms.size

    /** True if the import would not add anything. */
    val isEmpty: Boolean get() = newRooms == 0
}

/** How many levels and rooms an import created. */
data class ImportResult(val levels: Int, val rooms: Int)

/**
 * The plan for importing the areas [selected] into home [homeId] (null: a new home, so nothing exists yet).
 * Areas whose name a room of the home already has are skipped, and a level with the name of an imported floor is
 * reused; names compare without regard to case (concept 6.3).
 */
fun HaAreas.plan(selected: Set<String>, places: Places, homeId: String?): ImportPlan {
    val existingRooms = homeId?.let { places.roomsOf(it) }.orEmpty().map { it.name.trim().lowercase() }.toSet()
    val chosen = areas.filter { it.id in selected }
    val (kept, dropped) = chosen.partition { it.name.trim().lowercase() !in existingRooms }
    val keptIds = kept.mapTo(mutableSetOf()) { it.id }
    val floorsPlan = floors.mapNotNull { floor ->
        val rooms = areasOn(floor.id).filter { it.id in keptIds }.map { it.name.trim() }
        val existing = homeId?.let { places.floorsOf(it) }.orEmpty()
            .firstOrNull { it.name.trim().equals(floor.name.trim(), ignoreCase = true) }
        if (rooms.isEmpty()) null else PlannedFloor(floor.name.trim(), floor.level ?: 0, existing?.id, rooms)
    }
    val loose = areasOn(null).filter { it.id in keptIds }.map { it.name.trim() }
    return ImportPlan(floorsPlan, loose, dropped.size)
}
