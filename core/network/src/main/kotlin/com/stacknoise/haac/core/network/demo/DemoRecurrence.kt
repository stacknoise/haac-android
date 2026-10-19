package com.stacknoise.haac.core.network.demo

import com.stacknoise.haac.core.network.http.text
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/** The date rules of the demo bridge for repeating schedules (concept 19.10): validation and the dates a rule hits. */
internal object DemoRecurrence {
    /** Frequency names of the bridge. */
    const val ONCE = "once"
    const val DAILY = "daily"
    const val WEEKLY = "weekly"
    const val MONTHLY = "monthly"
    const val YEARLY = "yearly"

    /** How far ahead a one-off date may lie, in days. */
    private const val MAX_AHEAD_DAYS = 366L
    private val MAX_INTERVAL = mapOf(DAILY to 365, WEEKLY to 52, MONTHLY to 12, YEARLY to 1)

    /** Phase of a rule without a start date (a Monday); only weekly rules with interval 1 can lack one. */
    private val FALLBACK_START = LocalDate.of(2000, 1, 3)

    /**
     * The rule of the `repeat` object [raw] as sent by the app, validated as the bridge does (HAB-SCH-001); a missing
     * start date becomes [today].
     */
    fun parse(raw: JsonObject, today: LocalDate): DemoOutcome<DemoRepeat> {
        val freq = raw.text("freq")
        val interval = (raw["interval"] as? JsonPrimitive)?.let { it.intOrNull ?: INVALID_NUMBER }
        val start = raw.text("start_date")?.let(::date)
        val end = raw.text("end_date")?.let(::date)
        val problem = shapeProblem(freq, interval, raw) ?: dateProblem(freq, start, end, today)
        return if (problem == null) {
            DemoOutcome.Ok(DemoRepeat(freq.orEmpty(), interval ?: 1, (start ?: today).toString(), end?.toString()))
        } else {
            DemoOutcome.Failed(DemoCodes.SCH_INVALID, problem)
        }
    }

    /** True if a schedule with [repeat] and the weekdays [days] (0 = Monday) runs on [date]. */
    fun matches(repeat: DemoRepeat, days: List<Int>, date: LocalDate): Boolean {
        val start = repeat.startDate?.let(::date) ?: FALLBACK_START
        val end = repeat.endDate?.let(::date)
        val inRange = date >= start && (end == null || date <= end)
        return inRange && when (repeat.freq) {
            ONCE -> date == start
            DAILY -> ChronoUnit.DAYS.between(start, date) % repeat.interval == 0L
            WEEKLY -> weekMatches(repeat, days, start, date)
            MONTHLY -> monthMatches(start, date, repeat.interval)
            else -> date.monthValue == start.monthValue && dayMatches(start, date)
        }
    }

    /** Weekly: the weekday is chosen and the week is a multiple of the interval after the week of [start]. */
    private fun weekMatches(repeat: DemoRepeat, days: List<Int>, start: LocalDate, date: LocalDate): Boolean {
        val monday = TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        val weeks = ChronoUnit.WEEKS.between(start.with(monday), date.with(monday))
        return (date.dayOfWeek.value - 1) in days && weeks % repeat.interval == 0L
    }

    /** Monthly: the month is a multiple of the interval after the month of [start] and the day of month hits. */
    private fun monthMatches(start: LocalDate, date: LocalDate, interval: Int): Boolean {
        val months = ChronoUnit.MONTHS.between(start.withDayOfMonth(1), date.withDayOfMonth(1))
        return months % interval == 0L && dayMatches(start, date)
    }

    /** The day of month of [start], or the last day of the month if it has no such day (31 April, 29 February). */
    private fun dayMatches(start: LocalDate, date: LocalDate): Boolean =
        date.dayOfMonth == minOf(start.dayOfMonth, date.lengthOfMonth())

    /** Unknown fields, frequency and interval limits; null if the shape is right. */
    private fun shapeProblem(freq: String?, interval: Int?, raw: JsonObject): String? = when {
        !raw.keys.all { it in FIELDS } -> "Unknown repeat field"
        DATES.any { key -> raw.text(key)?.let { date(it) == null } == true } -> "Dates need the form YYYY-MM-DD"
        freq != ONCE && freq !in MAX_INTERVAL -> "Unknown frequency $freq"
        freq == ONCE && interval != null -> "A one-off schedule has no interval"
        freq != ONCE && interval != null && interval !in 1..MAX_INTERVAL.getValue(freq.orEmpty()) ->
            "The interval of $freq is 1 to ${MAX_INTERVAL.getValue(freq.orEmpty())}"
        else -> null
    }

    /** Date format, order and range of the start and end date; null if they hold. */
    private fun dateProblem(freq: String?, start: LocalDate?, end: LocalDate?, today: LocalDate): String? = when {
        freq == ONCE && (start == null || end != null) -> "A one-off schedule needs a date and no end date"
        start != null && end != null && end < start -> "The end date is before the start date"
        freq == ONCE && start != null && start > today.plusDays(MAX_AHEAD_DAYS) -> "The date is too far ahead"
        else -> null
    }

    /** The date of [text], or null if it is not `YYYY-MM-DD`. */
    private fun date(text: String): LocalDate? = try {
        LocalDate.parse(text)
    } catch (_: DateTimeParseException) {
        null
    }

    /** Request fields of a repeat object. */
    private val FIELDS = setOf("freq", "interval", "start_date", "end_date")
    private val DATES = listOf("start_date", "end_date")

    /** Marks an interval that is not an integer, so it fails the range check. */
    private const val INVALID_NUMBER = -1
}
