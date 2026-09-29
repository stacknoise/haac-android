package com.stacknoise.haac.core.database.layout

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** The visible rooms of an instance: neither they nor their home are deleted. */
private const val ObserveQuery = "SELECT room.* FROM room JOIN home ON home.id = room.home_id " +
    "WHERE home.server_id = :serverId AND room.deleted_at IS NULL AND home.deleted_at IS NULL " +
    "ORDER BY room.sort_order"

/** Moves and renames a room that is not deleted. */
private const val UpdateQuery = "UPDATE room SET name = :name, home_id = :homeId, floor_id = :floorId " +
    "WHERE id = :id AND deleted_at IS NULL"

/** Access to the `room` table (concept 6, 12); reads skip rows whose deletion is pending. */
@Dao
interface RoomDao {
    /** The rooms of every home of instance [serverId] in their sort order. */
    @Query(ObserveQuery)
    fun observe(serverId: String): Flow<List<RoomEntity>>

    /** Adds a room. */
    @Insert
    suspend fun insert(room: RoomEntity)

    /** Renames room [id] and links it to [homeId] and [floorId]; returns 0 if it does not exist or is deleted. */
    @Query(UpdateQuery)
    suspend fun update(id: String, name: String, homeId: String, floorId: String?): Int

    /** The highest sort order of the rooms of [homeId], or null without rooms. */
    @Query("SELECT MAX(sort_order) FROM room WHERE home_id = :homeId")
    suspend fun maxSortOrder(homeId: String): Int?

    /** Links rooms [ids] to [homeId] and [floorId] (null = directly in the home). */
    @Query("UPDATE room SET home_id = :homeId, floor_id = :floorId WHERE id IN (:ids)")
    suspend fun link(ids: List<String>, homeId: String, floorId: String?)

    /** Rooms on floor [floorId] that are not in [keep] lose their floor and stay directly in the home. */
    @Query("UPDATE room SET floor_id = NULL WHERE floor_id = :floorId AND id NOT IN (:keep)")
    suspend fun releaseFloor(floorId: String, keep: List<String>)

    /** Marks room [id] deleted at [at]. */
    @Query("UPDATE room SET deleted_at = :at WHERE id = :id AND deleted_at IS NULL")
    suspend fun markDeleted(id: String, at: Long)

    /** Marks every room of home [homeId] deleted at [at]. */
    @Query("UPDATE room SET deleted_at = :at WHERE home_id = :homeId AND deleted_at IS NULL")
    suspend fun markDeletedOfHome(homeId: String, at: Long)

    /** Marks every room on floor [floorId] deleted at [at]. */
    @Query("UPDATE room SET deleted_at = :at WHERE floor_id = :floorId AND deleted_at IS NULL")
    suspend fun markDeletedOfFloor(floorId: String, at: Long)
}
