package com.stacknoise.haac.core.database.server

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Access to the `server` table (concept 12). */
@Dao
interface ServerDao {
    /** Adds a new instance. */
    @Insert
    suspend fun insert(server: ServerEntity)

    /** Replaces the row with the same id. */
    @Update
    suspend fun update(server: ServerEntity)

    /** The instance with [id], or null. */
    @Query("SELECT * FROM server WHERE id = :id")
    suspend fun get(id: String): ServerEntity?

    /** The instance used last, or null if there is none. */
    @Query("SELECT * FROM server ORDER BY last_active_at DESC LIMIT 1")
    suspend fun mostRecent(): ServerEntity?

    /** The instance with [id] while it exists. */
    @Query("SELECT * FROM server WHERE id = :id")
    fun observe(id: String): Flow<ServerEntity?>

    /** Number of instances; used to pick the accent colour of a new one. */
    @Query("SELECT COUNT(*) FROM server")
    suspend fun count(): Int
}
