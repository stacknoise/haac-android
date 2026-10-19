package com.stacknoise.haac.feature.entities.domain

import androidx.annotation.StringRes
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.EntityDomains.ON

/** How a binary sensor that is `on` stands out: [ACTIVE] like a switched-on switch, [ALARM] in the error colours. */
enum class BinaryLevel {
    ACTIVE,
    ALARM,
}

/** The words for `on` and `off` of one device class, the [level] at `on` and its [icon] (concept 8.6). */
data class BinaryClass(
    @param:StringRes val on: Int,
    @param:StringRes val off: Int,
    val level: BinaryLevel,
    val icon: TileIcon,
)

/** Shorthand for a row of the class table. */
private fun row(@StringRes on: Int, @StringRes off: Int, level: BinaryLevel, icon: TileIcon) =
    BinaryClass(on, off, level, icon)

/** The words of the classes that open and close. */
private val Open = row(R.string.binary_open, R.string.binary_closed, BinaryLevel.ACTIVE, TileIcon.DOOR)

/** The class used for no or an unknown `device_class`: *On* and *Off*, active. */
private val Plain = row(R.string.tile_on, R.string.tile_off, BinaryLevel.ACTIVE, TileIcon.BINARY)

/** The fixed text table of concept 8.6 by `device_class`; a `lock` that is `on` is unlocked, which is *Active*. */
private val BinaryTable: Map<String, BinaryClass> = mapOf(
    "door" to Open,
    "garage_door" to Open,
    "window" to Open,
    "opening" to Open,
    "lock" to row(R.string.binary_unlocked, R.string.binary_locked, BinaryLevel.ACTIVE, TileIcon.LOCK),
    "motion" to row(R.string.binary_motion, R.string.binary_no_motion, BinaryLevel.ACTIVE, TileIcon.MOTION),
    "moving" to row(R.string.binary_moving, R.string.binary_still, BinaryLevel.ACTIVE, TileIcon.MOTION),
    "occupancy" to row(R.string.binary_occupied, R.string.binary_clear, BinaryLevel.ACTIVE, TileIcon.MOTION),
    "presence" to row(R.string.binary_home, R.string.binary_away, BinaryLevel.ACTIVE, TileIcon.MOTION),
    "smoke" to row(R.string.binary_smoke, R.string.binary_no_smoke, BinaryLevel.ALARM, TileIcon.ALARM),
    "gas" to row(R.string.binary_gas, R.string.binary_no_gas, BinaryLevel.ALARM, TileIcon.ALARM),
    "carbon_monoxide" to row(R.string.binary_co, R.string.binary_no_co, BinaryLevel.ALARM, TileIcon.ALARM),
    "moisture" to row(R.string.binary_wet, R.string.binary_dry, BinaryLevel.ALARM, TileIcon.MOISTURE),
    "safety" to row(R.string.binary_unsafe, R.string.binary_safe, BinaryLevel.ALARM, TileIcon.ALARM),
    "tamper" to row(R.string.binary_tampered, R.string.binary_not_tampered, BinaryLevel.ALARM, TileIcon.ALARM),
    "problem" to row(R.string.binary_problem, R.string.binary_ok, BinaryLevel.ALARM, TileIcon.ALARM),
    "heat" to row(R.string.binary_hot, R.string.binary_normal, BinaryLevel.ALARM, TileIcon.ALARM),
    "cold" to row(R.string.binary_cold, R.string.binary_normal, BinaryLevel.ALARM, TileIcon.ALARM),
    "battery" to row(R.string.binary_low, R.string.binary_normal, BinaryLevel.ALARM, TileIcon.BATTERY),
    "battery_charging" to
        row(R.string.binary_charging, R.string.binary_not_charging, BinaryLevel.ACTIVE, TileIcon.BATTERY),
    "connectivity" to
        row(R.string.binary_connected, R.string.binary_disconnected, BinaryLevel.ACTIVE, TileIcon.BINARY),
    "plug" to row(R.string.binary_plugged_in, R.string.binary_unplugged, BinaryLevel.ACTIVE, TileIcon.POWER),
    "power" to row(R.string.binary_power, R.string.binary_no_power, BinaryLevel.ACTIVE, TileIcon.POWER),
    "running" to row(R.string.binary_running, R.string.binary_not_running, BinaryLevel.ACTIVE, TileIcon.BINARY),
    "light" to row(R.string.binary_light, R.string.binary_dark, BinaryLevel.ACTIVE, TileIcon.BINARY),
    "sound" to row(R.string.binary_sound, R.string.binary_quiet, BinaryLevel.ACTIVE, TileIcon.BINARY),
    "vibration" to row(R.string.binary_vibration, R.string.binary_no_vibration, BinaryLevel.ACTIVE, TileIcon.MOTION),
    "update" to row(R.string.binary_update, R.string.binary_up_to_date, BinaryLevel.ACTIVE, TileIcon.BINARY),
)

/** The class of a binary sensor with [deviceClass]; an unknown or missing class is *On* / *Off*, active. */
fun binaryClass(deviceClass: String?): BinaryClass = BinaryTable[deviceClass] ?: Plain

/**
 * The tile content of a binary sensor in state [raw] since [since]: the words of its class, and the level only
 * while it is `on` (`off`, `unavailable` and `unknown` stay neutral, concept 8.6).
 */
internal fun binaryContent(deviceClass: String?, raw: String?, since: Long?): TileContent.Binary {
    val cls = binaryClass(deviceClass)
    val on = raw == ON
    return TileContent.Binary(if (on) cls.on else cls.off, if (on) cls.level else null, since)
}
