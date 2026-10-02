package com.stacknoise.haac.core.network.demo

import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The answers of `haac_bridge/history` and `haac_bridge/statistics` for the demo (concept 20.3): deterministic
 * series from [DemoSeries] in HA's compressed state format and with statistics rows in milliseconds. Nothing
 * after "now" is recorded.
 */
internal class DemoHistory(private val clock: Clock, private val series: DemoSeries = DemoSeries(clock)) {
    /** The states of [entity] from [start] to [end] (ms), oldest first; each row is `s` with `lc` in seconds. */
    fun history(entity: DemoEntity, start: Long, end: Long, minimal: Boolean): JsonArray {
        val until = minOf(end, clock.millis())
        if (until <= start) return JsonArray(emptyList())
        val rows = when {
            entity.domain == "climate" -> sampled(start, until) { climateRow(entity, it) }
            entity.unit != null -> sampled(start, until) { valueRow(entity, it) }
            else -> switchRows(entity, start, until, minimal)
        }
        return JsonArray(rows)
    }

    /** The statistics rows of [entity] per [period] (hour, day, week or month) with the requested [types]. */
    fun statistics(entity: DemoEntity, start: Long, end: Long, period: String, types: List<String>): JsonArray {
        if (entity.unit == null) return JsonArray(emptyList())
        val until = minOf(end, clock.millis())
        val rows = buckets(start, until, period).map { (from, to) -> statisticRow(entity, from, to, types) }
        return JsonArray(rows)
    }

    /** One row of statistics: `mean`, `min` and `max` of a measurement or the `sum` of a counter. */
    private fun statisticRow(entity: DemoEntity, from: Long, to: Long, types: List<String>): JsonObject {
        val counter = entity.stateClass == "total_increasing" || entity.stateClass == "total"
        val mean = if (counter) 0.0 else series.measurement(entity, (from + to) / 2)
        return buildJsonObject {
            put("start", from)
            put("end", to)
            types.forEach { type ->
                when (type) {
                    "mean" -> put(type, mean)
                    "min" -> put(type, mean - SPREAD)
                    "max" -> put(type, mean + SPREAD)
                    "sum" -> put(type, series.counter(to))
                }
            }
        }
    }

    /** The period buckets (start, end) that begin at or after [start] and end by [until], at most [MAX_ROWS]. */
    private fun buckets(start: Long, until: Long, period: String): List<Pair<Long, Long>> {
        val result = mutableListOf<Pair<Long, Long>>()
        var from = firstBucket(start, period)
        while (result.size < MAX_ROWS) {
            val to = next(from, period)
            if (to.toInstant().toEpochMilli() > until) break
            result += from.toInstant().toEpochMilli() to to.toInstant().toEpochMilli()
            from = to
        }
        return result
    }

    /** The first bucket that starts at or after [start]. */
    private fun firstBucket(start: Long, period: String): ZonedDateTime {
        val at = Instant.ofEpochMilli(start).atZone(clock.zone)
        val floor = when (period) {
            "day" -> at.truncatedTo(ChronoUnit.DAYS)
            "week" -> at.truncatedTo(ChronoUnit.DAYS).minusDays((at.dayOfWeek.value - 1).toLong())
            "month" -> at.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1)
            else -> at.truncatedTo(ChronoUnit.HOURS)
        }
        return if (floor.toInstant().toEpochMilli() >= start) floor else next(floor, period)
    }

    /** The start of the bucket after [from]. */
    private fun next(from: ZonedDateTime, period: String): ZonedDateTime = when (period) {
        "day" -> from.plusDays(1)
        "week" -> from.plusWeeks(1)
        "month" -> from.plusMonths(1)
        else -> from.plusHours(1)
    }

    /** One state row every half hour on the grid, the first at [start]. */
    private fun sampled(start: Long, until: Long, row: (Long) -> JsonObject): List<JsonObject> {
        val grid = generateSequence((start / STEP_MS + 1) * STEP_MS) { it + STEP_MS }.takeWhile { it < until }
        return (sequenceOf(start) + grid).map(row).toList()
    }

    /** A number state such as a temperature at [at]. */
    private fun valueRow(entity: DemoEntity, at: Long): JsonObject {
        val counter = entity.stateClass == "total_increasing" || entity.stateClass == "total"
        val value = if (counter) series.counter(at) else series.measurement(entity, at)
        return buildJsonObject {
            put("s", String.format(Locale.ROOT, "%.2f", value))
            put("lc", DemoTimes.seconds(at))
        }
    }

    /** The thermostat at [at]: mode `heat` with the current and target temperature and what it is doing. */
    private fun climateRow(entity: DemoEntity, at: Long): JsonObject {
        val current = series.climateCurrent(entity, at)
        val target = series.climateTarget(at)
        return buildJsonObject {
            put("s", "heat")
            put(
                "a",
                buildJsonObject {
                    put("current_temperature", current)
                    put("temperature", target)
                    put("hvac_action", if (current < target) "heating" else "idle")
                },
            )
            put("lc", DemoTimes.seconds(at))
        }
    }

    /** The on and off periods of a switch: a row at [start] and one wherever the state changes in a slot. */
    private fun switchRows(entity: DemoEntity, start: Long, until: Long, minimal: Boolean): List<JsonObject> {
        val slots = generateSequence((start / SLOT_MS + 1) * SLOT_MS) { it + SLOT_MS }.takeWhile { it < until }
        val rows = mutableListOf<JsonObject>()
        var last: Boolean? = null
        (sequenceOf(start) + slots).forEach { at ->
            val on = series.switchOn(entity, at)
            if (on != last) rows += switchRow(entity, on, at, minimal)
            last = on
        }
        return rows
    }

    /** One switch state at [at]; a full response carries the attributes too. */
    private fun switchRow(entity: DemoEntity, on: Boolean, at: Long, minimal: Boolean): JsonObject = buildJsonObject {
        put("s", if (on) "on" else "off")
        if (!minimal) put("a", DemoEntityWire.attributes(entity))
        put("lc", DemoTimes.seconds(at))
    }

    /** Spread of min and max around the mean, row limit and grid steps. */
    private companion object {
        const val SPREAD = 0.4
        const val MAX_ROWS = 2_000
        const val STEP_MS = 1_800_000L
        const val SLOT_MS = 7_200_000L
    }
}
