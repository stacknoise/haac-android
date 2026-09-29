package com.stacknoise.haac.feature.entities.domain

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * A line of the numeric states; a state that is not a number (e.g. `unavailable`) ends the current segment, so
 * the line shows a gap there.
 */
internal fun valueLine(states: List<RecordedState>, window: HistoryWindow, unit: String?): HistoryChart.Line {
    val segments = segmentsOf(states.map { it.at to it.state.toDoubleOrNull() }, window)
    return HistoryChart.Line(window.start, window.end, unit, listOf(LineSeries(SeriesKind.VALUE, segments)))
}

/** Current and target temperature of a climate entity with its heating and cooling phases (concept 8.4). */
internal fun climateLine(states: List<RecordedState>, window: HistoryWindow): HistoryChart.Line {
    /** The line of [attribute]. */
    fun series(kind: SeriesKind, attribute: String) =
        LineSeries(kind, segmentsOf(states.map { it.at to it.attributes.number(attribute) }, window))
    return HistoryChart.Line(
        start = window.start,
        end = window.end,
        unit = "°",
        series = listOf(series(SeriesKind.CURRENT, "current_temperature"), series(SeriesKind.TARGET, "temperature")),
        phases = phases(states, window),
    )
}

/** States as segments from each change to the next; repeated states are merged (switches, enums). */
internal fun timeline(states: List<RecordedState>, window: HistoryWindow): HistoryChart.Timeline {
    val changes = states.filterIndexed { index, state -> index == 0 || state.state != states[index - 1].state }
    val segments = changes.mapIndexedNotNull { index, state ->
        val start = state.at.coerceAtLeast(window.start)
        val end = (changes.getOrNull(index + 1)?.at ?: window.end).coerceAtMost(window.end)
        if (end > start) Segment(start, end, state.state) else null
    }
    return HistoryChart.Timeline(window.start, window.end, segments)
}

/** Mean per period as a line with the min/max band (long ranges of measurements, concept 8.3). */
internal fun statisticsLine(rows: List<StatisticRow>, window: HistoryWindow, unit: String?): HistoryChart.Line {
    val shown = rows.filter { it.start >= window.start }
    val mean = shown.mapNotNull { row -> row.mean?.let { ChartPoint(row.start, it) } }
    val band = shown.mapNotNull { row ->
        val low = row.min ?: return@mapNotNull null
        val high = row.max ?: return@mapNotNull null
        RangePoint(row.start, low, high)
    }
    val series = listOf(LineSeries(SeriesKind.MEAN, if (mean.isEmpty()) emptyList() else listOf(mean)))
    return HistoryChart.Line(window.start, window.end, unit, series, band)
}

/**
 * Consumption per period of a counter: the growth of `sum` from the period before (concept 8.3); the rows
 * start one period early for that.
 */
internal fun counterBars(rows: List<StatisticRow>, window: HistoryWindow, unit: String?): HistoryChart.Bars {
    val bars = rows.zipWithNext().mapNotNull { (before, row) ->
        val used = row.sum?.let { sum -> before.sum?.let { sum - it } } ?: return@mapNotNull null
        if (row.start < window.start) null else Bar(row.start, row.end, used)
    }
    return HistoryChart.Bars(window.start, window.end, unit, bars)
}

/**
 * Points in time order split into segments at every null value. HA records only changes, so each value holds
 * until the next state: a segment ends at the time of the null value, the last one at the end of the window.
 */
private fun segmentsOf(values: List<Pair<Long, Double?>>, window: HistoryWindow): List<List<ChartPoint>> {
    val segments = mutableListOf<List<ChartPoint>>()
    var current = mutableListOf<ChartPoint>()
    /** Ends the current segment at [at] with its last value. */
    fun close(at: Long) {
        current.lastOrNull()?.let { last -> segments += current + ChartPoint(clamp(at, window), last.value) }
        current = mutableListOf()
    }
    values.forEach { (at, value) ->
        if (value == null) close(at) else current += ChartPoint(clamp(at, window), value)
    }
    close(window.end)
    return segments
}

/** [at] within the window. */
private fun clamp(at: Long, window: HistoryWindow): Long = at.coerceIn(window.start, window.end)

/** Heating and cooling phases from `hvac_action`; a phase lasts until the action changes. */
private fun phases(states: List<RecordedState>, window: HistoryWindow): List<Phase> {
    val actions = states.map { it.at to (it.attributes["hvac_action"] as? JsonPrimitive)?.contentOrNull }
    val changes = actions.filterIndexed { index, (_, action) -> index == 0 || action != actions[index - 1].second }
    return changes.mapIndexedNotNull { index, (at, action) ->
        val kind = PhaseActions[action] ?: return@mapIndexedNotNull null
        val end = clamp(changes.getOrNull(index + 1)?.first ?: window.end, window)
        clamp(at, window).takeIf { it < end }?.let { Phase(it, end, kind) }
    }
}

/** `hvac_action` values drawn as phases. */
private val PhaseActions = mapOf("heating" to PhaseKind.HEATING, "cooling" to PhaseKind.COOLING)
