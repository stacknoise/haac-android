package com.stacknoise.haac.feature.entities.domain

import java.util.concurrent.TimeUnit

/** The period choices of the history (concept 8.1). */
enum class HistoryPreset {
    DAY,
    WEEK,
    CUSTOM,
}

/** The shown period from [start] to [end] in milliseconds, chosen with [preset]. */
data class HistoryWindow(val start: Long, val end: Long, val preset: HistoryPreset) {
    /** Length of the period in milliseconds. */
    val length: Long get() = end - start
}

/** Resolution of long-term statistics (concept 11.2); [key] is the bridge's `period`. */
enum class StatisticPeriod(val key: String, val millis: Long) {
    HOUR("hour", TimeUnit.HOURS.toMillis(1)),
    DAY("day", TimeUnit.DAYS.toMillis(1)),
}

/** What to ask the bridge for (concept 11.2). */
sealed interface HistoryQuery {
    /** `haac_bridge/history`; [attributes] false asks for a minimal response. */
    data class States(val attributes: Boolean) : HistoryQuery

    /**
     * `haac_bridge/statistics` with [period] and [types]; [lead] periods before the window are read too, so the
     * first bar of a counter has a value to subtract from.
     */
    data class Statistics(val period: StatisticPeriod, val types: List<String>, val lead: Int = 0) : HistoryQuery
}

/** A value at time [at]. */
data class ChartPoint(val at: Long, val value: Double)

/** Low and high of one statistics period starting at [at]. */
data class RangePoint(val at: Long, val low: Double, val high: Double)

/** What a line shows; decides its colour and legend entry. */
enum class SeriesKind {
    VALUE,
    MEAN,
    CURRENT,
    TARGET,
}

/** One line; [segments] break where HA had no number (e.g. `unavailable`). */
data class LineSeries(val kind: SeriesKind, val segments: List<List<ChartPoint>>) {
    /** Every point of all segments. */
    val points: List<ChartPoint> get() = segments.flatten()
}

/** What a climate entity did during a phase (`hvac_action`). */
enum class PhaseKind {
    HEATING,
    COOLING,
}

/** A period from [start] to [end]. */
data class Phase(val start: Long, val end: Long, val kind: PhaseKind)

/** A bar of a counter: the [value] used from [start] to [end]. */
data class Bar(val start: Long, val end: Long, val value: Double)

/** A period from [start] to [end] with HA's [state] (timeline). */
data class Segment(val start: Long, val end: Long, val state: String)

/** A chart of the detail screen's history (concept 8.1 – 8.4); built only by [HistoryChartFactory]. */
sealed interface HistoryChart {
    /** Start of the shown period. */
    val start: Long

    /** End of the shown period. */
    val end: Long

    /** True if there is nothing to draw. */
    val isEmpty: Boolean

    /** Numbers over time: one or more lines, a min/max [band] and the heating or cooling [phases]. */
    data class Line(
        override val start: Long,
        override val end: Long,
        val unit: String?,
        val series: List<LineSeries>,
        val band: List<RangePoint> = emptyList(),
        val phases: List<Phase> = emptyList(),
    ) : HistoryChart {
        override val isEmpty: Boolean get() = series.all { it.points.isEmpty() } && band.isEmpty()
    }

    /** Consumption per hour or day of a counter (`total`, `total_increasing`). */
    data class Bars(override val start: Long, override val end: Long, val unit: String?, val bars: List<Bar>) :
        HistoryChart {
        override val isEmpty: Boolean get() = bars.isEmpty()
    }

    /** States over time (switches, text and enum sensors). */
    data class Timeline(override val start: Long, override val end: Long, val segments: List<Segment>) :
        HistoryChart {
        override val isEmpty: Boolean get() = segments.isEmpty()
    }
}
