package com.stacknoise.haac.feature.notifications.domain

import com.stacknoise.haac.core.common.sync.RunResult
import com.stacknoise.haac.core.database.notification.NotificationType
import com.stacknoise.haac.core.error.ErrorCode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** An entity named in an entry: its display name (configured name, else HA name, else id) and its id. */
data class EntityLabel(val entityId: String, val name: String)

/**
 * The run behind a [NotificationType.SCHEDULE_RUN] entry (concept 19.11): the schedule [scheduleId] (null if the
 * entry is damaged), the [result], whether the run [removed] a one-off or ending schedule and whether the schedule
 * still [exists] in the cache, so *Open schedule* works.
 */
data class RunInfo(val scheduleId: String?, val result: RunResult, val removed: Boolean, val exists: Boolean)

/**
 * One entry of the notification list as the screen shows it (concept 9.1, 17.4). [entities] are set for
 * [NotificationType.ADDED] and [NotificationType.REMOVED] (for the schedule types they hold the schedule names),
 * [error] for [NotificationType.ERROR] and the schedule types, [detail] is the pause reason of
 * [NotificationType.SCHEDULE_PAUSED] (concept 19.7), [run] the run of a [NotificationType.SCHEDULE_RUN] entry
 * (19.11). [unassigned] are the ids of an [NotificationType.ADDED] entry's entities that are still shared and in no
 * room; *Add to room* stays available while any are left.
 */
data class NotificationItem(
    val id: Long,
    val type: NotificationType,
    val createdAt: Long,
    val unread: Boolean,
    val resolved: Boolean,
    val count: Int = 1,
    val entities: List<EntityLabel> = emptyList(),
    val error: ErrorCode? = null,
    val bridgeCode: String? = null,
    val serverId: String? = null,
    val detail: String? = null,
    val unassigned: List<String> = emptyList(),
    val run: RunInfo? = null,
) {
    /** Whether *Add to room* is offered: new entities are in no room yet, also after the entry was dismissed. */
    val canAssign: Boolean get() = type == NotificationType.ADDED && unassigned.isNotEmpty()

    /** Whether *Open schedule* is offered: an open run entry whose schedule still exists (concept 19.11). */
    val canOpenSchedule: Boolean get() = run?.exists == true && !resolved

    /** Whether *Dismiss* or *Keep* is offered: open entries, but a run entry with *Open schedule* resolves by it. */
    val canDismiss: Boolean get() = !resolved && !(type == NotificationType.SCHEDULE_RUN && run?.exists == true)
}

/** The entries of one day, newest first. */
data class NotificationDay(val date: LocalDate, val items: List<NotificationItem>)

/** Groups [items] (newest first) by their day in [zone] (concept 9.1: grouped by day). */
fun groupByDay(items: List<NotificationItem>, zone: ZoneId): List<NotificationDay> =
    items.groupBy { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() }
        .map { (date, dayItems) -> NotificationDay(date, dayItems) }
