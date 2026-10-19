package com.stacknoise.haac.feature.schedules.data

/**
 * The optional bridge features the schedule screens depend on (concept 11.2): [repeat] for date rules and the
 * preview (19.10), [runs] for run notifications (19.11). Without them the app keeps to weekdays and sends no new field.
 */
data class ScheduleFeatures(val repeat: Boolean = false, val runs: Boolean = false) {
    /** Names of the feature flags. */
    companion object {
        const val REPEAT = "schedule_repeat"
        const val RUNS = "schedule_runs"

        /** The schedule features among the [reported] flags of the bridge. */
        fun of(reported: Set<String>): ScheduleFeatures = ScheduleFeatures(REPEAT in reported, RUNS in reported)
    }
}
