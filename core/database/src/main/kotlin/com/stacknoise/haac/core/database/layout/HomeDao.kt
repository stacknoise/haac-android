package com.stacknoise.haac.core.database.layout

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Access to the `home` table (concept 6, 12); reads skip rows whose deletion is pending. */
@Dao
interface HomeDao {
    /** The homes of instance [serverId] in their sort order. */
    @Query("SELECT * FROM home WHERE server_id = :serverId AND deleted_at IS NULL ORDER BY sort_order")
    fun observe(serverId: String): Flow<List<HomeEntity>>

    /** Adds a home. */
    @Insert
    suspend fun insert(home: HomeEntity)

    /** Renames home [id]; returns 0 if it does not exist or is deleted. */
    @Query("UPDATE home SET name = :name WHERE id = :id AND deleted_at IS NULL")
    suspend fun rename(id: String, name: String): Int

    /** The highest sort order of the homes of [serverId], or null without homes. */
    @Query("SELECT MAX(sort_order) FROM home WHERE server_id = :serverId")
    suspend fun maxSortOrder(serverId: String): Int?

    /** 1 if home [id] exists and is not deleted, else 0. */
    @Query("SELECT COUNT(*) FROM home WHERE id = :id AND deleted_at IS NULL")
    suspend fun isActive(id: String): Int

    /** Marks home [id] deleted at [at] (undo window, concept 6.2). */
    @Query("UPDATE home SET deleted_at = :at WHERE id = :id AND deleted_at IS NULL")
    suspend fun markDeleted(id: String, at: Long)
}
