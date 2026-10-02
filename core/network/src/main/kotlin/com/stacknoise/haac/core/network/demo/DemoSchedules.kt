package com.stacknoise.haac.core.network.demo

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.serialization.json.JsonObject

/**
 * The schedule rules of the demo bridge (concept 19, 20.3): validation, limits, optimistic concurrency, the planned
 * next run and the effect of *Run now*. Schedules never fire by themselves in the demo.
 */
internal class DemoSchedules(private val clock: Clock) {
    /** A new schedule from the fields of `schedules/create`; HAB-SCH-001 if invalid, HAB-SCH-005 at the limit. */
    fun create(
        fields: JsonObject,
        existing: List<DemoSchedule>,
        entities: List<DemoEntity>,
    ): DemoOutcome<DemoSchedule> = DemoScheduleRules.plan(fields, null, entities).then { plan ->
        if (existing.size >= DemoScheduleRules.MAX_SCHEDULES) {
            DemoOutcome.Failed(DemoCodes.SCH_LIMIT, "Too many schedules")
        } else {
            val now = version(null)
            DemoOutcome.Ok(build(UUID.randomUUID().toString(), plan, now, now, null))
        }
    }

    /**
     * [current] with the changed fields of `schedules/update`; HAB-SCH-004 if [updatedAt] is not its version,
     * HAB-SCH-001 if the result is invalid.
     */
    fun update(
        fields: JsonObject,
        current: DemoSchedule,
        updatedAt: String?,
        entities: List<DemoEntity>,
    ): DemoOutcome<DemoSchedule> = if (updatedAt != current.updatedAt) {
        DemoOutcome.Failed(DemoCodes.SCH_CONFLICT, "The schedule changed in the meantime")
    } else {
        DemoScheduleRules.plan(fields, current, entities).then { plan ->
            val version = version(current.updatedAt)
            DemoOutcome.Ok(build(current.id, plan, current.createdAt, version, current.lastRun))
        }
    }

    /** The entities [schedule] switches, in the state its action leads to, and the schedule with its new `last_run`. */
    fun run(schedule: DemoSchedule, entities: List<DemoEntity>): Pair<DemoSchedule, List<DemoEntity>> {
        val now = clock.millis()
        val switched = schedule.entities
            .mapNotNull { id -> entities.firstOrNull { it.entityId == id } }
            .mapNotNull { switch(it, schedule.action, now) }
        return schedule.copy(lastRun = DemoLastRun(DemoTimes.iso(now), "ok")) to switched
    }

    /**
     * The next planned run as an ISO time with offset, computed in the time zone of the phone from the weekdays and
     * the time (sun events: the fixed demo times plus the offset); null while the schedule is off.
     */
    fun nextRun(schedule: DemoSchedule): String? = schedule.takeIf { it.enabled }
        ?.let(::upcoming)
        ?.let { DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(it.atZone(clock.zone)) }

    /** The earliest planned run after now within the next days that falls on one of the weekdays. */
    private fun upcoming(schedule: DemoSchedule): Instant? {
        val today = LocalDate.now(clock)
        return (-1L..SEARCH_DAYS).map { runOn(schedule, today.plusDays(it), clock.zone) }
            .filter { it.toInstant().isAfter(clock.instant()) && schedule.days.contains(it.dayOfWeek.value - 1) }
            .minOfOrNull { it.toInstant() }
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

    /** [entity] after the schedule action [action], or null if the action does not apply to it. */
    private fun switch(entity: DemoEntity, action: String, now: Long): DemoEntity? =
        (DemoServices.call(entity, action, JsonObject(emptyMap()), now) as? DemoOutcome.Ok)?.value

    /** The schedule from a validated [plan]. */
    private fun build(id: String, plan: Plan, createdAt: String, updatedAt: String, lastRun: DemoLastRun?) =
        DemoSchedule(
            id, plan.name, plan.enabled, plan.whenType, plan.time, plan.days, plan.offsetMin, plan.action,
            plan.entities, createdAt, updatedAt, lastRun,
        )

    /** A version token after [previous]: the current time, at least a millisecond later than [previous]. */
    private fun version(previous: String?): String {
        val now = clock.instant()
        val earliest = previous?.let { Instant.parse(it).plusMillis(1) }
        return DemoTimes.iso(maxOf(now, earliest ?: now).toEpochMilli())
    }

    /** Search range of the next run in days and the fixed sun times of the demo (concept 20.3). */
    private companion object {
        const val SEARCH_DAYS = 8L
        val SUNRISE: LocalTime = LocalTime.of(6, 30)
        val SUNSET: LocalTime = LocalTime.of(19, 30)
    }
}
