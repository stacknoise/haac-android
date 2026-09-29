package com.stacknoise.haac.feature.entities.domain

import kotlin.math.roundToLong
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/** One state of `haac_bridge/history`: since [at] (ms) the entity was [state] with [attributes]. */
internal data class RecordedState(val at: Long, val state: String, val attributes: JsonObject)

/** One row of `haac_bridge/statistics` (times in ms, values null when not requested or not recorded). */
internal data class StatisticRow(
    val start: Long,
    val end: Long,
    val mean: Double? = null,
    val min: Double? = null,
    val max: Double? = null,
    val sum: Double? = null,
)

/**
 * The states of [entityId] in a history result, oldest first (HA's compressed format, concept 11.3): `s` state,
 * `a` attributes (only in full responses), `lc` last changed and `lu` last updated in seconds; `lc` is left out
 * when it equals `lu`.
 */
internal fun recordedStates(result: JsonObject, entityId: String): List<RecordedState> =
    (result[entityId] as? JsonArray).orEmpty()
        .mapNotNull { row ->
            val item = row as? JsonObject ?: return@mapNotNull null
            val state = (item["s"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            val seconds = item.number("lc") ?: item.number("lu") ?: return@mapNotNull null
            RecordedState((seconds * MsPerSecond).roundToLong(), state, item["a"] as? JsonObject ?: NoAttributes)
        }
        .sortedBy { it.at }

/** The statistics rows of [entityId] in a statistics result, oldest first. */
internal fun statisticRows(result: JsonObject, entityId: String): List<StatisticRow> =
    (result[entityId] as? JsonArray).orEmpty()
        .mapNotNull { row ->
            val item = row as? JsonObject ?: return@mapNotNull null
            val start = (item["start"] as? JsonPrimitive)?.longOrNull ?: return@mapNotNull null
            val end = (item["end"] as? JsonPrimitive)?.longOrNull ?: return@mapNotNull null
            StatisticRow(start, end, item.value("mean"), item.value("min"), item.value("max"), item.value("sum"))
        }
        .sortedBy { it.start }

/** A number of a statistics row, or null (JSON null or missing). */
private fun JsonObject.value(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull

/** Milliseconds per second. */
private const val MsPerSecond = 1_000

/** Attributes of a minimal response entry. */
private val NoAttributes = JsonObject(emptyMap())
