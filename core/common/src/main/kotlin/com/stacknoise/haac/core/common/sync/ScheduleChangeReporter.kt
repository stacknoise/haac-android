package com.stacknoise.haac.core.common.sync

/**
 * Receives the changes of the user's own schedules a sync found and turns them into entries of the notification
 * list (concept 19.7). Implemented by `:feature:notifications`, called by the sync in `:feature:schedules`.
 */
interface ScheduleChangeReporter {
    /** Records that the schedules named [names] of instance [serverId] no longer exist on the server (HAAC-SCH-001). */
    suspend fun removed(serverId: String, names: List<String>)

    /** Records that the server paused schedule [name] of instance [serverId] for [reason] (HAAC-SCH-002). */
    suspend fun paused(serverId: String, name: String, reason: String)

    /**
     * Records the runs of own schedules that instance [serverId] announced since the last sync and moves the run
     * log cursor to [logId] and [lastSeq], in one transaction (concept 19.11). [runs] holds only entries with
     * `notify = true`; it is empty when only the cursor is set (first sync of the instance, new `log_id`).
     */
    suspend fun ran(serverId: String, logId: String, lastSeq: Long, runs: List<ScheduleRun>)
}
