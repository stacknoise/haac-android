package com.stacknoise.haac.feature.layout.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.feature.layout.data.PlaceRepository
import com.stacknoise.haac.feature.layout.data.PlaceTrash
import com.stacknoise.haac.feature.layout.data.PlaceWriter
import com.stacknoise.haac.feature.layout.domain.PlaceForm
import com.stacknoise.haac.feature.layout.domain.PlaceKind
import com.stacknoise.haac.feature.layout.domain.Places
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Progress of the editor: [busy] while saving or deleting, [done] once the screen should close. */
data class EditorStatus(val busy: Boolean = false, val error: ErrorCode? = null, val done: Boolean = false)

/** What the editor shows; [form] is null until the places are read. */
data class PlaceEditorUiState(
    val form: PlaceForm? = null,
    val places: Places = Places(),
    val status: EditorStatus = EditorStatus(),
)

/**
 * Form for a new or existing home, level or room (M-03, concept 6.1, 6.2). The route passes the kind
 * ([KIND_ARG]) and the place to edit ([ID_ARG], none for a new one).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaceEditorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    active: ActiveInstanceStore,
    repository: PlaceRepository,
    private val writer: PlaceWriter,
    private val trash: PlaceTrash,
) : ViewModel() {
    private val kind = savedState.get<String>(KIND_ARG)?.let(PlaceKind::valueOf) ?: PlaceKind.HOME
    private val placeId: String? = savedState[ID_ARG]
    private val serverId = active.activeServerId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val form = MutableStateFlow<PlaceForm?>(null)
    private val status = MutableStateFlow(EditorStatus())

    private val places: StateFlow<Places?> = active.activeServerId
        .flatMapLatest { id -> if (id == null) flowOf(Places()) else repository.places(id) }
        .catch { e ->
            if (e !is HaacException) throw e
            status.update { it.copy(error = e.code) }
            emit(Places())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The current screen state. */
    val state: StateFlow<PlaceEditorUiState> = combine(form, places, status) { f, p, s ->
        PlaceEditorUiState(f, p ?: Places(), s)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PlaceEditorUiState())

    init {
        viewModelScope.launch {
            val current = places.filterNotNull().first()
            val initial = when (placeId) {
                null -> PlaceForm.create(kind, current)
                else -> PlaceForm.edit(kind, placeId, current)
            }
            // A place deleted in the meantime closes the editor.
            if (initial == null) status.update { it.copy(done = true) } else form.value = initial
        }
    }

    /** Applies an input [change] to the form; it gets the current places, e.g. to check a level's home. */
    fun onChange(change: (PlaceForm, Places) -> PlaceForm) {
        form.update { current -> current?.let { change(it, places.value ?: Places()) } }
    }

    /** *Create â€¦* or *Save*. */
    fun onSave() {
        val current = form.value ?: return
        val instance = serverId.value ?: return
        submit { writer.save(instance, current) }
    }

    /** Deletes the edited place; a level's rooms go with it if [withRooms] (concept 6.2). */
    fun onDelete(withRooms: Boolean = false) {
        val current = places.value ?: return
        submit {
            when (kind) {
                PlaceKind.HOME -> current.home(placeId)?.let { trash.deleteHome(it) }
                PlaceKind.FLOOR -> current.floor(placeId)?.let { trash.deleteFloor(it, withRooms) }
                PlaceKind.ROOM -> current.room(placeId)?.let { trash.deleteRoom(it) }
            }
        }
    }

    /** Runs a save or delete and closes the editor; a failure stays open with its code. */
    private fun submit(block: suspend () -> Unit) {
        if (status.value.busy) return
        status.value = EditorStatus(busy = true)
        viewModelScope.launch {
            status.value = try {
                block()
                EditorStatus(done = true)
            } catch (e: HaacException) {
                EditorStatus(error = e.code)
            }
        }
    }

    /** Route arguments and sharing timeout. */
    companion object {
        /** Kind of the place, a [PlaceKind] name. */
        const val KIND_ARG = "kind"

        /** Id of the place to edit; absent for a new place. */
        const val ID_ARG = "id"

        /** Keeps the state while the screen briefly stops. */
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
