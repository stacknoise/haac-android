package com.stacknoise.haac.core.database.assignment

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** The assignments in visible rooms of an instance: neither the room nor its home is deleted. */
private const val ObserveAllQuery = "SELECT room_entity.* FROM room_entity " +
    "JOIN room ON room.id = room_entity.room_id JOIN home ON home.id = room.home_id " +
    "WHERE home.server_id = :serverId AND room.deleted_at IS NULL AND home.deleted_at IS NULL"

/** Removes entities from every room of an instance. */
private const val RemoveEverywhereQuery = "DELETE FROM room_entity WHERE entity_id IN (:entityIds) AND room_id IN " +
    "(SELECT room.id FROM room JOIN home ON home.id = room.home_id WHERE home.server_id = :serverId)"

/** 1 if a room and its home exist and are not deleted. */
private const val RoomActiveQuery = "SELECT COUNT(*) FROM room JOIN home ON home.id = room.home_id " +
    "WHERE room.id = :roomId AND room.deleted_at IS NULL AND home.deleted_at IS NULL"

/** Sets order and size of one assignment. */
private const val ArrangeQuery = "UPDATE room_entity SET sort_order = :sortOrder, tile_size = :tileSize " +
    "WHERE room_id = :roomId AND entity_id = :entityId"

/** Access to the `room_entity` table (concept 7.2, 12). */
@Dao
interface RoomAssignmentDao {
    /** The entities of room [roomId] in their sort order. */
    @Query("SELECT * FROM room_entity WHERE room_id = :roomId ORDER BY sort_order")
    fun observe(roomId: String): Flow<List<RoomAssignment>>

    /** Every assignment in the visible rooms of instance [serverId] ("in Kitchen" hints, concept 7.1). */
    @Query(ObserveAllQuery)
    fun observeAll(serverId: String): Flow<List<RoomAssignment>>

    /** Adds assignments; an entity already in the room keeps its place (concept 7.2: once per room). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rows: List<RoomAssignment>)

    /** The highest sort order in room [roomId], or null for an empty room. */
    @Query("SELECT MAX(sort_order) FROM room_entity WHERE room_id = :roomId")
    suspend fun maxSortOrder(roomId: String): Int?

    /** 1 if room [roomId] and its home exist and are not deleted, else 0. */
    @Query(RoomActiveQuery)
    suspend fun roomIsActive(roomId: String): Int

    /** Removes entity [entityId] from room [roomId] (concept 7.2: only the local assignment). */
    @Query("DELETE FROM room_entity WHERE room_id = :roomId AND entity_id = :entityId")
    suspend fun remove(roomId: String, entityId: String)

    /** Sets [sortOrder] and [tileSize] of [entityId] in room [roomId] (edit layout, M-06, M-07). */
    @Query(ArrangeQuery)
    suspend fun arrange(roomId: String, entityId: String, sortOrder: Int, tileSize: TileSize)

    /** Removes [entityIds] from every room of instance [serverId] (*Remove from all rooms*, concept 7.4). */
    @Query(RemoveEverywhereQuery)
    suspend fun removeEverywhere(serverId: String, entityIds: List<String>)
}
