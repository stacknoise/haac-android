package com.stacknoise.haac.feature.entities.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.assignment.TileSize
import com.stacknoise.haac.core.database.layout.PlaceRepository
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.feature.entities.data.AssignmentWriter
import com.stacknoise.haac.feature.entities.data.EntityCatalog
import com.stacknoise.haac.feature.entities.domain.LayoutDraft
import com.stacknoise.haac.feature.entities.domain.Tile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Grid preview (M-06) or list arrange mode (M-07). */
enum class ArrangeMode {
    GRID,
    LIST,
}

/**
 * What the edit layout shows: [draft] is null until the room's tiles are read. [done] closes the screen after
 * saving; [error] is shown with its code.
 */
data class EditLayoutUiState(
    val roomName: String = "",
    val draft: LayoutDraft? = null,
    val mode: ArrangeMode = ArrangeMode.GRID,
    val saving: Boolean = false,
    val error: ErrorCode? = null,
    val done: Boolean = false,
)

/**
 * Edit layout of one room (concept 7.2, 7.3, M-06, M-07); the route passes [ROOM_ARG]. Order, sizes and removals
 * stay a draft until *Done*; *Rename* is saved at once, like everywhere else.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EditLayoutViewModel @Inject constructor(
    savedState: SavedStateHandle,
    active: ActiveInstanceStore,
    places: PlaceRepository,
    catalog: EntityCatalog,
    private val writer: AssignmentWriter,
) : ViewModel() {
    /** The edited room. */
    val roomId: String = savedState[ROOM_ARG] ?: ""
    private val serverId = active.activeServerId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val _state = MutableStateFlow(EditLayoutUiState())

    /** The current screen state. */
    val state: StateFlow<EditLayoutUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            active.activeServerId
                .flatMapLatest { id ->
                    if (id == null) {
                        emptyFlow<Pair<String, List<Tile>>>()
                    } else {
                        val name = places.places(id).map { it.room(roomId)?.name.orEmpty() }
                        combine(name, catalog.roomTiles(id, roomId)) { room, tiles -> room to tiles }
                    }
                }
                .catch { e ->
                    if (e !is HaacException) throw e
                    _state.update { it.copy(error = e.code) }
                }
                .collect { (room, tiles) ->
                    _state.update { it.copy(roomName = room, draft = it.draft?.refresh(tiles) ?: LayoutDraft(tiles)) }
                }
        }
    }

    /** Drag and drop, or *Move up* / *Move down*: the tile at [from] goes to [to]. */
    fun onMove(from: Int, to: Int) = edit { it.move(from, to) }

    /** The size menu of a tile. */
    fun onResize(entityId: String, size: TileSize) = edit { it.resize(entityId, size) }

    /** The remove badge (−) of a tile. */
    fun onRemove(entityId: String) = edit { it.remove(entityId) }

    /** *Grid* or *List*. */
    fun onMode(mode: ArrangeMode) = _state.update { it.copy(mode = mode) }

    /** Saves the local name at once (M-07); null restores the default name. */
    fun onRename(entityId: String, alias: String?) {
        val id = serverId.value ?: return
        viewModelScope.launch {
            try {
                writer.rename(id, entityId, alias)
            } catch (e: HaacException) {
                _state.update { it.copy(error = e.code) }
            }
        }
    }

    /** *Done*: saves the draft in one transaction and closes; without changes it only closes. */
    fun onDone() {
        val draft = _state.value.draft
        if (draft == null || !draft.changed) {
            _state.update { it.copy(done = true) }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                writer.saveLayout(roomId, draft.arrangement(), draft.removed)
                _state.update { it.copy(saving = false, done = true) }
            } catch (e: HaacException) {
                _state.update { it.copy(saving = false, error = e.code) }
            }
        }
    }

    /** Applies [change] to the draft. */
    private fun edit(change: (LayoutDraft) -> LayoutDraft) =
        _state.update { current -> current.copy(draft = current.draft?.let(change)) }

    /** Route argument. */
    companion object {
        /** Route argument with the room id. */
        const val ROOM_ARG = "roomId"
    }
}
