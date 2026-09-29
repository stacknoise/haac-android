package com.stacknoise.haac.feature.notifications.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.MonoFontFamily
import com.stacknoise.haac.core.database.notification.NotificationType
import com.stacknoise.haac.core.error.ErrorAction
import com.stacknoise.haac.feature.notifications.R
import com.stacknoise.haac.feature.notifications.domain.NotificationItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** One entry: icon, title, time, text, unread dot and, until it is dismissed, its actions (M-09, 17.4). */
@Composable
internal fun NotificationRow(item: NotificationItem, actions: NotificationActions) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { actions.onOpen(item) }
            .padding(horizontal = 8.dp, vertical = 16.dp),
    ) {
        EntryIcon(item)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title(item), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(timeOf(item.createdAt), style = MaterialTheme.typography.bodySmall, color = secondary())
            }
            Text(text(item), style = MaterialTheme.typography.bodyMedium, color = secondary())
            if (!item.resolved) EntryActions(item, actions)
        }
        Spacer(Modifier.width(12.dp))
        UnreadDot(item.unread)
    }
}

/** Icon in a rounded box; unread entries use the accent container, errors the error colour. */
@Composable
private fun EntryIcon(item: NotificationItem) {
    val colors = MaterialTheme.colorScheme
    val icon = when (item.type) {
        NotificationType.ADDED -> R.drawable.ic_notifications_added
        else -> R.drawable.ic_notifications_warning
    }
    val tint = when {
        item.type == NotificationType.ERROR -> colors.error
        item.unread -> colors.primary
        else -> colors.onSurfaceVariant
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .background(if (item.unread) colors.primaryContainer else colors.surface, HaacShapes.Medium),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** The dot of an unread entry; keeps its space when read so the rows stay aligned. */
@Composable
private fun UnreadDot(unread: Boolean) {
    val description = stringResource(R.string.notifications_unread)
    val color = if (unread) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        modifier = Modifier
            .padding(top = 8.dp)
            .size(8.dp)
            .background(color, CircleShape)
            .then(if (unread) Modifier.semantics { contentDescription = description } else Modifier),
    )
}

/**
 * *Dismiss* or *Keep*, after the entry's own action: *Add to room* or *Review N entities* for new entities,
 * *Remove tile* for removed ones (concept 9.1), the error's action for errors (17.4).
 */
@Composable
private fun EntryActions(item: NotificationItem, actions: NotificationActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        when (item.type) {
            NotificationType.ADDED -> {
                val label = if (item.count == 1) {
                    stringResource(R.string.notifications_add_to_room)
                } else {
                    pluralStringResource(R.plurals.notifications_review, item.count, item.count)
                }
                ActionButton(label, primary = true) { actions.onAddToRoom(item) }
            }
            NotificationType.REMOVED -> ActionButton(
                pluralStringResource(R.plurals.notifications_remove_tiles, item.count),
                primary = true,
            ) { actions.onRemoveTiles(item) }
            NotificationType.ERROR -> {
                val label = item.error?.action?.takeIf { it != ErrorAction.NONE }?.label
                if (label != null) ActionButton(stringResource(label), primary = true) { actions.onErrorAction(item) }
            }
        }
        val resolve = when (item.type) {
            NotificationType.REMOVED -> R.string.notifications_keep
            else -> R.string.notifications_dismiss
        }
        ActionButton(stringResource(resolve), primary = false) { actions.onResolve(item) }
    }
}

/** Outlined button in the accent colour ([primary]) or neutral (concept 15.2). */
@Composable
private fun ActionButton(text: String, primary: Boolean, onClick: () -> Unit) {
    val color = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
    OutlinedButton(onClick = onClick, shape = HaacShapes.Medium) { Text(text, color = color) }
}

/** Title of an entry: what happened, or the error's user message. */
@Composable
private fun title(item: NotificationItem): String = when (item.type) {
    NotificationType.ADDED -> pluralStringResource(R.plurals.notifications_added, item.count, item.count)
    NotificationType.REMOVED -> pluralStringResource(R.plurals.notifications_removed, item.count, item.count)
    NotificationType.ERROR -> stringResource(item.error?.message ?: R.string.notifications_title)
}

/** Text of an entry: name and `entity_id`, the names of several entities, or the error code (mono). */
@Composable
private fun text(item: NotificationItem): AnnotatedString {
    val names = item.entities.joinToString(", ") { it.name }
    return when (item.type) {
        NotificationType.ADDED -> item.entities.singleOrNull()
            ?.let { mono(it.name + " · ", it.entityId, "") }
            ?: AnnotatedString(names)
        NotificationType.REMOVED ->
            AnnotatedString(pluralStringResource(R.plurals.notifications_removed_text, item.count, names))
        NotificationType.ERROR -> {
            val times = if (item.count > 1) " · " + stringResource(R.string.notifications_times, item.count) else ""
            mono("", item.error?.code.orEmpty(), times)
        }
    }
}

/** [code] in the mono font between [before] and [after]. */
private fun mono(before: String, code: String, after: String) = buildAnnotatedString {
    append(before)
    withStyle(SpanStyle(fontFamily = MonoFontFamily)) { append(code) }
    append(after)
}

/** Secondary text colour. */
@Composable
private fun secondary(): Color = MaterialTheme.colorScheme.onSurfaceVariant

/** Local time of day of [millis], e.g. 09:12. */
internal fun timeOf(millis: Long): String = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
