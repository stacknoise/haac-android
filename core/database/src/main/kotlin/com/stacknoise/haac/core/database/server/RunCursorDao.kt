package com.stacknoise.haac.core.database.server

import androidx.room.Dao
import androidx.room.Query

/** Access to the run log cursor in the `server` table (concept 19.11, 12). */
@Dao
interface RunCursorDao {
    /** Stores the cursor in the bridge's run log of instance [id]. */
    @Query("UPDATE server SET schedule_run_log_id = :logId, schedule_run_seq = :seq WHERE id = :id")
    suspend fun setRunCursor(id: String, logId: String, seq: Long)
}
