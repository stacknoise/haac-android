package com.stacknoise.haac.feature.entities.domain

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * The controls of a climate entity with [features] in [state] (concept 8.4); each one only with its flag and the
 * attributes HA reports for it. Limits HA leaves out use HA's defaults for °C.
 */
internal class ClimateControls(private val features: Int, private val state: EntityState) {
    /** Power, HVAC mode, target temperature or range, humidity, then the fan, preset and swing modes. */
    fun all(): List<EntityControl> =
        listOfNotNull(power(), hvacModes(), targetTemperature(), targetRange(), humidity()) +
            ClimateMode.entries.mapNotNull(::modes)

    /** − and + for the target temperature: flag 1 and a target from HA (none in mode `off`). */
    fun targetTemperature(): EntityControl.TargetTemperature? {
        val target = state.attributes.number(EntityDomains.TEMPERATURE)
            ?.takeIf { hasFlag(features, EntityDomains.CLIMATE_TARGET_TEMPERATURE) }
            ?: return null
        return EntityControl.TargetTemperature(target, minTemp(), maxTemp(), step())
    }

    /** The power button when at least one of flags 128 and 256 is set. */
    private fun power(): EntityControl.Power? {
        val canOff = hasFlag(features, EntityDomains.CLIMATE_TURN_OFF)
        val canOn = hasFlag(features, EntityDomains.CLIMATE_TURN_ON)
        if (!canOff && !canOn) return null
        return EntityControl.Power(on = state.state != EntityDomains.OFF, canTurnOn = canOn, canTurnOff = canOff)
    }

    /** The HVAC modes from `hvac_modes`, when HA lists more than one. */
    private fun hvacModes(): EntityControl.HvacModes? =
        texts(HVAC_MODES).takeIf { it.size > 1 }?.let { EntityControl.HvacModes(state.state, it) }

    /** The target range with flag 2 and both ends from HA. */
    private fun targetRange(): EntityControl.TargetRange? {
        val low = state.attributes.number(EntityDomains.TARGET_LOW)
        val high = state.attributes.number(EntityDomains.TARGET_HIGH)
        if (!hasFlag(features, EntityDomains.CLIMATE_TARGET_RANGE) || low == null || high == null) return null
        return EntityControl.TargetRange(low, high, minTemp(), maxTemp(), step())
    }

    /** The target humidity with flag 4 and a target from HA. */
    private fun humidity(): EntityControl.Humidity? {
        val target = state.attributes.number(EntityDomains.HUMIDITY)
            ?.takeIf { hasFlag(features, EntityDomains.CLIMATE_TARGET_HUMIDITY) }
            ?: return null
        return EntityControl.Humidity(
            target,
            state.attributes.number("min_humidity") ?: DEFAULT_MIN_HUMIDITY,
            state.attributes.number("max_humidity") ?: DEFAULT_MAX_HUMIDITY,
        )
    }

    /** The chips of [kind] with its flag and at least one choice. */
    private fun modes(kind: ClimateMode): EntityControl.Modes? {
        val choices = texts(kind.choices)
        if (!hasFlag(features, kind.flag) || choices.isEmpty()) return null
        return EntityControl.Modes(kind, (state.attributes[kind.attribute] as? JsonPrimitive)?.contentOrNull, choices)
    }

    /** The strings of list attribute [key]. */
    private fun texts(key: String): List<String> =
        (state.attributes[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()

    /** `min_temp`, or HA's default. */
    private fun minTemp(): Double = state.attributes.number("min_temp") ?: DEFAULT_MIN

    /** `max_temp`, or HA's default. */
    private fun maxTemp(): Double = state.attributes.number("max_temp") ?: DEFAULT_MAX

    /** `target_temp_step`, or 0.5. */
    private fun step(): Double = state.attributes.number("target_temp_step")?.takeIf { it > 0 } ?: DEFAULT_STEP

    /** Attribute names and HA's defaults of a climate entity in °C. */
    private companion object {
        const val HVAC_MODES = "hvac_modes"
        const val DEFAULT_MIN = 7.0
        const val DEFAULT_MAX = 35.0
        const val DEFAULT_STEP = 0.5
        const val DEFAULT_MIN_HUMIDITY = 30.0
        const val DEFAULT_MAX_HUMIDITY = 99.0
    }
}
