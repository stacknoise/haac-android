package com.stacknoise.haac.feature.notifications.ui

import com.stacknoise.haac.feature.notifications.domain.NotificationItem

/** Callbacks of the notification list. */
data class NotificationActions(
    val onBack: () -> Unit,
    val onOpen: (NotificationItem) -> Unit,
    val onMarkAllRead: () -> Unit,
    val onResolve: (NotificationItem) -> Unit,
    val onErrorAction: (NotificationItem) -> Unit,
)
