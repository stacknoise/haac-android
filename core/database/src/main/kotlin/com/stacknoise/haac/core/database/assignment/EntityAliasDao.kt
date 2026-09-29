package com.stacknoise.haac.core.database.assignment

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Access to the `entity_alias` table (concept 7.3). */
@Dao
interface EntityAliasDao {
    /** The local names of instance [serverId]. */
    @Query("SELECT * FROM entity_alias WHERE server_id = :serverId")
    fun observe(serverId: String): Flow<List<EntityAlias>>

    /** Sets or replaces a local name. */
    @Upsert
    suspend fun set(alias: EntityAlias)

    /** Removes the local name of [entityId] (*Use default name*). */
    @Query("DELETE FROM entity_alias WHERE server_id = :serverId AND entity_id = :entityId")
    suspend fun clear(serverId: String, entityId: String)
}
