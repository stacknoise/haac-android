package com.stacknoise.haac.core.database.schedule

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Access to the `schedule` table (concept 12, 19.7). */
@Dao
interface ScheduleDao {
    /** Every cached schedule of instance [serverId]. */
    @Query("SELECT * FROM schedule WHERE server_id = :serverId")
    suspend fun all(serverId: String): List<ScheduleEntity>

    /** The same rows as [all], sorted by name and updated on every change. */
    @Query("SELECT * FROM schedule WHERE server_id = :serverId ORDER BY name COLLATE NOCASE, schedule_id")
    fun observe(serverId: String): Flow<List<ScheduleEntity>>

    /** One cached schedule, updated on every change; null once it is gone. */
    @Query("SELECT * FROM schedule WHERE server_id = :serverId AND schedule_id = :scheduleId")
    fun observe(serverId: String, scheduleId: String): Flow<ScheduleEntity?>

    /** Inserts new rows and replaces existing ones. */
    @Upsert
    suspend fun upsert(rows: List<ScheduleEntity>)

    /** Deletes the schedules [scheduleIds] of instance [serverId]. */
    @Query("DELETE FROM schedule WHERE server_id = :serverId AND schedule_id IN (:scheduleIds)")
    suspend fun delete(serverId: String, scheduleIds: List<String>)

    /** Writes the result of a sync in one transaction. */
    @Transaction
    suspend fun applySync(serverId: String, rows: List<ScheduleEntity>, removed: List<String>) {
        if (removed.isNotEmpty()) delete(serverId, removed)
        upsert(rows)
    }
}
