package com.stacknoise.haac.feature.schedules.domain

import java.time.LocalDate
import java.time.format.DateTimeParseException

/** How often a schedule runs (concept 19.10); [maxInterval] is the largest interval the bridge accepts. */
enum class RepeatFreq(val wire: String, val maxInterval: Int) {
    ONCE("once", 1),
    DAILY("daily", MaxDays),
    WEEKLY("weekly", MaxWeeks),
    MONTHLY("monthly", MaxMonths),
    YEARLY("yearly", 1),
    ;

    /** True for the frequencies with an interval stepper ("Every 2 weeks"); once and yearly have none. */
    val hasInterval: Boolean get() = maxInterval > 1

    /** Lookup by the bridge's name. */
    companion object {
        /** The frequency named [wire]; an unknown one from a newer bridge reads as weekly. */
        fun of(wire: String): RepeatFreq = entries.firstOrNull { it.wire == wire } ?: WEEKLY
    }
}

/** Largest intervals of the bridge in days, weeks and months (concept 19.10). */
private const val MaxDays = 365
private const val MaxWeeks = 52
private const val MaxMonths = 12

/**
 * The date rule of a schedule (concept 19.10): [freq] every [interval] days, weeks or months, from [startDate] to
 * [endDate] (both local dates, [endDate] without limit and only for repeating rules). A one-off schedule runs on
 * [startDate] only. A [startDate] of null means "today" for the bridge.
 */
data class RepeatRule(
    val freq: RepeatFreq = RepeatFreq.WEEKLY,
    val interval: Int = 1,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
) {
    /** True for a one-off schedule, which the bridge removes after its run. */
    val once: Boolean get() = freq == RepeatFreq.ONCE

    /** True if the dates are consistent: a one-off schedule has a date and no end, the end is not before the start. */
    val valid: Boolean
        get() = (!once || (startDate != null && endDate == null)) &&
            (startDate == null || endDate == null || !endDate.isBefore(startDate))

    /**
     * The rule with frequency [to]: the interval is kept within the new limit, a one-off schedule gets [today] plus a
     * day when it has no date yet and loses its end date, and a repeating one keeps its dates.
     */
    fun switchedTo(to: RepeatFreq, today: LocalDate): RepeatRule = when (to) {
        RepeatFreq.ONCE -> RepeatRule(to, 1, startDate?.takeIf { it.isAfter(today) } ?: today.plusDays(1))
        else -> copy(freq = to, interval = interval.coerceIn(1, to.maxInterval))
    }

    /** The interval moved by [steps], kept within 1 and the limit of the frequency. */
    fun steppedBy(steps: Int): RepeatRule = copy(interval = (interval + steps).coerceIn(1, freq.maxInterval))

    /** The rule starting on [date]; an end date before it is dropped. */
    fun startingOn(date: LocalDate): RepeatRule =
        copy(startDate = date, endDate = endDate?.takeIf { !it.isBefore(date) })
}

/** How the weekdays of a schedule read in the summary (design 3b, concept 19.7). */
enum class DayPattern {
    EVERY_DAY,
    WEEKDAYS,
    WEEKEND,
    NONE,
    CUSTOM,
}

private val WEEKDAY_SET = setOf(0, 1, 2, 3, 4)
private val WEEKEND_SET = setOf(5, 6)

/** Classifies [days] (0 = Monday to 6 = Sunday): all days, Monday to Friday, Saturday and Sunday, none or others. */
fun dayPatternOf(days: Collection<Int>): DayPattern = when (days.toSet()) {
    emptySet<Int>() -> DayPattern.NONE
    WEEKDAY_SET -> DayPattern.WEEKDAYS
    WEEKEND_SET -> DayPattern.WEEKEND
    else -> if (days.toSet().size == WeekDays.COUNT) DayPattern.EVERY_DAY else DayPattern.CUSTOM
}

/** The date of the bridge's `YYYY-MM-DD` text, or null if it is missing or malformed. */
fun parseDate(text: String?): LocalDate? = try {
    text?.let(LocalDate::parse)
} catch (_: DateTimeParseException) {
    null
}
