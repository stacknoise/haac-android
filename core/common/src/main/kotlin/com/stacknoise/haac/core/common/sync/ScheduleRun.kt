package com.stacknoise.haac.core.common.sync

/** The result of one run of a schedule as the run log tells it (concept 19.11). */
enum class RunResult {
    OK,
    PARTIAL,
    FAILED,
    MISSED,
}

/**
 * One run of an own schedule that the user wants to see in the notification list (concept 19.11): the schedule
 * [scheduleId] with its [name] at the time of the run, the time [at] in epoch milliseconds and the [result].
 * [removed] tells that the run deleted a one-off or ending schedule.
 */
data class ScheduleRun(
    val scheduleId: String,
    val name: String,
    val at: Long,
    val result: RunResult,
    val removed: Boolean,
)
