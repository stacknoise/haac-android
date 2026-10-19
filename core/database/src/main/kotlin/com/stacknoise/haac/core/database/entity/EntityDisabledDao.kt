package com.stacknoise.haac.core.database.entity

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

/** Access to the deactivation columns of `exposed_entity` (concept 7.5, 12). */
@Dao
interface EntityDisabledDao {
    /** Marks [ids] of instance [id] deactivated since [at] (7.5); a time already set stays. */
    @Query(MarkDisabledSql)
    suspend fun markDisabled(id: String, ids: List<String>, at: Long)

    /** Clears the deactivation of every entity of instance [id] except [keep]. */
    @Query(ClearDisabledSql)
    suspend fun clearDisabledExcept(id: String, keep: List<String>)

    /** Makes [disabledIds] the deactivated entities of [serverId], the bridge's full list (concept 7.5, 11.2). */
    @Transaction
    suspend fun applyDisabled(serverId: String, disabledIds: List<String>, at: Long) {
        clearDisabledExcept(serverId, disabledIds)
        if (disabledIds.isNotEmpty()) markDisabled(serverId, disabledIds, at)
    }
}

/** Sets `disabled`; the time of a row that is already deactivated stays. */
private const val MarkDisabledSql = "UPDATE exposed_entity SET disabled = 1, disabled_at = IFNULL(disabled_at, :at) " +
    "WHERE server_id = :id AND entity_id IN (:ids)"

/** Clears `disabled` of every row of the instance that is not in `keep`. */
private const val ClearDisabledSql = "UPDATE exposed_entity SET disabled = 0, disabled_at = NULL " +
    "WHERE server_id = :id AND entity_id NOT IN (:keep)"
