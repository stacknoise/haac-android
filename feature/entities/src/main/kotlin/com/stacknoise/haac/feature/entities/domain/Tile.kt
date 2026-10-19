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

/**
 * How an active tile may be used, shared by tile, list arrange mode and detail screen (concept 7.5, 8.5):
 * [NORMAL] switches as usual, [GUARDED] asks for a confirmation first (switch guard), [DISABLED] cannot be switched
 * by the user's own choice. A deactivated entity that also has a guard is [DISABLED]: no dialog appears for it.
 */
enum class TileMode {
    NORMAL,
    GUARDED,
    DISABLED,
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
 * and for unavailable or withdrawn entities (concept 8, 14.1). [mode] tells whether the user deactivated or guarded
 * the entity (7.5, 8.5).
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
    val mode: TileMode = TileMode.NORMAL,
) {
    /** True if HA reports a usable state and the entity is still shared; only then controls work. */
    val active: Boolean get() = available && !withdrawn

    /** True if the user deactivated the entity: its state is shown, but it cannot be switched (concept 7.5). */
    val disabled: Boolean get() = mode == TileMode.DISABLED

    /** True if a tap that switches needs the confirmation dialog first (concept 8.5). */
    val guarded: Boolean get() = mode == TileMode.GUARDED

    /**
     * True if a tap on the tile body does something: a withdrawn or deactivated tile opens its sheet, a switch
     * toggles. Every other tile reacts to a long press only, so it must not announce a tap action to a screen
     * reader (concept 7.5, 8.1, M-05).
     */
    val tappable: Boolean get() = withdrawn || disabled || control is EntityControl.Toggle
}
