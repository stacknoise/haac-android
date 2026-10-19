package com.stacknoise.haac.feature.schedules.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.stacknoise.haac.feature.schedules.R
import com.stacknoise.haac.feature.schedules.domain.Ordinal
import com.stacknoise.haac.feature.schedules.domain.RepeatFreq
import com.stacknoise.haac.feature.schedules.domain.RepeatRule
import com.stacknoise.haac.feature.schedules.domain.ScheduleItem
import com.stacknoise.haac.feature.schedules.domain.WhenType
import com.stacknoise.haac.feature.schedules.domain.ordinalOf
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Locale

/** Date skeletons: weekday with day and month, the same with the year, day and month, and month with year. */
private const val ShortDate = "EEEdMMM"
private const val FullDate = "EEEdMMMy"
private const val DayAndMonth = "dMMM"
private const val MonthAndYear = "MMMMy"

/** [date] written in the phone's language for [skeleton], e.g. "Mon, 26 Oct" or "Mo., 26. Okt.". */
private fun formatDate(date: TemporalAccessor, skeleton: String): String {
    val locale = Locale.getDefault()
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(date)
}

/** The weekday, day and month of [date], e.g. "Mon, 26 Oct". */
internal fun shortDate(date: LocalDate): String = formatDate(date, ShortDate)

/** The weekday, day, month and year of [date], e.g. "Mon, 26 Oct 2026". */
internal fun fullDate(date: LocalDate): String = formatDate(date, FullDate)

/** The month and year of [month], e.g. "October 2026", for the calendar header. */
internal fun monthTitle(month: YearMonth): String = formatDate(month, MonthAndYear)

/** The local date of [millis] in the phone's time zone. */
internal fun dateOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

/** [days] (0 = Monday) as "Mon and Thu" or "Mon, Wed and Fri". */
@Composable
private fun daysList(days: List<Int>): String {
    val names = days.sorted().map { dayName(it) }
    return if (names.size < 2) names.joinToString() else names.dropLast(1).joinToString(", ") +
        " " + stringResource(R.string.schedules_and) + " " + names.last()
}

/** Day [day] of a month as an ordinal in the phone's language: "15th", "15.". */
@Composable
private fun ordinalText(day: Int): String = stringResource(
    when (ordinalOf(day)) {
        Ordinal.FIRST -> R.string.schedules_ordinal_first
        Ordinal.SECOND -> R.string.schedules_ordinal_second
        Ordinal.THIRD -> R.string.schedules_ordinal_third
        Ordinal.OTHER -> R.string.schedules_ordinal_other
    },
    day,
)

/** The interval in words: "Every day", "Every 2 weeks", "Every month", "Every year", or "Once" for a one-off rule. */
@Composable
internal fun intervalLabel(freq: RepeatFreq, interval: Int): String = when (freq) {
    RepeatFreq.ONCE -> stringResource(R.string.schedules_mode_once)
    RepeatFreq.DAILY -> if (interval == 1) {
        stringResource(R.string.schedules_repeat_every_day)
    } else {
        stringResource(R.string.schedules_every_n_days, interval)
    }
    RepeatFreq.WEEKLY -> if (interval == 1) {
        stringResource(R.string.schedules_every_week)
    } else {
        stringResource(R.string.schedules_every_n_weeks, interval)
    }
    RepeatFreq.MONTHLY -> if (interval == 1) {
        stringResource(R.string.schedules_every_month)
    } else {
        stringResource(R.string.schedules_every_n_months, interval)
    }
    RepeatFreq.YEARLY -> stringResource(R.string.schedules_every_year)
}

/**
 * The date rule in words without the time and the end: "Once · Mon, 26 Oct 2026", "Every weekday", "Every 2 weeks on
 * Mon and Thu", "Every month on the 15th", "Every year on 1 May" (concept 19.10). [days] count only for weekly.
 */
@Composable
internal fun repeatLabel(rule: RepeatRule, days: List<Int>): String {
    val start = rule.startDate
    return when {
        rule.once -> stringResource(R.string.schedules_label_once, start?.let(::fullDate).orEmpty())
        rule.freq == RepeatFreq.WEEKLY && rule.interval == 1 -> repeatText(days)
        rule.freq == RepeatFreq.WEEKLY -> {
            val every = intervalLabel(rule.freq, rule.interval)
            stringResource(R.string.schedules_label_weekly_days, every, daysList(days))
        }
        rule.freq == RepeatFreq.MONTHLY && start != null ->
            stringResource(
                R.string.schedules_label_monthly,
                intervalLabel(rule.freq, rule.interval),
                ordinalText(start.dayOfMonth),
            )
        rule.freq == RepeatFreq.YEARLY && start != null ->
            stringResource(R.string.schedules_label_yearly, formatDate(start, DayAndMonth))
        else -> intervalLabel(rule.freq, rule.interval)
    }
}

/**
 * The whole rule as a sentence: the [repeatLabel], the [phrase] of the trigger if there is one ("at 06:45", "30 min
 * after sunrise") and, for a rule with an end, ", until 31 Dec". This replaces the former weekday summary (19.10).
 */
@Composable
internal fun repeatSummary(rule: RepeatRule, days: List<Int>, phrase: String? = null): String {
    val base = repeatLabel(rule, days) + phrase?.let { " $it" }.orEmpty()
    val end = rule.endDate?.takeIf { !rule.once }
    return if (end == null) base else stringResource(R.string.schedules_label_until, base, formatDate(end, DayAndMonth))
}

/** The trigger after the rule: "at 06:45" for a fixed time, "at sunrise" or "30 min after sunset" for a sun event. */
@Composable
internal fun whenPhrase(type: WhenType, time: LocalTime?, offsetMin: Int): String =
    if (type == WhenType.TIME && time != null) {
        stringResource(R.string.schedules_at_time, time.toString())
    } else {
        offsetText(type, offsetMin).replaceFirstChar { it.lowercase() }
    }

/** The rule short, for the list card: "Once · Mon, 26 Oct", "Mon – Fri", "Every 2 weeks", "Every month". */
@Composable
internal fun shortRepeat(item: ScheduleItem): String {
    val rule = item.repeat
    return when {
        rule.once -> stringResource(R.string.schedules_label_once, rule.startDate?.let(::shortDate).orEmpty())
        rule.freq == RepeatFreq.WEEKLY && rule.interval == 1 -> shortDays(item.days)
        else -> intervalLabel(rule.freq, rule.interval)
    }
}
