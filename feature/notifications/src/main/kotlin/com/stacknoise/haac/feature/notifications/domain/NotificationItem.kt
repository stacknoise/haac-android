package com.stacknoise.haac.feature.notifications.domain

import com.stacknoise.haac.core.database.notification.NotificationType
import com.stacknoise.haac.core.error.ErrorCode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** An entity named in an entry: its display name (configured name, else HA name, else id) and its id. */
data class EntityLabel(val entityId: String, val name: String)

/**
 * One entry of the notification list as the screen shows it (concept 9.1, 17.4). [entities] are set for
 * [NotificationType.ADDED] and [NotificationType.REMOVED], [error] for [NotificationType.ERROR].
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
)

/** The entries of one day, newest first. */
data class NotificationDay(val date: LocalDate, val items: List<NotificationItem>)

/** Groups [items] (newest first) by their day in [zone] (concept 9.1: grouped by day). */
fun groupByDay(items: List<NotificationItem>, zone: ZoneId): List<NotificationDay> =
    items.groupBy { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() }
        .map { (date, dayItems) -> NotificationDay(date, dayItems) }
