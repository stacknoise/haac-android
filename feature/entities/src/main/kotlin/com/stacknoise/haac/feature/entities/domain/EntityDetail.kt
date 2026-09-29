package com.stacknoise.haac.feature.entities.domain

import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** A value always shown on the detail screen when HA reports it (concept 8.4). */
enum class ReadingKind(val attribute: String) {
    CURRENT_TEMPERATURE("current_temperature"),
    CURRENT_HUMIDITY("current_humidity"),
    HVAC_ACTION("hvac_action"),
}

/** One reading: [value] is formatted for numbers (21.4°, 48 %) and HA's raw text otherwise. */
data class Reading(val kind: ReadingKind, val value: String)

/** One attribute as HA reports it, with its value as text (lists joined by commas). */
data class Attribute(val name: String, val value: String)

/**
 * Everything the detail screen shows about one entity (concept 8.1, 15.4): the [tile] (name, value, rename),
 * HA's raw [state] (e.g. the HVAC mode), HA's and the bridge's names, the times of the last change and update,
 * [readings], all [attributes] and the [controls] of the domain.
 */
data class EntityDetail(
    val tile: Tile,
    val state: String?,
    val haName: String,
    val configuredName: String?,
    val lastChanged: Long?,
    val lastUpdated: Long?,
    val readings: List<Reading>,
    val attributes: List<Attribute>,
    val controls: List<EntityControl>,
)

/** The readings HA reports in [attributes], in the order of [ReadingKind]. */
internal fun readings(attributes: JsonObject): List<Reading> = ReadingKind.entries.mapNotNull { kind ->
    val value = attributes[kind.attribute] as? JsonPrimitive ?: return@mapNotNull null
    val number = attributes.number(kind.attribute)
    val text = when {
        number == null -> value.contentOrNull
        kind == ReadingKind.CURRENT_TEMPERATURE -> degrees(number)
        else -> oneDecimal(number).removeSuffix(".0") + " %"
    }
    text?.let { Reading(kind, it) }
}

/** All attributes sorted by name; JSON null is left out. */
internal fun attributes(attributes: JsonObject): List<Attribute> = attributes
    .filterValues { it !is JsonNull }
    .map { (name, value) -> Attribute(name, text(value)) }
    .sortedBy { it.name }

/** A value as text: plain content, list items joined by commas, objects as JSON. */
private fun text(value: JsonElement): String = when (value) {
    is JsonPrimitive -> value.content
    is JsonArray -> value.joinToString(", ", transform = ::text)
    is JsonObject -> value.toString()
}

/** [value] with one decimal. */
private fun oneDecimal(value: Double): String =
    BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toPlainString()

/** A temperature with one decimal, e.g. 21.5°. */
internal fun degrees(value: Double): String = oneDecimal(value) + "°"
