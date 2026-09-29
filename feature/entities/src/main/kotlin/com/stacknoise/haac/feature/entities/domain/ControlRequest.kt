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

    /** Turn a switch on or off (concept 8.2). */
    data class SwitchTo(val on: Boolean) : ControlRequest {
        /** The state becomes `on` or `off`. */
        override fun applyTo(state: EntityState): EntityState = state.copy(state = target)

        /** Confirmed once HA reports the target state. */
        override fun isConfirmedBy(state: EntityState): Boolean = state.state == target

        /** `on` or `off`. */
        private val target: String get() = if (on) EntityDomains.ON else EntityDomains.OFF
    }

    /** Set the target temperature of a climate entity (concept 8.4). */
    data class SetTemperature(val target: Double) : ControlRequest {
        /** The attribute `temperature` becomes [target]. */
        override fun applyTo(state: EntityState): EntityState =
            state.copy(attributes = JsonObject(state.attributes + (EntityDomains.TEMPERATURE to JsonPrimitive(target))))

        /** Confirmed once HA reports [target] as the target temperature (22 and 22.0 are equal). */
        override fun isConfirmedBy(state: EntityState): Boolean =
            state.attributes.number(EntityDomains.TEMPERATURE)?.let { abs(it - target) < TOLERANCE } == true

        /** Tolerance of the temperature comparison. */
        private companion object {
            const val TOLERANCE = 1e-6
        }
    }
}
