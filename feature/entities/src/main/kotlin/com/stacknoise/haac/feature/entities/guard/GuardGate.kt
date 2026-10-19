package com.stacknoise.haac.feature.entities.guard

import com.stacknoise.haac.feature.entities.domain.ControlRequest
import com.stacknoise.haac.feature.entities.domain.EntityControl
import com.stacknoise.haac.feature.entities.domain.Tile
import com.stacknoise.haac.feature.entities.domain.TileContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The open confirmation dialog (M-27): [name] of the switch, whether it is [on] right now (null if unknown) and the
 * [request] that is sent once the user flips the dialog's switch.
 */
data class GuardPrompt(
    val entityId: String,
    val name: String,
    val on: Boolean?,
    val request: ControlRequest.SwitchTo,
)

/**
 * The one place in the switching path that checks the switch guard before a request reaches the service call
 * (concept 8.5): tile, list and detail screen all send through [submit]. A guarded request opens [prompt] instead of
 * calling [send]; only [confirm] sends it, [dismiss] (cancel, back, outside tap, timeout) drops it without a command.
 */
class GuardGate(private val send: (entityId: String, request: ControlRequest) -> Unit) {
    private val open = MutableStateFlow<GuardPrompt?>(null)

    /** The dialog to show, or null if none is open. */
    val prompt: StateFlow<GuardPrompt?> = open

    /** Sends [request] for [tile] at once, or asks for the confirmation first if [guard] applies to it. */
    fun submit(tile: Tile, guard: SwitchGuard?, request: ControlRequest) {
        if (!requiresConfirmation(tile, guard, request)) {
            send(tile.entityId, request)
            return
        }
        val on = (tile.content as? TileContent.Switch)?.on
        open.value = GuardPrompt(tile.entityId, tile.name, on, request as ControlRequest.SwitchTo)
    }

    /** The user flipped the dialog's switch: closes the dialog and sends the request. */
    fun confirm() {
        val asked = open.value.also { open.value = null } ?: return
        send(asked.entityId, asked.request)
    }

    /** Closes the dialog without a command. */
    fun dismiss() {
        open.value = null
    }
}

/**
 * True if [request] on [tile] must be confirmed: [guard] covers its direction and the tile is an active switch.
 * Deactivated and withdrawn entities cannot be switched, so no dialog appears for them (concept 8.5).
 */
internal fun requiresConfirmation(tile: Tile, guard: SwitchGuard?, request: ControlRequest): Boolean =
    guard != null &&
        request is ControlRequest.SwitchTo &&
        tile.control is EntityControl.Toggle &&
        tile.active &&
        !tile.disabled &&
        guard.requiresConfirmation(request.on)
