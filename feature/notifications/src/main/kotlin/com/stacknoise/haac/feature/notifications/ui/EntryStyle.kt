package com.stacknoise.haac.feature.notifications.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.stacknoise.haac.core.common.sync.RunResult
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.MonoFontFamily
import com.stacknoise.haac.core.database.notification.NotificationType
import com.stacknoise.haac.feature.notifications.R
import com.stacknoise.haac.feature.notifications.domain.NotificationItem

/** Opacity of the amber tint behind the icon of a partial run. */
private const val RunTintAlpha = 0.18f

/** The icon drawable of an entry with its [tint] and the [background] of its rounded box. */
internal class IconStyle(val icon: Int, val tint: Color, val background: Color)

/** The icon style of [item]: by the result for a run entry (concept 19.11), else by its type and read state. */
@Composable
internal fun entryIconStyle(item: NotificationItem): IconStyle =
    item.run?.let { runIconStyle(it.result) } ?: baseIconStyle(item)

/** Accent when unread, muted when read, error colour for errors; a warning triangle except for new entities. */
@Composable
private fun baseIconStyle(item: NotificationItem): IconStyle {
    val colors = MaterialTheme.colorScheme
    val icon = if (item.type == NotificationType.ADDED) {
        R.drawable.ic_notifications_added
    } else {
        R.drawable.ic_notifications_warning
    }
    val tint = when {
        item.type == NotificationType.ERROR -> colors.error
        item.unread -> colors.primary
        else -> colors.onSurfaceVariant
    }
    val background = if (item.unread) colors.primaryContainer else HaacColors.SurfaceMuted
    return IconStyle(icon, tint, background)
}

/** A clock for a run that went well, an amber triangle for a partial one, a red one for a failed or missed run. */
@Composable
private fun runIconStyle(result: RunResult): IconStyle {
    val colors = MaterialTheme.colorScheme
    return when (result) {
        RunResult.OK -> IconStyle(R.drawable.ic_notifications_clock, colors.onSurfaceVariant, HaacColors.SurfaceMuted)
        RunResult.PARTIAL -> HaacColors.ChartYellow.let {
            IconStyle(R.drawable.ic_notifications_warning, it, it.copy(alpha = RunTintAlpha))
        }
        RunResult.FAILED, RunResult.MISSED ->
            IconStyle(R.drawable.ic_notifications_warning, colors.error, HaacColors.DangerContainer)
    }
}

/** "Morning light ran", "ran only partly", "failed" or "did not run" (missed), with the count of runs (19.11). */
@Composable
internal fun runTitle(item: NotificationItem): String {
    val name = item.entities.firstOrNull()?.name.orEmpty()
    val plural = when (item.run?.result) {
        RunResult.OK -> R.plurals.notifications_run_ok
        RunResult.PARTIAL -> R.plurals.notifications_run_partial
        RunResult.FAILED -> R.plurals.notifications_run_failed
        else -> R.plurals.notifications_run_missed
    }
    return pluralStringResource(plural, item.count, name, item.count)
}

/**
 * The line under a run title (concept 19.11): the time of the run (of the last one if several), what went wrong
 * and "The one-off schedule was removed"; the code HAAC-SCH-007 in mono on its own line for partial and failed runs.
 */
@Composable
internal fun runText(item: NotificationItem): AnnotatedString {
    val run = item.run
    val time = timeOf(item.createdAt)
    val head = if (item.count > 1) stringResource(R.string.notifications_run_last, time) else time
    val reason = when (run?.result) {
        RunResult.PARTIAL, RunResult.FAILED -> R.string.notifications_run_incomplete
        RunResult.MISSED -> R.string.notifications_run_missed_text
        else -> null
    }
    val parts = listOfNotNull(
        head,
        reason?.let { stringResource(it) },
        stringResource(R.string.notifications_run_removed).takeIf { run?.removed == true },
    )
    val code = item.error?.code
    return buildAnnotatedString {
        append(parts.joinToString(" · "))
        if (code != null) {
            append("\n")
            withStyle(SpanStyle(fontFamily = MonoFontFamily)) { append(code) }
        }
    }
}
