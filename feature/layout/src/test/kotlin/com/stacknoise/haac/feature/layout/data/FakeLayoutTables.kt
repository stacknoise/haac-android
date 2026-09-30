package com.stacknoise.haac.feature.layout.data

import com.stacknoise.haac.core.database.DatabaseTransactions
import com.stacknoise.haac.core.database.layout.FloorDao
import com.stacknoise.haac.core.database.layout.FloorEntity
import com.stacknoise.haac.core.database.layout.HomeDao
import com.stacknoise.haac.core.database.layout.HomeEntity
import com.stacknoise.haac.core.database.layout.LayoutTrashDao
import com.stacknoise.haac.core.database.layout.RoomDao
import com.stacknoise.haac.core.database.layout.RoomEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * The tables `home`, `floor` and `room` in memory with the filters of the DAO queries, the foreign keys (cascade,
 * set null) and a transaction that rolls back on failure.
 */
class FakeLayoutTables {
    val homes = MutableStateFlow<List<HomeEntity>>(emptyList())
    val floors = MutableStateFlow<List<FloorEntity>>(emptyList())
    val rooms = MutableStateFlow<List<RoomEntity>>(emptyList())

    private fun visibleHome(id: String) = homes.value.any { it.id == id && it.deletedAt == null }

    val homeDao = object : HomeDao {
        override fun observe(serverId: String): Flow<List<HomeEntity>> = homes.map { list ->
            list.filter { it.serverId == serverId && it.deletedAt == null }.sortedBy { it.sortOrder }
        }

        override suspend fun insert(home: HomeEntity) {
            homes.value += home
        }

        override suspend fun update(id: String, name: String, icon: String?): Int =
            updateHome(id) { it.copy(name = name, icon = icon) }

        override suspend fun maxSortOrder(serverId: String) =
            homes.value.filter { it.serverId == serverId }.maxOfOrNull { it.sortOrder }

        override suspend fun isActive(id: String) = if (visibleHome(id)) 1 else 0

        override suspend fun markDeleted(id: String, at: Long) {
            updateHome(id) { it.copy(deletedAt = at) }
        }
    }

    val floorDao = object : FloorDao {
        override fun observe(serverId: String): Flow<List<FloorEntity>> = combine(homes, floors) { _, list ->
            list.filter { floor -> floor.deletedAt == null && homeOfServer(floor.homeId, serverId) }
                .sortedWith(compareBy({ it.level }, { it.sortOrder }))
        }

        override suspend fun insert(floor: FloorEntity) {
            floors.value += floor
        }

        override suspend fun update(id: String, name: String, level: Int, icon: String?): Int =
            updateFloors({ it.id == id }) { it.copy(name = name, level = level, icon = icon) }

        override suspend fun maxSortOrder(homeId: String) =
            floors.value.filter { it.homeId == homeId }.maxOfOrNull { it.sortOrder }

        override suspend fun homeOf(id: String) =
            floors.value.firstOrNull { it.id == id && it.deletedAt == null }?.homeId

        override suspend fun markDeleted(id: String, at: Long) {
            updateFloors({ it.id == id }) { it.copy(deletedAt = at) }
        }

        override suspend fun markDeletedOfHome(homeId: String, at: Long) {
            updateFloors({ it.homeId == homeId }) { it.copy(deletedAt = at) }
        }
    }

    val roomDao = object : RoomDao {
        override fun observe(serverId: String): Flow<List<RoomEntity>> = combine(homes, rooms) { _, list ->
            list.filter { room -> room.deletedAt == null && homeOfServer(room.homeId, serverId) }
                .sortedBy { it.sortOrder }
        }

        override suspend fun insert(room: RoomEntity) {
            checkFloor(room)
            rooms.value += room
        }

        override suspend fun update(id: String, name: String, homeId: String, floorId: String?, icon: String?): Int =
            updateRooms({ it.id == id }) { it.copy(name = name, homeId = homeId, floorId = floorId, icon = icon) }

        override suspend fun maxSortOrder(homeId: String) =
            rooms.value.filter { it.homeId == homeId }.maxOfOrNull { it.sortOrder }

        override suspend fun link(ids: List<String>, homeId: String, floorId: String?) {
            rooms.value = rooms.value.map { if (it.id in ids) it.copy(homeId = homeId, floorId = floorId) else it }
            rooms.value.forEach(::checkFloor)
        }

        override suspend fun releaseFloor(floorId: String, keep: List<String>) {
            rooms.value = rooms.value.map { room ->
                if (room.floorId == floorId && room.id !in keep) room.copy(floorId = null) else room
            }
        }

        override suspend fun markDeleted(id: String, at: Long) {
            updateRooms({ it.id == id }) { it.copy(deletedAt = at) }
        }

        override suspend fun markDeletedOfHome(homeId: String, at: Long) {
            updateRooms({ it.homeId == homeId }) { it.copy(deletedAt = at) }
        }

        override suspend fun markDeletedOfFloor(floorId: String, at: Long) {
            updateRooms({ it.floorId == floorId }) { it.copy(deletedAt = at) }
        }
    }

