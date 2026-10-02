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
}
