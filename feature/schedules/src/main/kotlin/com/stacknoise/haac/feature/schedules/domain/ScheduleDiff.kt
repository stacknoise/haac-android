package com.stacknoise.haac.feature.schedules.domain

import com.stacknoise.haac.core.database.schedule.ScheduleEntity

/** A schedule of the user that the server newly paused, with the reason (concept 19.6). */
data class PausedSchedule(val name: String, val reason: String)

/**
 * What one schedule sync changes: the rows to write, the ids to delete and what the user is told about (concept
 * 19.7). Only the user's own schedules are announced; foreign ones, which admins see, change silently.
 */
data class ScheduleChange(
    val rows: List<ScheduleEntity>,
    val removedIds: List<String>,
    val removedNames: List<String>,
    val paused: List<PausedSchedule>,
)

/** The difference between the cached schedules and a fresh list, by schedule id (concept 19.7 step 2 and 3). */
object ScheduleDiff {
    /**
     * Compares [cached] with [listed]. Rows that did not change are not written again (their sync time aside).
     * Nothing is announced for an empty cache, so the first sync of an instance stays silent.
     */
    fun compute(cached: List<ScheduleEntity>, listed: List<ScheduleEntity>): ScheduleChange {
        val before = cached.associateBy { it.scheduleId }
        val after = listed.map { it.scheduleId }.toSet()
        val gone = cached.filter { it.scheduleId !in after }
        return ScheduleChange(
            rows = listed.filter { row -> before[row.scheduleId]?.let { changed(it, row) } ?: true },
            removedIds = gone.map { it.scheduleId },
            removedNames = gone.filter { it.own }.map { it.name },
            paused = listed.mapNotNull { row -> before[row.scheduleId]?.let { pausedNow(it, row) } },
        )
    }

    /** True if [row] differs from its cached version [old] in anything but the sync time. */
    private fun changed(old: ScheduleEntity, row: ScheduleEntity): Boolean = old != row.copy(syncedAt = old.syncedAt)

    /** The entry for [row] if it is the user's, paused now and was not paused with the same reason and time before. */
    private fun pausedNow(old: ScheduleEntity, row: ScheduleEntity): PausedSchedule? {
        val reason = row.pausedReason ?: return null
        val isNew = old.pausedReason != reason || old.pausedAt != row.pausedAt
        return PausedSchedule(row.name, reason).takeIf { row.own && isNew }
    }
}