    val trashDao = object : LayoutTrashDao {
        override suspend fun restoreHomes(at: Long) {
            homes.value = homes.value.map { if (it.deletedAt == at) it.copy(deletedAt = null) else it }
        }

        override suspend fun restoreFloors(at: Long) {
            floors.value = floors.value.map { if (it.deletedAt == at) it.copy(deletedAt = null) else it }
        }

        override suspend fun restoreRooms(at: Long) {
            rooms.value = rooms.value.map { if (it.deletedAt == at) it.copy(deletedAt = null) else it }
        }

        override suspend fun purgeRooms(before: Long) {
            rooms.value = rooms.value.filterNot { (it.deletedAt ?: Long.MAX_VALUE) <= before }
        }

        override suspend fun purgeFloors(before: Long) {
            val gone = floors.value.filter { (it.deletedAt ?: Long.MAX_VALUE) <= before }.map { it.id }.toSet()
            floors.value = floors.value.filterNot { it.id in gone }
            rooms.value = rooms.value.map { if (it.floorId in gone) it.copy(floorId = null) else it }
        }

        override suspend fun purgeHomes(before: Long) {
            val gone = homes.value.filter { (it.deletedAt ?: Long.MAX_VALUE) <= before }.map { it.id }.toSet()
            homes.value = homes.value.filterNot { it.id in gone }
            floors.value = floors.value.filterNot { it.homeId in gone }
            rooms.value = rooms.value.filterNot { it.homeId in gone }
        }
    }

    /** Restores all three tables when the block throws, like a rolled back transaction. */
    val transactions = object : DatabaseTransactions {
        @Suppress("TooGenericExceptionCaught") // Any failure rolls back, as in SQLite.
        override suspend fun <T> run(block: suspend () -> T): T {
            val saved = Triple(homes.value, floors.value, rooms.value)
            return try {
                block()
            } catch (e: Exception) {
                homes.value = saved.first
                floors.value = saved.second
                rooms.value = saved.third
                throw e
            }
        }
    }

    val daos = LayoutDaos(homeDao, floorDao, roomDao)

    private fun homeOfServer(homeId: String, serverId: String) =
        homes.value.any { it.id == homeId && it.serverId == serverId && it.deletedAt == null }

    private fun updateHome(id: String, change: (HomeEntity) -> HomeEntity): Int {
        val hit = homes.value.count { it.id == id && it.deletedAt == null }
        homes.value = homes.value.map { if (it.id == id && it.deletedAt == null) change(it) else it }
        return hit
    }

    private fun updateFloors(match: (FloorEntity) -> Boolean, change: (FloorEntity) -> FloorEntity): Int {
        val hit = floors.value.count { match(it) && it.deletedAt == null }
        floors.value = floors.value.map { if (match(it) && it.deletedAt == null) change(it) else it }
        return hit
    }

    private fun updateRooms(match: (RoomEntity) -> Boolean, change: (RoomEntity) -> RoomEntity): Int {
        val hit = rooms.value.count { match(it) && it.deletedAt == null }
        rooms.value = rooms.value.map { if (match(it) && it.deletedAt == null) change(it) else it }
        rooms.value.forEach(::checkFloor)
        return hit
    }

    /** The trigger of concept 6.1: a room's floor must belong to its home. */
    private fun checkFloor(room: RoomEntity) {
        val floor = room.floorId ?: return
        check(floors.value.first { it.id == floor }.homeId == room.homeId) { "floor of another home" }
    }
}
