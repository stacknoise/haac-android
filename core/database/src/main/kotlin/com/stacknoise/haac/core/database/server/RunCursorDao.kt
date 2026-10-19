package com.stacknoise.haac.core.database.server

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query

/** The position in the bridge's run log up to which the app has seen the entries (concept 19.11). */
data class RunCursor(
    @ColumnInfo(name = "schedule_run_log_id") val logId: String?,
    @ColumnInfo(name = "schedule_run_seq") val seq: Long?,
)

/** Access to the run log cursor in the `server` table (concept 19.11, 12). */
@Dao
interface RunCursorDao {
    /** The cursor of instance [id]; null if the instance does not exist. Both parts are null before the first sync. */
    @Query("SELECT schedule_run_log_id, schedule_run_seq FROM server WHERE id = :id")
    suspend fun runCursor(id: String): RunCursor?

    /** Stores the cursor in the bridge's run log of instance [id]. */
    @Query("UPDATE server SET schedule_run_log_id = :logId, schedule_run_seq = :seq WHERE id = :id")
    suspend fun setRunCursor(id: String, logId: String, seq: Long)
}
