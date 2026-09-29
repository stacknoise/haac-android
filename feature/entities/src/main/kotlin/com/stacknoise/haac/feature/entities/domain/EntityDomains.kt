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

    /** States that mean HA has no usable state (concept 8.1). */
    val UNAVAILABLE = setOf("unavailable", "unknown")

    /** `supported_features` flag of a climate entity with a single target temperature (concept 8.4). */
    const val CLIMATE_TARGET_TEMPERATURE = 1
}

/** Attribute [key] as a number, or null. */
internal fun JsonObject.number(key: String): Double? = (get(key) as? JsonPrimitive)?.doubleOrNull
