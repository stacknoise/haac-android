package com.stacknoise.haac.feature.entities.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** A control of a tile or the detail screen (concept 8, M-05); built only by [EntityControlFactory]. */
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
            val next = roundedStep(if (up) target + step else target - step).coerceIn(min, max)
            return if (next == target) null else ControlRequest.SetTemperature(next)
        }

        /** Where [target] lies between [min] and [max], from 0 to 1 (arc of the tile). */
        val fraction: Float
            get() = if (max > min) ((target - min) / (max - min)).coerceIn(0.0, 1.0).toFloat() else 0f
    }

    /**
     * The power button of a climate entity that is [on] (any state but `off`); [canTurnOn] and [canTurnOff]
     * follow the flags 256 and 128 (concept 8.4).
     */
    data class Power(val on: Boolean, val canTurnOn: Boolean, val canTurnOff: Boolean) : EntityControl {
        /** The request for the other state, or null if the entity does not support it. */
        fun flipped(): ControlRequest? = when {
            on && canTurnOff -> ControlRequest.PowerTo(on = false)
            !on && canTurnOn -> ControlRequest.PowerTo(on = true)
            else -> null
        }
    }

    /** The HVAC mode [current] out of [modes] (concept 8.4, always present). */
    data class HvacModes(val current: String, val modes: List<String>) : EntityControl

    /** The target range [low] … [high] between [min] and [max] in steps of [step] (concept 8.4, flag 2). */
    data class TargetRange(val low: Double, val high: Double, val min: Double, val max: Double, val step: Double) :
        EntityControl {
        /** The request for the range [from] … [to], rounded to [step] and kept in order. */
        fun request(from: Double, to: Double): ControlRequest.SetTemperatureRange {
            val first = snap(from, min, step).coerceIn(min, max)
            return ControlRequest.SetTemperatureRange(first, snap(to, min, step).coerceIn(first, max))
        }
    }

    /** The target humidity [target] between [min] and [max] in whole percent (concept 8.4, flag 4). */
    data class Humidity(val target: Double, val min: Double, val max: Double) : EntityControl {
        /** The request for [value], rounded to whole percent. */
        fun request(value: Double): ControlRequest =
            ControlRequest.SetHumidity(snap(value, min, 1.0).coerceIn(min, max))
    }

    /** A fan, preset or swing mode [current] out of [choices] (concept 8.4). */
    data class Modes(val kind: ClimateMode, val current: String?, val choices: List<String>) : EntityControl {
        /** The request for [mode]. */
        fun request(mode: String): ControlRequest = ControlRequest.SetMode(kind, mode)
    }
}

/** Digits kept after a step, so 0.1 steps do not add up to 21.299999. */
private const val Decimals = 2

/** [value] rounded to [Decimals] digits. */
private fun roundedStep(value: Double): Double =
    BigDecimal.valueOf(value).setScale(Decimals, RoundingMode.HALF_UP).toDouble()

/** [value] moved to the nearest step of [step] counted from [origin]. */
private fun snap(value: Double, origin: Double, step: Double): Double =
    roundedStep(origin + Math.round((value - origin) / step) * step)
