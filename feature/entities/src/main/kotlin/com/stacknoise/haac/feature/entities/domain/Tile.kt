package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.assignment.TileSize

/** Icon of a tile, from the domain and the device class (concept 8.2, 8.3). */
enum class TileIcon {
    SWITCH,
    OUTLET,
    SENSOR,
    TEMPERATURE,
    HUMIDITY,
    POWER,
    ENERGY,
    BATTERY,
    CLIMATE,
}

/** What a tile shows, by kind (M-05); built only by [TileFactory]. */
sealed interface TileContent {
    /** A switch: [on] is null while its state is not known. */
    data class Switch(val on: Boolean?) : TileContent

    /** A sensor value, rounded as HA suggests, with its unit. */
    data class Sensor(val value: String, val unit: String?) : TileContent

    /** A timestamp sensor: the time [at] in milliseconds, shown relative ("in 3 h") and absolute (concept 8.3). */
    data class Timestamp(val at: Long) : TileContent

    /** A climate entity: target and current temperature (formatted) and whether it heats right now. */
    data class Climate(val target: String?, val current: String?, val heating: Boolean) : TileContent

    /** Any other domain: its plain state. */
    data class Other(val state: String) : TileContent
}

/**
 * One tile of a room (concept 7.2, 7.4, M-05, M-08). [name] includes the local alias, [defaultName] is the name
 * without it (7.3). [available] is false while HA reports `unavailable` or `unknown`; [withdrawn] entities are
 * no longer shared and show as inactive tiles with their last state. [control] is null for tiles without one
 * and for unavailable or withdrawn entities (concept 8, 14.1).
 */
data class Tile(
    val entityId: String,
    val name: String,
    val icon: TileIcon,
    val size: TileSize,
    val content: TileContent,
    val available: Boolean = true,
    val withdrawn: Boolean = false,
    val defaultName: String = name,
    val control: EntityControl? = null,
) {
    /** True if HA reports a usable state and the entity is still shared; only then controls work. */
    val active: Boolean get() = available && !withdrawn
}
