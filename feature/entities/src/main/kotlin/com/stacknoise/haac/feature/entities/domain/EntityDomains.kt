package com.stacknoise.haac.feature.entities.domain

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/** Domains, states and attribute names of v1 that the factories share (concept 1.2, 8). */
internal object EntityDomains {
    const val SWITCH = "switch"
    const val SENSOR = "sensor"
    const val CLIMATE = "climate"
    const val ON = "on"
    const val OFF = "off"
    const val TEMPERATURE = "temperature"
    const val TARGET_LOW = "target_temp_low"
    const val TARGET_HIGH = "target_temp_high"
    const val HUMIDITY = "humidity"
    const val TIMESTAMP = "timestamp"

    /** States that mean HA has no usable state (concept 8.1). */
    val UNAVAILABLE = setOf("unavailable", "unknown")

    /** `supported_features` flags of a climate entity (concept 8.4). */
    const val CLIMATE_TARGET_TEMPERATURE = 1
    const val CLIMATE_TARGET_RANGE = 2
    const val CLIMATE_TARGET_HUMIDITY = 4
    const val CLIMATE_FAN_MODE = 8
    const val CLIMATE_PRESET_MODE = 16
    const val CLIMATE_SWING_MODE = 32
    const val CLIMATE_TURN_OFF = 128
    const val CLIMATE_TURN_ON = 256
    const val CLIMATE_SWING_HORIZONTAL = 512
}

/** True if [features] contains [flag]. */
internal fun hasFlag(features: Int, flag: Int): Boolean = features and flag != 0

/** Attribute [key] as a number, or null. */
internal fun JsonObject.number(key: String): Double? = (get(key) as? JsonPrimitive)?.doubleOrNull
