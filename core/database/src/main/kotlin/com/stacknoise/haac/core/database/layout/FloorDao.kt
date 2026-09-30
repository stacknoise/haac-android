package com.stacknoise.haac.core.database.layout

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** The visible floors of an instance: neither they nor their home are deleted. */
private const val ObserveQuery = "SELECT floor.* FROM floor JOIN home ON home.id = floor.home_id " +
    "WHERE home.server_id = :serverId AND floor.deleted_at IS NULL AND home.deleted_at IS NULL " +
    "ORDER BY floor.level, floor.sort_order"

/** Access to the `floor` table (concept 6, 12); reads skip rows whose deletion is pending. */
@Dao
interface FloorDao {
    /** The floors of every home of instance [serverId], by level. */
    @Query(ObserveQuery)
    fun observe(serverId: String): Flow<List<FloorEntity>>

    /** Adds a floor. */
    @Insert
    suspend fun insert(floor: FloorEntity)

    /** Changes name, level and icon of floor [id]; returns 0 if it does not exist or is deleted. */
    @Query("UPDATE floor SET name = :name, level = :level, icon = :icon WHERE id = :id AND deleted_at IS NULL")
    suspend fun update(id: String, name: String, level: Int, icon: String?): Int

    /** The highest sort order of the floors of [homeId], or null without floors. */
    @Query("SELECT MAX(sort_order) FROM floor WHERE home_id = :homeId")
    suspend fun maxSortOrder(homeId: String): Int?

    /** The home of floor [id], or null if it does not exist or is deleted. */
    @Query("SELECT home_id FROM floor WHERE id = :id AND deleted_at IS NULL")
    suspend fun homeOf(id: String): String?

    /** Marks floor [id] deleted at [at]. */
    @Query("UPDATE floor SET deleted_at = :at WHERE id = :id AND deleted_at IS NULL")
    suspend fun markDeleted(id: String, at: Long)

    /** Marks every floor of home [homeId] deleted at [at]. */
    @Query("UPDATE floor SET deleted_at = :at WHERE home_id = :homeId AND deleted_at IS NULL")
    suspend fun markDeletedOfHome(homeId: String, at: Long)
}
