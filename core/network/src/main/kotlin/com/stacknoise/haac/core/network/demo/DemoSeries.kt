package com.stacknoise.haac.core.network.demo

import java.time.Clock
import java.time.Instant
import kotlin.math.PI
import kotlin.math.sin

/**
 * The generated curves behind the history of the demo (concept 20.3): pure functions of the entity ID and the
 * time, so the same window always shows the same series and nothing has to be stored.
 */
internal class DemoSeries(private val clock: Clock) {
    /** A measurement such as a temperature: a daily curve around 21 with a little deterministic noise. */
    fun measurement(entity: DemoEntity, at: Long): Double =
        round(MEAN_TEMPERATURE + SWING * dayCurve(at) + noise(entity.entityId, at))

    /** The thermostat's current temperature: a smaller daily curve around 20. */
    fun climateCurrent(entity: DemoEntity, at: Long): Double =
        round(MEAN_CLIMATE + CLIMATE_SWING * dayCurve(at) + noise(entity.entityId, at))

    /** The thermostat's target: 21 °C by day, 18 °C at night (local time). */
    fun climateTarget(at: Long): Double = if (hourOfDay(at) in DAY_START..DAY_END) DAY_TARGET else NIGHT_TARGET

    /** A rising counter such as the energy meter: [KWH_PER_HOUR] per hour since a fixed start. */
    fun counter(at: Long): Double = round(COUNTER_START + (at - COUNTER_EPOCH) / MS_PER_HOUR * KWH_PER_HOUR)

    /** True if the switch is on in the two-hour slot of [at]; about two slots in five are on. */
    fun switchOn(entity: DemoEntity, at: Long): Boolean = mix(entity.entityId, at / SLOT_MS) % SLOT_ODDS < SLOT_ON

    /** Deterministic noise of at most 0.3 for the half hour of [at]. */
    private fun noise(id: String, at: Long): Double =
        (mix(id, at / HALF_HOUR_MS) % NOISE_STEPS - NOISE_STEPS / 2) * NOISE

    /** A curve between -1 and 1 with its high at 15:00 local time. */
    private fun dayCurve(at: Long): Double = sin(2 * PI * (hourOfDay(at) - PEAK_HOUR + QUARTER_DAY) / HOURS_PER_DAY)

    /** The local hour of [at] with its fraction. */
    private fun hourOfDay(at: Long): Double {
        val local = Instant.ofEpochMilli(at).atZone(clock.zone)
        return local.hour + local.minute / MINUTES_PER_HOUR
    }

    /** A non-negative number that depends only on [id] and [slot] (`String.hashCode` is fixed by the language). */
    private fun mix(id: String, slot: Long): Int = Math.floorMod(id.hashCode() * HASH_FACTOR + slot.toInt(), MIX_RANGE)

    /** [value] with two decimals. */
    private fun round(value: Double): Double = Math.round(value * HUNDRED) / HUNDRED

    /** Shape of the curves and of the switch slots. */
    private companion object {
        const val MEAN_TEMPERATURE = 21.0
        const val SWING = 2.5
        const val MEAN_CLIMATE = 20.0
        const val CLIMATE_SWING = 1.2
        const val PEAK_HOUR = 15.0
        const val QUARTER_DAY = 6.0
        const val HOURS_PER_DAY = 24.0
        const val MINUTES_PER_HOUR = 60.0
        const val DAY_START = 6.0
        const val DAY_END = 21.99
        const val DAY_TARGET = 21.0
        const val NIGHT_TARGET = 18.0
        const val NOISE = 0.1
        const val NOISE_STEPS = 7
        const val COUNTER_START = 1000.0
        const val COUNTER_EPOCH = 1_767_225_600_000L
        const val KWH_PER_HOUR = 0.4
        const val MS_PER_HOUR = 3_600_000.0
        const val HALF_HOUR_MS = 1_800_000L
        const val SLOT_MS = 7_200_000L
        const val SLOT_ODDS = 5
        const val SLOT_ON = 2
        const val HASH_FACTOR = 31
        const val MIX_RANGE = 1_000_003
        const val HUNDRED = 100.0
    }
}
