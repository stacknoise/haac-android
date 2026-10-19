package com.stacknoise.haac.feature.schedules.ui

import android.text.format.DateFormat
import java.time.Instant
import java.time.LocalDate
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

/** The day and month of [date] without the weekday, e.g. "26 Oct". */
internal fun dayAndMonth(date: LocalDate): String = formatDate(date, DayAndMonth)
