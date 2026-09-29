package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.entity.ExposedEntity
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject

/**
 * Decides per entity which history to read and how to draw it (concept 8.1 – 8.4, 17.2): the only place that
 * branches on domain and `state_class` for the history.
 */
interface HistoryChartFactory {
    /** What to ask the bridge for to show [entity] in [window]. */
    fun query(entity: ExposedEntity, window: HistoryWindow): HistoryQuery

    /** The chart of [entity] in [window] from the bridge's [result] of [query]. */
    fun create(entity: ExposedEntity, window: HistoryWindow, query: HistoryQuery, result: JsonObject): HistoryChart
}

/**
 * [HistoryChartFactory] of v1: switches and text sensors as a timeline; measurements as a line, beyond two days
 * from hourly statistics (beyond 31 days daily) with min/max; counters as bars per hour, beyond two days per day;
 * climate entities as current vs. target temperature with heating and cooling phases.
 */
class DefaultHistoryChartFactory @Inject constructor() : HistoryChartFactory {
    /** States for switches, climate, text and short ranges; statistics for counters and long ranges. */
    override fun query(entity: ExposedEntity, window: HistoryWindow): HistoryQuery = when (kindOf(entity)) {
        ChartKind.CLIMATE -> HistoryQuery.States(attributes = true)
        ChartKind.COUNTER -> HistoryQuery.Statistics(period(window, STATES_UP_TO_MS), listOf(SUM), lead = 1)
        ChartKind.MEASUREMENT -> if (window.length > STATES_UP_TO_MS) {
            HistoryQuery.Statistics(period(window, HOURS_UP_TO_MS), listOf(MEAN, MIN, MAX))
        } else {
            HistoryQuery.States(attributes = false)
        }
        ChartKind.NUMBER, ChartKind.TIMELINE -> HistoryQuery.States(attributes = false)
    }

    /** Builds the chart that fits [query]. */
    override fun create(entity: ExposedEntity, window: HistoryWindow, query: HistoryQuery, result: JsonObject) =
        when (query) {
            is HistoryQuery.Statistics -> {
                val rows = statisticRows(result, entity.entityId)
                if (kindOf(entity) == ChartKind.COUNTER) {
                    counterBars(rows, window, entity.unit)
                } else {
                    statisticsLine(rows, window, entity.unit)
                }
            }
            is HistoryQuery.States -> {
                val states = recordedStates(result, entity.entityId)
                when (kindOf(entity)) {
                    ChartKind.CLIMATE -> climateLine(states, window)
                    ChartKind.TIMELINE -> timeline(states, window)
                    else -> valueLine(states, window, entity.unit)
                }
            }
        }

    /** How [entity] is drawn: by domain, then `state_class`, then whether it has a unit. */
    private fun kindOf(entity: ExposedEntity): ChartKind = when {
        entity.domain == EntityDomains.CLIMATE -> ChartKind.CLIMATE
        entity.domain != EntityDomains.SENSOR -> ChartKind.TIMELINE
        entity.stateClass in COUNTERS -> ChartKind.COUNTER
        entity.stateClass == "measurement" -> ChartKind.MEASUREMENT
        entity.unit != null -> ChartKind.NUMBER
        else -> ChartKind.TIMELINE
    }

    /** Hourly statistics up to [hoursUpTo], daily ones beyond. */
    private fun period(window: HistoryWindow, hoursUpTo: Long): StatisticPeriod =
        if (window.length > hoursUpTo) StatisticPeriod.DAY else StatisticPeriod.HOUR

    /** The chart forms of v1. */
    private enum class ChartKind { CLIMATE, COUNTER, MEASUREMENT, NUMBER, TIMELINE }

    /** Thresholds and statistic types. */
    private companion object {
        /** Up to two days the states are read; longer ranges use hourly or daily statistics. */
        const val STATES_UP_TO_MS = 48 * 3_600_000L

        /** Measurements use hourly statistics up to 31 days, daily ones beyond. */
        const val HOURS_UP_TO_MS = 31 * 86_400_000L
        const val MEAN = "mean"
        const val MIN = "min"
        const val MAX = "max"
        const val SUM = "sum"
        val COUNTERS = setOf("total", "total_increasing")
    }
}
