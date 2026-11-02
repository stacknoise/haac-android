package com.stacknoise.haac.core.network.demo

import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * The run times of the demo schedules (concept 19.10, 19.12): the runs a date rule hits, the upcoming runs with the
 * skipped ones between them, and skipping a run. Schedules never fire by themselves in the demo.
 */
internal class DemoRunTimes(private val clock: Clock) {
    /** The first [count] runs after now that the date rule hits from its start date on, on or off. */
    fun runs(schedule: DemoSchedule, count: Int): List<ZonedDateTime> {
        val today = LocalDate.now(clock)
        val from = maxOf(today.minusDays(1), schedule.repeat.startDate?.let(LocalDate::parse) ?: today)
        return (0L..SEARCH_DAYS).asSequence()
            .map { runOn(schedule, from.plusDays(it), clock.zone) }
            .filter { it.toInstant().isAfter(clock.instant()) }
            .filter { DemoRecurrence.matches(schedule.repeat, schedule.days, it.toLocalDate()) }
            .take(count)
            .toList()
    }

    /** The planned time of [schedule] on [date]; a time inside a DST gap moves to the first minute after it. */
    private fun runOn(schedule: DemoSchedule, date: LocalDate, zone: ZoneId): ZonedDateTime {
        val base = when (schedule.whenType) {
            "sunrise" -> SUNRISE
            "sunset" -> SUNSET
            else -> LocalTime.parse(schedule.time)
        }
        return ZonedDateTime.of(date, base, zone).plusMinutes((schedule.offsetMin ?: 0).toLong())
    }

    /** The runs up to the [NEXT_RUNS]-th that happens, with the skipped ones between them (concept 19.12). */
    fun upcoming(all: List<ZonedDateTime>, skipped: Set<String>): List<DemoUpcoming> {
        var happening = 0
        return all.asSequence()
            .takeWhile { happening < NEXT_RUNS }
            .onEach { if (it.toLocalDate().toString() !in skipped) happening++ }
            .map { DemoUpcoming(iso(it), it.toLocalDate().toString(), it.toLocalDate().toString() in skipped) }
            .toList()
    }

    /** [time] as an ISO time with offset. */
    fun iso(time: ZonedDateTime): String = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time)

    /**
     * [schedule] with the run of [date] skipped or taken up again (concept 19.12); HAB-SCH-010 unless [date] is one
     * of the next runs and another run stays.
     */
    fun skipRun(schedule: DemoSchedule, date: String, skip: Boolean): DemoOutcome<DemoSchedule> {
        val today = LocalDate.now(clock).toString()
        val dates = runs(schedule, UPCOMING_LIMIT).map { it.toLocalDate().toString() }
        val kept = schedule.skipped.filter { it >= today }
        val skipped = if (skip) (kept + date).distinct() else kept - date
        val valid = !skip || (date in dates && dates.any { it !in skipped })
        return if (valid) {
            DemoOutcome.Ok(schedule.copy(skipped = skipped.sorted()))
        } else {
            DemoOutcome.Failed(DemoCodes.SCH_SKIP_INVALID, "This run cannot be skipped")
        }
    }

    /** Search range of the next runs in days, their number, the upcoming limit and the fixed sun times (20.3). */
    companion object {
        private const val SEARCH_DAYS = 800L
        const val NEXT_RUNS = 3
        const val UPCOMING_LIMIT = 14
        private val SUNRISE: LocalTime = LocalTime.of(6, 30)
        private val SUNSET: LocalTime = LocalTime.of(19, 30)
    }
}
