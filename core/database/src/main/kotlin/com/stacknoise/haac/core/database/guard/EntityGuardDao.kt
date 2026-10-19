package com.stacknoise.haac.core.database.guard

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Access to the `entity_guard` table (concept 8.5, 12). */
@Dao
interface EntityGuardDao {
    /** The guards of instance [serverId], updated on every change. */
    @Query("SELECT * FROM entity_guard WHERE server_id = :serverId")
    fun observe(serverId: String): Flow<List<EntityGuard>>

    /** Turns the guard of an entity on or replaces its directions. */
    @Upsert
    suspend fun set(guard: EntityGuard)

    /** Turns the guard of [entityId] off by removing its row. */
    @Query("DELETE FROM entity_guard WHERE server_id = :serverId AND entity_id = :entityId")
    suspend fun clear(serverId: String, entityId: String)
}
