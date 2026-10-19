package com.stacknoise.haac.feature.entities.guard

import com.stacknoise.haac.core.database.guard.EntityGuard
import com.stacknoise.haac.feature.entities.domain.Tile
import com.stacknoise.haac.feature.entities.domain.TileContent
import com.stacknoise.haac.feature.entities.domain.TileMode

/** In which direction a switch guard asks for a confirmation (concept 8.5, M-28). */
enum class GuardDirection {
    ON,
    OFF,
    BOTH,
}

/** The switch guard of one switch: a tap that goes in [direction] needs the confirmation dialog (concept 8.5). */
data class SwitchGuard(val direction: GuardDirection = GuardDirection.BOTH) {
    /** True if turning the switch on ([target] true) or off (false) must be confirmed first. */
    fun requiresConfirmation(target: Boolean): Boolean = when (direction) {
        GuardDirection.BOTH -> true
        GuardDirection.ON -> target
        GuardDirection.OFF -> !target
    }
}

/** The table row of this guard for entity [entityId] of instance [serverId]. */
internal fun SwitchGuard.toRow(serverId: String, entityId: String): EntityGuard = EntityGuard(
    serverId = serverId,
    entityId = entityId,
    onTurnOn = direction != GuardDirection.OFF,
    onTurnOff = direction != GuardDirection.ON,
)

/** The guard of this row; null if it guards neither direction, i.e. is off. */
internal fun EntityGuard.toSwitchGuard(): SwitchGuard? = when {
    onTurnOn && onTurnOff -> SwitchGuard(GuardDirection.BOTH)
    onTurnOn -> SwitchGuard(GuardDirection.ON)
    onTurnOff -> SwitchGuard(GuardDirection.OFF)
    else -> null
}

/** True if a guard can be set for this tile: only switches have one (concept 8.5), and not withdrawn ones. */
val Tile.guardable: Boolean get() = content is TileContent.Switch && !withdrawn

/**
 * This tile with [guard] applied: a normal switch tile becomes [TileMode.GUARDED]. A deactivated tile keeps its
 * mode, because no dialog appears for it (concept 8.5).
 */
fun Tile.withGuard(guard: SwitchGuard?): Tile =
    if (guard != null && guardable && mode == TileMode.NORMAL) copy(mode = TileMode.GUARDED) else this

/** These tiles with the [guards] of their entities applied (see [withGuard]). */
fun List<Tile>.withGuards(guards: Map<String, SwitchGuard>): List<Tile> =
    if (guards.isEmpty()) this else map { it.withGuard(guards[it.entityId]) }
