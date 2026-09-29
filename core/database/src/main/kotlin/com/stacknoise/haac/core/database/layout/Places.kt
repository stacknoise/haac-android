package com.stacknoise.haac.core.database.layout

/** A home of the active instance (concept 6.1). */
data class Home(val id: String, val name: String)

/** A floor of home [homeId]; [level] sorts the floors of a home. */
data class Floor(val id: String, val homeId: String, val name: String, val level: Int)

/** A room of home [homeId], on floor [floorId] or directly in the home (null). */
data class Room(val id: String, val homeId: String, val floorId: String?, val name: String)

/**
 * Homes, floors and rooms of the active instance in their display order (concept 6.1); rows whose deletion can
 * still be undone are not included.
 */
data class Places(
    val homes: List<Home> = emptyList(),
    val floors: List<Floor> = emptyList(),
    val rooms: List<Room> = emptyList(),
) {
    /** Home [id], or null. */
    fun home(id: String?): Home? = homes.firstOrNull { it.id == id }

    /** Floor [id], or null. */
    fun floor(id: String?): Floor? = floors.firstOrNull { it.id == id }

    /** Room [id], or null. */
    fun room(id: String?): Room? = rooms.firstOrNull { it.id == id }

    /** The floors of home [homeId] by level. */
    fun floorsOf(homeId: String?): List<Floor> = floors.filter { it.homeId == homeId }

    /** Every room of home [homeId], with or without a floor. */
    fun roomsOf(homeId: String?): List<Room> = rooms.filter { it.homeId == homeId }

    /** The rooms on floor [floorId]. */
    fun roomsOn(floorId: String): List<Room> = rooms.filter { it.floorId == floorId }

    /** The rooms of home [homeId] without a floor ("Other rooms", concept 6.1). */
    fun roomsWithoutFloor(homeId: String): List<Room> = rooms.filter { it.homeId == homeId && it.floorId == null }
}
