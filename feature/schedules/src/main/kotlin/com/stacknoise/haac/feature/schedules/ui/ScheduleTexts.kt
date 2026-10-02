package com.stacknoise.haac.feature.schedules.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.stacknoise.haac.feature.schedules.R
import com.stacknoise.haac.feature.schedules.data.ScheduleView
import com.stacknoise.haac.feature.schedules.domain.Repeat
import com.stacknoise.haac.feature.schedules.domain.ScheduleAction
import com.stacknoise.haac.feature.schedules.domain.ScheduleItem
import com.stacknoise.haac.feature.schedules.domain.WhenType
import com.stacknoise.haac.feature.schedules.domain.repeatOf
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Monday and Friday as day numbers (0 = Monday). */
private const val Monday = 0
private const val Friday = 4

/** The name of weekday [day] (0 = Monday): abbreviated, or narrow for the day buttons. */
internal fun dayName(day: Int, style: TextStyle = TextStyle.SHORT): String =
    DayOfWeek.of(day + 1).getDisplayName(style, Locale.getDefault())

/** Clock time of [millis] in the phone's time zone, e.g. 06:45. */
internal fun clockOf(millis: Long): String = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

/** The day of [millis] abbreviated and its clock time, e.g. Fri 06:45. */
internal fun dayAndClock(millis: Long): String {
    val day = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).dayOfWeek
    return day.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " " + clockOf(millis)
}

/** The time shown big: the fixed time, or for a sun event the time of its next run, else the event's name. */
@Composable
internal fun timeText(item: ScheduleItem): String = when {
    item.whenType == WhenType.TIME && item.time != null -> item.time.toString()
    item.nextRun != null -> clockOf(item.nextRun)
    else -> sunName(item.whenType)
}

/** *Sunrise* or *Sunset*; other triggers have no sun name. */
@Composable
internal fun sunName(type: WhenType): String = stringResource(
    if (type == WhenType.SUNSET) R.string.schedules_sunset else R.string.schedules_sunrise,
)

/** The offset of a sun event in words: "At sunrise", "30 min after sunset". */
@Composable
internal fun offsetText(type: WhenType, offsetMin: Int): String {
    val sun = sunName(type).lowercase()
    return when {
        offsetMin == 0 -> stringResource(R.string.schedules_sun_at, sun)
        offsetMin < 0 -> stringResource(R.string.schedules_sun_before, -offsetMin, sun)
        else -> stringResource(R.string.schedules_sun_after, offsetMin, sun)
    }
}

/** The weekdays short, for the card: "Every day", "Mon – Fri", "Sat, Sun", "Mon, Wed". */
@Composable
internal fun shortDays(days: List<Int>): String = when (repeatOf(days)) {
    Repeat.EVERY_DAY -> stringResource(R.string.schedules_short_every_day)
    Repeat.NONE -> stringResource(R.string.schedules_short_none)
    Repeat.WEEKDAYS -> stringResource(R.string.schedules_short_weekdays, dayName(Monday), dayName(Friday))
    else -> days.sorted().joinToString(", ") { dayName(it) }
}

/** The weekdays in words, for the detail screen: "Every weekday", "Every weekend", "Mon, Wed, Fri". */
@Composable
internal fun repeatText(days: List<Int>): String = when (repeatOf(days)) {
    Repeat.EVERY_DAY -> stringResource(R.string.schedules_repeat_every_day)
    Repeat.WEEKDAYS -> stringResource(R.string.schedules_repeat_weekdays)
    Repeat.WEEKEND -> stringResource(R.string.schedules_repeat_weekend)
    Repeat.NONE -> stringResource(R.string.schedules_repeat_none)
    Repeat.CUSTOM -> days.sorted().joinToString(", ") { dayName(it) }
}

/** What the schedule does to an entity: "turns on", "turns off", "toggles". */
@Composable
internal fun actionVerb(action: ScheduleAction): String = stringResource(
    when (action) {
        ScheduleAction.TURN_ON -> R.string.schedules_turns_on
        ScheduleAction.TURN_OFF -> R.string.schedules_turns_off
        ScheduleAction.TOGGLE -> R.string.schedules_toggles
    },
)

/** The target line of a card: "Lamp · turns on" for one entity, "2 entities turn off" for several. */
@Composable
internal fun targetText(view: ScheduleView): String {
    val count = view.entityNames.size
    if (count == 1) {
        return stringResource(R.string.schedules_target_single, view.entityNames.single(), actionVerb(view.item.action))
    }
    val plural = when (view.item.action) {
        ScheduleAction.TURN_ON -> R.plurals.schedules_targets_on
        ScheduleAction.TURN_OFF -> R.plurals.schedules_targets_off
        ScheduleAction.TOGGLE -> R.plurals.schedules_targets_toggle
    }
    return pluralStringResource(plural, count, count)
}
