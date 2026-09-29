package com.stacknoise.haac.feature.entities.domain

import kotlin.math.abs
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * A change the user asks for on a control (concept 8). It knows the state it expects, so the tile can show it
 * before HA confirms it (optimistic UI, 8.1); [ServiceCallFactory] turns it into a service call.
 */
sealed interface ControlRequest {
    /** [state] as it looks once HA carried out the request. */
    fun applyTo(state: EntityState): EntityState

    /** True if [state] from HA already shows the result of the request. */
    fun isConfirmedBy(state: EntityState): Boolean

    /** True for values set by dragging or repeated taps; they are sent after a pause (concept 8.4). */
    val debounced: Boolean get() = false

    /** Turn a switch on or off (concept 8.2). */
    data class SwitchTo(val on: Boolean) : ControlRequest {
        /** The state becomes `on` or `off`. */
        override fun applyTo(state: EntityState): EntityState = state.copy(state = target)

        /** Confirmed once HA reports the target state. */
        override fun isConfirmedBy(state: EntityState): Boolean = state.state == target

        /** `on` or `off`. */
        private val target: String get() = if (on) EntityDomains.ON else EntityDomains.OFF
    }

    /** Turn a climate entity on (HA picks the mode) or off (concept 8.4, flags 256 and 128). */
    data class PowerTo(val on: Boolean) : ControlRequest {
        /** Off becomes `off`; on keeps the state, because HA decides the mode. */
        override fun applyTo(state: EntityState): EntityState =
            if (on) state else state.copy(state = EntityDomains.OFF)

        /** Confirmed once the state is `off`, or any other state when turning on. */
        override fun isConfirmedBy(state: EntityState): Boolean = (state.state == EntityDomains.OFF) != on
    }

    /** Set the HVAC mode, e.g. `heat` (concept 8.4). */
    data class SetHvacMode(val mode: String) : ControlRequest {
        /** The state becomes [mode]. */
        override fun applyTo(state: EntityState): EntityState = state.copy(state = mode)

        /** Confirmed once HA reports [mode]. */
        override fun isConfirmedBy(state: EntityState): Boolean = state.state == mode
    }

    /** Set the target temperature of a climate entity (concept 8.4). */
    data class SetTemperature(val target: Double) : ControlRequest {
        override val debounced: Boolean get() = true

        /** The attribute `temperature` becomes [target]. */
        override fun applyTo(state: EntityState): EntityState =
            state.withNumbers(EntityDomains.TEMPERATURE to target)

        /** Confirmed once HA reports [target] as the target temperature (22 and 22.0 are equal). */
        override fun isConfirmedBy(state: EntityState): Boolean =
            state.hasNumber(EntityDomains.TEMPERATURE, target)
    }

    /** Set the target range `target_temp_low` … `target_temp_high` (concept 8.4, flag 2). */
    data class SetTemperatureRange(val low: Double, val high: Double) : ControlRequest {
        override val debounced: Boolean get() = true

        /** Both range attributes become the new values. */
        override fun applyTo(state: EntityState): EntityState =
            state.withNumbers(EntityDomains.TARGET_LOW to low, EntityDomains.TARGET_HIGH to high)

        /** Confirmed once HA reports both values. */
        override fun isConfirmedBy(state: EntityState): Boolean =
            state.hasNumber(EntityDomains.TARGET_LOW, low) && state.hasNumber(EntityDomains.TARGET_HIGH, high)
    }

    /** Set the target humidity (concept 8.4, flag 4). */
    data class SetHumidity(val target: Double) : ControlRequest {
        override val debounced: Boolean get() = true

        /** The attribute `humidity` becomes [target]. */
        override fun applyTo(state: EntityState): EntityState = state.withNumbers(EntityDomains.HUMIDITY to target)

        /** Confirmed once HA reports [target]. */
        override fun isConfirmedBy(state: EntityState): Boolean = state.hasNumber(EntityDomains.HUMIDITY, target)
    }

    /** Set a fan, preset or swing mode (concept 8.4). */
    data class SetMode(val kind: ClimateMode, val mode: String) : ControlRequest {
        /** The attribute of [kind] becomes [mode]. */
        override fun applyTo(state: EntityState): EntityState =
            state.copy(attributes = JsonObject(state.attributes + (kind.attribute to JsonPrimitive(mode))))

        /** Confirmed once HA reports [mode]. */
        override fun isConfirmedBy(state: EntityState): Boolean =
            (state.attributes[kind.attribute] as? JsonPrimitive)?.content == mode
    }
}

/** Tolerance when comparing numbers from HA with requested ones. */
private const val Tolerance = 1e-6

/** This state with the number attributes of [values] replaced. */
private fun EntityState.withNumbers(vararg values: Pair<String, Double>): EntityState =
    copy(attributes = JsonObject(attributes + values.map { (key, value) -> key to JsonPrimitive(value) }))

/** True if attribute [key] is the number [value] (22 and 22.0 are equal). */
private fun EntityState.hasNumber(key: String, value: Double): Boolean =
    attributes.number(key)?.let { abs(it - value) < Tolerance } == true
