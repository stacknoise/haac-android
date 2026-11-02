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

/** One of the upcoming runs (concept 19.12): ISO time, local date and whether the user skipped it. */
internal class DemoUpcoming(val at: String, val date: String, val skipped: Boolean)

/**
 * The planned runs of a schedule as the bridge reports them (concept 19.10, 19.12): ISO times, whether none is left
 * and the [upcoming] runs including the skipped ones.
 */
internal class Planned(
    val nextRun: String?,
    val nextRuns: List<String>,
    val expired: Boolean,
    val upcoming: List<DemoUpcoming> = emptyList(),
)

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
    ): DemoOutcome<DemoSchedule> = DemoScheduleRules.plan(fields, null, entities, LocalDate.now(clock)).then { plan ->
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
        DemoScheduleRules.plan(fields, current, entities, LocalDate.now(clock)).then { plan ->
            val version = version(current.updatedAt)
            val keep = if (fields.containsKey("when") || fields.containsKey("repeat")) emptyList() else current.skipped
            val built = build(current.id, plan, current.createdAt, version, current.lastRun).copy(skipped = keep)
            val reopened = fields.containsKey("repeat") || (plan.enabled && !current.enabled)
            if (reopened) checked(built) else DemoOutcome.Ok(built)
        }
    }

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

    /** The next runs of the draft in [fields] of `schedules/preview`: ISO times, none for a draft that cannot run. */
    fun preview(fields: JsonObject): DemoOutcome<List<String>> =
        DemoScheduleRules.trigger(fields, LocalDate.now(clock)).then { plan ->
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
        val all = runs(schedule, UPCOMING_LIMIT)
        val skipped = schedule.skipped.toSet()
        val happening = all.filter { it.toLocalDate().toString() !in skipped }.take(NEXT_RUNS)
        val shown = if (schedule.enabled) happening.map(::iso) else emptyList()
        val upcoming = if (schedule.enabled) upcoming(all, skipped) else emptyList()
        return Planned(shown.firstOrNull(), shown, expired = all.isEmpty(), upcoming = upcoming)
    }

    /** The runs up to the [NEXT_RUNS]-th that happens, with the skipped ones between them (concept 19.12). */
    private fun upcoming(all: List<ZonedDateTime>, skipped: Set<String>): List<DemoUpcoming> {
        var happening = 0
        return all.asSequence()
            .takeWhile { happening < NEXT_RUNS }
            .onEach { if (it.toLocalDate().toString() !in skipped) happening++ }
            .map { DemoUpcoming(iso(it), it.toLocalDate().toString(), it.toLocalDate().toString() in skipped) }
            .toList()
    }

    /** [time] as an ISO time with offset. */
    private fun iso(time: ZonedDateTime): String = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time)

    /** The first [count] runs after now that the date rule hits from its start date on, on or off. */
    private fun runs(schedule: DemoSchedule, count: Int): List<ZonedDateTime> {
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
        const val UPCOMING_LIMIT = 14
        const val PREVIEW_ID = "preview"
        val SUNRISE: LocalTime = LocalTime.of(6, 30)
        val SUNSET: LocalTime = LocalTime.of(19, 30)
    }
}
