package com.stacknoise.haac.feature.notifications.ui

import com.stacknoise.haac.feature.notifications.domain.NotificationItem

/**
 * Callbacks of the notification list; *Add to room* and *Remove tile* belong to entity entries (concept 9.1),
 * *Open schedule* to run entries (19.11).
 */
data class NotificationActions(
    val onBack: () -> Unit,
    val onOpen: (NotificationItem) -> Unit,
    val onMarkAllRead: () -> Unit,
    val onResolve: (NotificationItem) -> Unit,
    val onErrorAction: (NotificationItem) -> Unit,
    val onAddToRoom: (NotificationItem) -> Unit = {},
    val onRemoveTiles: (NotificationItem) -> Unit = {},
    val onOpenSchedule: (NotificationItem) -> Unit = {},
    val onDelete: (NotificationItem) -> Unit = {},
    val onDeleteAll: () -> Unit = {},
)
