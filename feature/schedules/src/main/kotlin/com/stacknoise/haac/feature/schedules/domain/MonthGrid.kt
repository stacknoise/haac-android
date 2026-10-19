package com.stacknoise.haac.feature.schedules.domain

import java.time.LocalDate
import java.time.YearMonth

/** The cells of [month] for a calendar grid that starts on Monday: null for the empty cells before the first day. */
fun monthCells(month: YearMonth): List<LocalDate?> {
    val lead = month.atDay(1).dayOfWeek.value - 1
    return List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
}

/** True if [date] may be picked: not before [min] and, if there is a limit, not after [max]. */
fun isPickable(date: LocalDate, min: LocalDate, max: LocalDate?): Boolean =
    !date.isBefore(min) && (max == null || !date.isAfter(max))

/** How a day of the month is written as an ordinal; the strings decide the words ("15th", "15."). */
enum class Ordinal { FIRST, SECOND, THIRD, OTHER }

/** The ordinal form of [day] of a month: 1, 21, 31 are first, 2 and 22 second, 3 and 23 third, 11 to 13 other. */
fun ordinalOf(day: Int): Ordinal =
    if (day % HundredStep in TeenRange) Ordinal.OTHER else LastDigit.getOrElse(day % DecimalStep) { Ordinal.OTHER }

private const val HundredStep = 100
private const val DecimalStep = 10
private val TeenRange = 11..13

/** The ordinal by the last digit 0, 1, 2, 3; the other digits are [Ordinal.OTHER]. */
private val LastDigit = listOf(Ordinal.OTHER, Ordinal.FIRST, Ordinal.SECOND, Ordinal.THIRD)
