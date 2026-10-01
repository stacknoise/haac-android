package com.stacknoise.haac.feature.notifications.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.ErrorMessage
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacTheme
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.database.notification.NotificationType
import com.stacknoise.haac.core.error.ErrorAction
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.feature.notifications.R
import com.stacknoise.haac.feature.notifications.domain.EntityLabel
import com.stacknoise.haac.feature.notifications.domain.NotificationDay
import com.stacknoise.haac.feature.notifications.domain.NotificationItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Notification list (M-09, concept 9.1, 17.4). [onErrorAction] receives the action of an error entry
 * (*Try again*, *Sign in*, *Open settings*) and the instance it belongs to (null for global entries);
 * [onAddToRoom] the entity ids of an *Add to room* or *Review N entities*, which also resolves the entry.
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onErrorAction: (ErrorAction, String?) -> Unit,
    onAddToRoom: (List<String>) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NotificationsContent(
        state = state,
        actions = NotificationActions(
            onBack = onBack,
            onOpen = viewModel::onOpen,
            onMarkAllRead = viewModel::onMarkAllRead,
            onResolve = viewModel::onResolve,
            onErrorAction = { item -> item.error?.let { onErrorAction(it.action, item.serverId) } },
            onAddToRoom = { item ->
                viewModel.onResolve(item)
                onAddToRoom(item.entities.map { it.entityId })
            },
            onRemoveTiles = viewModel::onRemoveTiles,
            onDelete = viewModel::onDelete,
            onDeleteAll = viewModel::onDeleteAll,
        ),
    )
    state.detail?.let { ErrorDetailSheet(it, state.instanceName, onDismiss = viewModel::onCloseDetail) }
}

/** Stateless layout: header, then the entries grouped by day. */
@Composable
fun NotificationsContent(state: NotificationsUiState, actions: NotificationActions) {
    val today = LocalDate.now()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
    ) {
        item { Header(actions, canDeleteAll = state.days.isNotEmpty()) }
        state.error?.let { item { ErrorMessage(it) } }
        if (state.days.isEmpty()) item { EmptyHint() }
        state.days.forEach { day ->
            item(key = "day-${day.date}") { DayLabel(day.date, today) }
            items(day.items, key = { it.id }) { entry ->
                Box(Modifier.padding(vertical = 5.dp)) {
                    SwipeToDelete(entry, actions.onDelete) { NotificationRow(entry, actions) }
                }
            }
        }
    }
}

/** Back arrow, *Mark all read*, *Delete all* (after a confirmation) and the title. */
@Composable
private fun Header(actions: NotificationActions, canDeleteAll: Boolean) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = actions.onBack) {
            Icon(painterResource(R.drawable.ic_notifications_back), stringResource(R.string.notifications_back))
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = actions.onMarkAllRead) { Text(stringResource(R.string.notifications_mark_all_read)) }
        if (canDeleteAll) {
            IconButton(onClick = { confirm = true }) {
                Icon(
                    painterResource(R.drawable.ic_notifications_delete),
                    stringResource(R.string.notifications_delete_all),
                )
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.notifications_delete_all_title)) },
            text = { Text(stringResource(R.string.notifications_delete_all_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirm = false
                        actions.onDeleteAll()
                    },
                ) {
                    Text(stringResource(R.string.notifications_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.notifications_cancel)) }
            },
        )
    }
    Text(
        stringResource(R.string.notifications_title),
        style = MaterialTheme.typography.headlineLarge,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
    )
}

/** *Today*, *Yesterday* or the date, as a section label. */
@Composable
private fun DayLabel(date: LocalDate, today: LocalDate) {
    val label = when (date) {
        today -> stringResource(R.string.notifications_today)
        today.minusDays(1) -> stringResource(R.string.notifications_yesterday)
        else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
    Text(
        label.uppercase(),
        style = SectionLabelStyle,
        modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 8.dp),
    )
}

/** Shown while the list is empty: a bell in an accent square above the message. */
@Composable
private fun EmptyHint() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 72.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp).background(HaacColors.AccentTintStrong, RoundedCornerShape(28.dp)),
        ) {
            Icon(
                painterResource(R.drawable.ic_notifications_bell),
                contentDescription = null,
                tint = HaacColors.Accent,
                modifier = Modifier.size(36.dp),
            )
        }
        Text(
            stringResource(R.string.notifications_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}

/** Preview with an added entity, a removed one and an error. */
@Preview
@Composable
private fun NotificationsPreview() {
    val now = System.currentTimeMillis()
    val items = listOf(
        NotificationItem(1, NotificationType.ADDED, now, unread = true, resolved = false,
            entities = listOf(EntityLabel("switch.hallway_light", "Hallway light"))),
        NotificationItem(2, NotificationType.REMOVED, now, unread = true, resolved = false,
            entities = listOf(EntityLabel("sensor.humidity", "Humidity"))),
        NotificationItem(3, NotificationType.ERROR, now, unread = false, resolved = false, count = 3,
            error = ErrorCode.NET_UNREACHABLE),
    )
    HaacTheme {
        NotificationsContent(
            NotificationsUiState(days = listOf(NotificationDay(LocalDate.now(), items))),
            NotificationActions({}, {}, {}, {}, {}),
        )
    }
}
