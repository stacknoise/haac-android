package com.stacknoise.haac.feature.entities.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** The control a tile offers (concept 8, M-05); built only by [EntityControlFactory]. */
sealed interface EntityControl {
    /** A switch that is [on] or off; a tap turns it to the other state (concept 8.2). */
    data class Toggle(val on: Boolean) : EntityControl {
        /** The request for the other state. */
        fun flipped(): ControlRequest = ControlRequest.SwitchTo(!on)
    }

    /** The target temperature with − and + in steps of [step] between [min] and [max] (concept 8.4). */
    data class TargetTemperature(val target: Double, val min: Double, val max: Double, val step: Double) :
        EntityControl {
        /** The request for one step up ([up]) or down, kept within [min] and [max]; null if already at the limit. */
        fun stepped(up: Boolean): ControlRequest? {
            val next = (if (up) target + step else target - step).coerceIn(min, max)
            val rounded = BigDecimal.valueOf(next).setScale(DECIMALS, RoundingMode.HALF_UP).toDouble()
            return if (rounded == target) null else ControlRequest.SetTemperature(rounded)
        }

        /** Where [target] lies between [min] and [max], from 0 to 1 (arc of the tile). */
        val fraction: Float
            get() = if (max > min) ((target - min) / (max - min)).coerceIn(0.0, 1.0).toFloat() else 0f

        /** Digits kept after a step, so 0.1 steps do not add up to 21.299999. */
        private companion object {
            const val DECIMALS = 2
        }
    }
}
