package com.stacknoise.haac.feature.layout.domain

/** Filter chips of the Places overview (M-02). */
enum class PlaceFilter {
    ALL,
    HOMES,
    LEVELS,
    ROOMS,
}

/** What the right side of a row in the Places overview says (M-02). */
sealed interface Relation {
    /** A home with [count] levels. */
    data class Levels(val count: Int) : Relation

    /** A home without levels and [count] rooms. */
    data class Rooms(val count: Int) : Relation

    /** A level in a home, or a room on a level or directly in a home: the name of that place. */
    data class In(val name: String) : Relation
}

/** One row of the Places overview. */
data class PlaceRow(val kind: PlaceKind, val id: String, val name: String, val relation: Relation)

/**
 * The rows of the Places overview for [filter]: homes, then levels, then rooms (M-02). Levels and rooms are
 * grouped by home; the rooms of a home follow its levels, rooms without a level come last.
 */
fun Places.rows(filter: PlaceFilter): List<PlaceRow> = buildList {
    if (filter == PlaceFilter.ALL || filter == PlaceFilter.HOMES) {
        homes.forEach { add(PlaceRow(PlaceKind.HOME, it.id, it.name, homeRelation(it))) }
    }
    if (filter == PlaceFilter.ALL || filter == PlaceFilter.LEVELS) {
        homes.flatMap { floorsOf(it.id) }
            .forEach { add(PlaceRow(PlaceKind.FLOOR, it.id, it.name, Relation.In(home(it.homeId)?.name.orEmpty()))) }
    }
    if (filter == PlaceFilter.ALL || filter == PlaceFilter.ROOMS) {
        homesRooms().forEach { add(PlaceRow(PlaceKind.ROOM, it.id, it.name, roomRelation(it))) }
    }
}

/** Number of places the chip [filter] shows. */
fun Places.count(filter: PlaceFilter): Int = when (filter) {
    PlaceFilter.ALL -> homes.size + floors.size + rooms.size
    PlaceFilter.HOMES -> homes.size
    PlaceFilter.LEVELS -> floors.size
    PlaceFilter.ROOMS -> rooms.size
}

/** "2 levels", or "3 rooms" for a home without levels. */
private fun Places.homeRelation(home: Home): Relation {
    val levels = floorsOf(home.id).size
    return if (levels > 0) Relation.Levels(levels) else Relation.Rooms(roomsOf(home.id).size)
}

/** The level of a room, or its home when it has none. */
private fun Places.roomRelation(room: Room): Relation =
    Relation.In(floor(room.floorId)?.name ?: home(room.homeId)?.name.orEmpty())

/** Every room in display order: by home, by level, then the rooms without a level. */
private fun Places.homesRooms(): List<Room> = homes.flatMap { home ->
    floorsOf(home.id).flatMap { roomsOn(it.id) } + roomsWithoutFloor(home.id)
}
