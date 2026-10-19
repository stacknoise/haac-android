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

/** The planned runs of a schedule as the bridge reports them (concept 19.10): ISO times, and whether none is left. */
internal class Planned(val nextRun: String?, val nextRuns: List<String>, val expired: Boolean)

/**
 * The schedule rules of the demo bridge (concept 19, 20.3): validation, limits, optimistic concurrency, the planned
 * next runs and the effect of *Run now*. Schedules never fire by themselves in the demo.
 */
internal class DemoSchedules(private val clock: Clock) {
    /** A new schedule from the fields of `schedules/create`; HAB-SCH-001 if invalid, HAB-SCH-005 at the limit. */
    fun create(
        fields: JsonObject,
        existing: List<DemoSchedule>,
        entities: List<DemoEntity>,
    ): DemoOutcome<DemoSchedule> = DemoScheduleRules.plan(fields, null, entities, today()).then { plan ->
        if (existing.size >= DemoScheduleRules.MAX_SCHEDULES) {
            DemoOutcome.Failed(DemoCodes.SCH_LIMIT, "Too many schedules")
        } else {
            val now = version(null)
            checked(build(UUID.randomUUID().toString(), plan, now, now, null))
        }
    }

    /**
     * [current] with the changed fields of `schedules/update`; HAB-SCH-004 if [updatedAt] is not its version,
     * HAB-SCH-001 if the result is invalid, HAB-SCH-009 if it has no run left and is changed in its dates or enabled.
     */
    fun update(
        fields: JsonObject,
        current: DemoSchedule,
        updatedAt: String?,
        entities: List<DemoEntity>,
    ): DemoOutcome<DemoSchedule> = if (updatedAt != current.updatedAt) {
        DemoOutcome.Failed(DemoCodes.SCH_CONFLICT, "The schedule changed in the meantime")
    } else {
        DemoScheduleRules.plan(fields, current, entities, today()).then { plan ->
            val version = version(current.updatedAt)
            val built = build(current.id, plan, current.createdAt, version, current.lastRun)
            val reopened = fields.containsKey("repeat") || (plan.enabled && !current.enabled)
            if (reopened) checked(built) else DemoOutcome.Ok(built)
        }
    }

    /** The next runs of the draft in [fields] of `schedules/preview`: ISO times, none for a draft that cannot run. */
    fun preview(fields: JsonObject): DemoOutcome<List<String>> =
        DemoScheduleRules.trigger(fields, today()).then { plan ->
            checked(build(PREVIEW_ID, plan, "", "", null)).then { DemoOutcome.Ok(planned(it).nextRuns) }
        }

    /** [schedule], or HAB-SCH-009 if its date rule leaves no run in the future. */
    private fun checked(schedule: DemoSchedule): DemoOutcome<DemoSchedule> =
        if (runs(schedule, 1).isEmpty()) {
            DemoOutcome.Failed(DemoCodes.SCH_DATE_PAST, "The date or time is in the past")
        } else {
            DemoOutcome.Ok(schedule)
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
     * The planned runs of [schedule] as ISO times with offset, computed in the time zone of the phone from its date
     * rule, the weekdays and the time (sun events: the fixed demo times plus the offset); none while it is off.
     */
    fun planned(schedule: DemoSchedule): Planned {
        val all = runs(schedule, NEXT_RUNS)
        val shown = if (schedule.enabled) all.map { DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(it) } else emptyList()
        return Planned(shown.firstOrNull(), shown, expired = all.isEmpty())
    }

    /** The first [count] runs after now that the date rule hits from its start date on, on or off. */
    private fun runs(schedule: DemoSchedule, count: Int): List<ZonedDateTime> {
        val from = maxOf(today().minusDays(1), schedule.repeat.startDate?.let(LocalDate::parse) ?: today())
        return (0L..SEARCH_DAYS).asSequence()
            .map { runOn(schedule, from.plusDays(it), clock.zone) }
            .filter { it.toInstant().isAfter(clock.instant()) }
            .filter { DemoRecurrence.matches(schedule.repeat, schedule.days, it.toLocalDate()) }
            .take(count)
            .toList()
    }

    /** Today's date in the time zone of the phone. */
    private fun today(): LocalDate = LocalDate.now(clock)

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
            plan.entities, createdAt, updatedAt, lastRun, plan.repeat, plan.notify,
        )

    /** A version token after [previous]: the current time, at least a millisecond later than [previous]. */
    private fun version(previous: String?): String {
        val now = clock.instant()
        val earliest = previous?.let { Instant.parse(it).plusMillis(1) }
        return DemoTimes.iso(maxOf(now, earliest ?: now).toEpochMilli())
    }

    /** Search range of the next runs in days, their number and the fixed sun times of the demo (concept 20.3). */
    private companion object {
        const val SEARCH_DAYS = 800L
        const val NEXT_RUNS = 3
        const val PREVIEW_ID = "preview"
        val SUNRISE: LocalTime = LocalTime.of(6, 30)
        val SUNSET: LocalTime = LocalTime.of(19, 30)
    }
}
