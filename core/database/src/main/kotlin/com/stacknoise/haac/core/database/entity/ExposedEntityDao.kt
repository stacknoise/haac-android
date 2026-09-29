package com.stacknoise.haac.core.database.entity

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Access to the `exposed_entity` table and the sync columns of `server` (concept 9.1, 12). */
@Dao
interface ExposedEntityDao {
    /** Every cached entity of instance [serverId], active and withdrawn. */
    @Query("SELECT * FROM exposed_entity WHERE server_id = :serverId")
    suspend fun all(serverId: String): List<ExposedEntity>

    /** The same rows as [all], updated on every change (live states for the room grid, concept 8.1). */
    @Query("SELECT * FROM exposed_entity WHERE server_id = :serverId")
    fun observe(serverId: String): Flow<List<ExposedEntity>>

    /** Inserts new rows and replaces existing ones. */
    @Upsert
    suspend fun upsert(rows: List<ExposedEntity>)

    /** The exposure revision of the last sync of instance [serverId], or null before the first one. */
    @Query("SELECT exposure_revision FROM server WHERE id = :serverId")
    suspend fun revision(serverId: String): String?

    /** Stores [revision] and the time [at] of a finished sync (concept 9.1 step 6). */
    @Query("UPDATE server SET exposure_revision = :revision, last_sync_at = :at WHERE id = :serverId")
    suspend fun markSynced(serverId: String, revision: String, at: Long)

    /** Writes the rows of a sync and its revision in one transaction. */
    @Transaction
    suspend fun applySync(serverId: String, rows: List<ExposedEntity>, revision: String, at: Long) {
        upsert(rows)
        markSynced(serverId, revision, at)
    }

    /** The last known state of [entityId] as JSON, or null. */
    @Query("SELECT last_state FROM exposed_entity WHERE server_id = :serverId AND entity_id = :entityId")
    suspend fun state(serverId: String, entityId: String): String?

    /** Replaces the last known state of [entityId]; an entity that is not cached is ignored. */
    @Query("UPDATE exposed_entity SET last_state = :state WHERE server_id = :serverId AND entity_id = :entityId")
    suspend fun setState(serverId: String, entityId: String, state: String)

    /** Replaces the states of several entities in one transaction (one subscription event). */
    @Transaction
    suspend fun setStates(serverId: String, states: Map<String, String>) {
        states.forEach { (entityId, state) -> setState(serverId, entityId, state) }
    }
}
