package com.stacknoise.haac.feature.entities.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.layout.PlaceRepository
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.feature.entities.data.AssignmentWriter
import com.stacknoise.haac.feature.entities.data.EntityCatalog
import com.stacknoise.haac.feature.entities.domain.CatalogEntry
import com.stacknoise.haac.feature.entities.domain.RoomGroup
import com.stacknoise.haac.feature.entities.domain.roomGroups
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What *Add to room* shows: the entities still shared, the rooms to choose from and the chosen [roomId]. */
data class AssignUiState(
    val entries: List<CatalogEntry> = emptyList(),
    val groups: List<RoomGroup>? = null,
    val roomId: String? = null,
    val status: AddStatus = AddStatus(),
)

/**
 * *Add to room* / *Review N entities* of the notification list (M-09, concept 9.1): puts the entities of the
 * route ([IDS_ARG], comma-separated) into one chosen room.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AssignViewModel @Inject constructor(
    savedState: SavedStateHandle,
    active: ActiveInstanceStore,
    catalog: EntityCatalog,
    places: PlaceRepository,
    private val writer: AssignmentWriter,
) : ViewModel() {
    private val ids: Set<String> =
        savedState.get<String>(IDS_ARG).orEmpty().split(',').filter { it.isNotBlank() }.toSet()
    private val roomId = MutableStateFlow<String?>(null)
    private val status = MutableStateFlow(AddStatus())

    private val entries: Flow<List<CatalogEntry>> = active.activeServerId.flatMapLatest { id ->
        if (id == null) {
            flowOf(emptyList())
        } else {
            catalog.assignable(id).map { list -> list.filter { it.entityId in ids } }
        }
    }.reportErrors(emptyList())

    private val groups: Flow<List<RoomGroup>> = active.activeServerId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else places.places(id).map { it.roomGroups() }
    }.reportErrors(emptyList())

    /** The current screen state. */
    val state: StateFlow<AssignUiState> = combine(entries, groups, roomId, status) { list, rooms, chosen, progress ->
        val room = chosen?.takeIf { id -> rooms.any { group -> group.rooms.any { it.id == id } } }
        AssignUiState(list, rooms, room, progress)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AssignUiState())

    /** Chooses the room. */
    fun onSelectRoom(id: String) {
        roomId.value = id
    }

    /** *Add to …*: adds the entities to the chosen room, then closes. */
    fun onAdd() {
        val current = state.value
        val room = current.roomId ?: return
        if (current.entries.isEmpty() || current.status.busy) return
        status.value = AddStatus(busy = true)
        viewModelScope.launch {
            status.value = try {
                writer.add(room, current.entries)
                AddStatus(done = true)
            } catch (e: HaacException) {
                AddStatus(error = e.code)
            }
        }
    }

    /** A read error is shown with its code; the flow goes on with [fallback]. */
    private fun <T> Flow<T>.reportErrors(fallback: T): Flow<T> = catch { e ->
        if (e !is HaacException) throw e
        status.update { it.copy(error = e.code) }
        emit(fallback)
    }

    /** Route argument and sharing timeout. */
    companion object {
        /** The entity ids to add, comma-separated. */
        const val IDS_ARG = "entityIds"

        /** Keeps the state while the screen briefly stops. */
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
