package com.stacknoise.haac.feature.schedules.domain

import com.stacknoise.haac.core.database.schedule.ScheduleEntity
import java.time.LocalTime
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** What a schedule does to its entities (concept 19.2). */
enum class ScheduleAction(val wire: String) {
    TURN_ON("turn_on"),
    TURN_OFF("turn_off"),
    TOGGLE("toggle"),
    ;

    /** Lookup by the bridge's name. */
    companion object {
        /** The action named [wire]; an unknown one from a newer bridge reads as toggle. */
        fun of(wire: String): ScheduleAction = entries.firstOrNull { it.wire == wire } ?: TOGGLE
    }
}

/** When a schedule runs: at a fixed time, or at sunrise or sunset with an offset (concept 19.1, 19.2). */
enum class WhenType(val wire: String) {
    TIME("time"),
    SUNRISE("sunrise"),
    SUNSET("sunset"),
    ;

    /** Lookup by the bridge's name. */
    companion object {
        /** The trigger named [wire]; an unknown one from a newer bridge reads as a fixed time. */
        fun of(wire: String): WhenType = entries.firstOrNull { it.wire == wire } ?: TIME
    }
}

/** The outcome of a schedule's last run (concept 19.2); [code] is the HAB code of a failure. */
data class LastRun(val at: Long, val result: String, val code: String?) {
    /** True if every entity was switched. */
    val ok: Boolean get() = result == RESULT_OK

    /** Result names of the bridge. */
    companion object {
        const val RESULT_OK = "ok"
    }
}

/**
 * One cached schedule as the screens use it (concept 19.2, 19.7). [days] are 0 (Monday) to 6 (Sunday), [time] is set
 * for fixed times and [offsetMin] for sun events. [updatedAt] is the bridge's own text, sent back with an edit.
 * [pausedReason] is set while the server has paused the schedule; [nextRun] is empty while it is off or paused.
 */
data class ScheduleItem(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val whenType: WhenType,
    val time: LocalTime?,
    val days: List<Int>,
    val offsetMin: Int,
    val action: ScheduleAction,
    val entityIds: List<String>,
    val own: Boolean,
    val ownerName: String?,
    val updatedAt: String,
    val pausedReason: String?,
    val lastRun: LastRun?,
    val nextRun: Long?,
) {
    /** True while the server has paused the schedule (concept 19.6). */
    val paused: Boolean get() = pausedReason != null

    /** True if the schedule is off or paused, so the card is muted (design 3b). */
    val muted: Boolean get() = !enabled || paused
}

/** The schedule of a cache row; a damaged entity list shows no entities. */
fun ScheduleEntity.toItem(): ScheduleItem = ScheduleItem(
    id = scheduleId,
    name = name,
    enabled = enabled,
    whenType = WhenType.of(whenType),
    time = time?.let(::parseTime),
    days = WeekDays.fromMask(days),
    offsetMin = offsetMin ?: 0,
    action = ScheduleAction.of(action),
    entityIds = decodeIds(entityIds),
    own = own,
    ownerName = ownerName,
    updatedAt = updatedAt,
    pausedReason = pausedReason,
    lastRun = lastRunAt?.let { LastRun(it, lastRunResult.orEmpty(), lastRunCode) },
    nextRun = nextRun,
)

/** `HH:MM` as a time of day, or null if it is malformed. */
private fun parseTime(text: String): LocalTime? = try {
    LocalTime.parse(text)
} catch (_: java.time.format.DateTimeParseException) {
    null
}

/** The entity ids of column `entity_ids`; a damaged value shows none. */
private fun decodeIds(json: String): List<String> = try {
    Json.decodeFromString(ListSerializer(String.serializer()), json)
} catch (_: IllegalArgumentException) {
    emptyList()
}
