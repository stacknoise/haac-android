package com.stacknoise.haac.feature.entities.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.layout.PlaceRepository
import com.stacknoise.haac.core.database.layout.Room
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.feature.entities.data.AssignmentWriter
import com.stacknoise.haac.feature.entities.data.EntityCatalog
import com.stacknoise.haac.feature.entities.data.EntityController
import com.stacknoise.haac.feature.entities.domain.ControlRequest
import com.stacknoise.haac.feature.entities.domain.EntityControl
import com.stacknoise.haac.feature.entities.domain.RoomGroup
import com.stacknoise.haac.feature.entities.domain.Tile
import com.stacknoise.haac.feature.entities.domain.groupOf
import com.stacknoise.haac.feature.entities.domain.roomGroups
import com.stacknoise.haac.feature.entities.domain.roomOrFirst
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What the room grid shows; [groups] is null until the places are read, empty without rooms. Controls work only
 * while [connected]; [stale] marks the cached states after a longer outage (concept 14.1).
 */
data class RoomsUiState(
    val groups: List<RoomGroup>? = null,
    val room: Room? = null,
    val tiles: List<Tile> = emptyList(),
    val error: ErrorCode? = null,
    val connected: Boolean = false,
    val stale: Boolean = false,
) {
    /** The level (or home) of the shown room, for the header and the chips. */
    val group: RoomGroup? get() = groups?.groupOf(room?.id)
}

/** The Rooms tab (M-05, M-08, concept 7, 8): one room of the active instance with its tiles and controls. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RoomsViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    active: ActiveInstanceStore,
    places: PlaceRepository,
    catalog: EntityCatalog,
    private val writer: AssignmentWriter,
    private val controller: EntityController,
) : ViewModel() {
    private val serverId = active.activeServerId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val failure = MutableStateFlow<ErrorCode?>(null)

    private val groups: Flow<List<RoomGroup>> = active.activeServerId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else places.places(id).map { it.roomGroups() }.reportErrors(emptyList())
        }

    private val room: Flow<Room?> = combine(groups, savedState.getStateFlow<String?>(ROOM_KEY, null)) { list, id ->
        list.roomOrFirst(id)
    }

    private val tiles: Flow<List<Tile>> = combine(serverId, room) { id, current -> id to current?.id }
        .flatMapLatest { (id, roomId) ->
            when {
                id == null || roomId == null -> flowOf(emptyList())
                else -> catalog.roomTiles(id, roomId).reportErrors(emptyList())
            }
        }

    /** Whether the connection is open and whether the cached states count as stale (concept 14.1). */
    private val link: Flow<Pair<Boolean, Boolean>> =
        combine(controller.connected, controller.stale) { connected, stale -> connected to stale }

    /** The current screen state. */
    val state: StateFlow<RoomsUiState> =
        combine(groups, room, tiles, failure, link) { list, current, shown, error, (connected, stale) ->
            RoomsUiState(list, current, shown, error, connected, stale)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), RoomsUiState())

    /** Failed service calls, shown as a snackbar with the code (concept 14.1, 17.4). */
    val failed: Flow<HaacException> = controller.failed

    /** Tap on a switch tile or its toggle: turns it to the other state (concept 8.2). */
    fun onToggle(tile: Tile) {
        val toggle = tile.control as? EntityControl.Toggle ?: return
        onControl(tile, toggle.flipped())
    }

    /** − or + of a climate tile (concept 8.4). */
    fun onStep(tile: Tile, up: Boolean) {
        val temperature = tile.control as? EntityControl.TargetTemperature ?: return
        temperature.stepped(up)?.let { onControl(tile, it) }
    }

    /** Sends [request] for [tile] while connected. */
    private fun onControl(tile: Tile, request: ControlRequest) {
        val id = serverId.value ?: return
        if (state.value.connected) controller.send(id, tile.entityId, request)
    }

    /** Shows room [roomId] (chips, level menu). */
    fun onSelectRoom(roomId: String) {
        savedState[ROOM_KEY] = roomId
    }

    /** *Remove from this room* for [entityIds]. */
    fun onRemoveHere(entityIds: List<String>) {
        val roomId = state.value.room?.id ?: return
        write { entityIds.forEach { writer.remove(roomId, it) } }
    }

    /** *Remove from all rooms* for [entityIds] (concept 7.4). */
    fun onRemoveEverywhere(entityIds: List<String>) {
        val id = serverId.value ?: return
        write { writer.removeEverywhere(id, entityIds) }
    }

    /** Sets the local name of [entityId]; null or blank restores the default name (M-07). */
    fun onRename(entityId: String, alias: String?) {
        val id = serverId.value ?: return
        write { writer.rename(id, entityId, alias) }
    }

    /** A read error is shown with its code; the flow goes on with [fallback]. */
    private fun <T> Flow<T>.reportErrors(fallback: T): Flow<T> = catch { e ->
        if (e !is HaacException) throw e
        failure.value = e.code
        emit(fallback)
    }

    /** Runs a write; a failed one is shown with its code. */
    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                failure.value = null
            } catch (e: HaacException) {
                failure.value = e.code
            }
        }
    }

    /** Saved state key and sharing timeout. */
    private companion object {
        const val ROOM_KEY = "roomId"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
