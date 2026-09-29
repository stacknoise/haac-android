package com.stacknoise.haac.feature.entities.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.layout.PlaceRepository
import com.stacknoise.haac.core.database.layout.Places
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.feature.entities.data.AssignmentWriter
import com.stacknoise.haac.feature.entities.data.EntityCatalog
import com.stacknoise.haac.feature.entities.domain.CatalogEntry
import com.stacknoise.haac.feature.entities.domain.PickerRow
import com.stacknoise.haac.feature.entities.domain.pickerDomains
import com.stacknoise.haac.feature.entities.domain.pickerRows
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Choices of the picker: tab, filter text and the picked entity ids. */
data class PickerInput(val domain: String? = null, val filter: String = "", val picked: Set<String> = emptySet())

/**
 * What *Add entities* shows (M-04). [rows] are those of the current tab; [pickedByDomain] counts the picks per
 * domain for the button. [done] closes the screen.
 */
data class AddEntitiesUiState(
    val roomName: String = "",
    val domains: List<String> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    val input: PickerInput = PickerInput(),
    val rows: List<PickerRow> = emptyList(),
    val pickedByDomain: Map<String, Int> = emptyMap(),
    val status: AddStatus = AddStatus(),
)

/** Progress of adding: [busy] while writing, [error] with its code, [done] once added. */
data class AddStatus(val busy: Boolean = false, val error: ErrorCode? = null, val done: Boolean = false)

/** Picker of entities for one room (M-04, concept 7.1, 7.2); the route passes the room ([ROOM_ARG]). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AddEntitiesViewModel @Inject constructor(
    savedState: SavedStateHandle,
    active: ActiveInstanceStore,
    catalog: EntityCatalog,
    places: PlaceRepository,
    private val writer: AssignmentWriter,
) : ViewModel() {
    private val roomId: String = savedState[ROOM_ARG] ?: ""
    private val input = MutableStateFlow(PickerInput())
    private val status = MutableStateFlow(AddStatus())

    private val entries: StateFlow<List<CatalogEntry>> = active.activeServerId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else catalog.assignable(id).reportErrors(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val placeFlow: Flow<Places> = active.activeServerId
        .flatMapLatest { id -> if (id == null) flowOf(Places()) else places.places(id).reportErrors(Places()) }

    /** The current screen state. */
    val state: StateFlow<AddEntitiesUiState> = combine(entries, placeFlow, input, status) { list, where, choice, run ->
        val domains = pickerDomains(list)
        val tab = choice.domain?.takeIf { it in domains } ?: domains.firstOrNull()
        AddEntitiesUiState(
            roomName = where.room(roomId)?.name.orEmpty(),
            domains = domains,
            counts = list.groupingBy { it.domain }.eachCount(),
            input = choice.copy(domain = tab),
            rows = pickerRows(list, roomId, where, tab, choice.filter),
            pickedByDomain = list.filter { it.entityId in choice.picked }.groupingBy { it.domain }.eachCount(),
            status = run,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AddEntitiesUiState())

    /** Selects a domain tab. */
    fun onDomain(domain: String) = input.update { it.copy(domain = domain) }

    /** Changes the filter text. */
    fun onFilter(text: String) = input.update { it.copy(filter = text) }

    /** Picks or unpicks [entityId]. */
    fun onToggle(entityId: String) = input.update {
        it.copy(picked = if (entityId in it.picked) it.picked - entityId else it.picked + entityId)
    }

    /** *Add …*: places the picked entities at the end of the room, then closes (concept 7.2). */
    fun onAdd() {
        val picked = entries.value.filter { it.entityId in input.value.picked && roomId !in it.roomIds }
        if (picked.isEmpty() || status.value.busy) return
        status.value = AddStatus(busy = true)
        viewModelScope.launch {
            status.value = try {
                writer.add(roomId, picked)
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
        /** Id of the room to add to. */
        const val ROOM_ARG = "roomId"

        /** Keeps the state while the screen briefly stops. */
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
