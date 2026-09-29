package com.stacknoise.haac.feature.layout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.database.layout.PlaceRepository
import com.stacknoise.haac.feature.layout.data.PlaceTrash
import com.stacknoise.haac.feature.layout.domain.Deletion
import com.stacknoise.haac.feature.layout.domain.PlaceFilter
import com.stacknoise.haac.core.database.layout.Places
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

/** What the Places overview shows; [places] is null until the first read finished. */
data class PlacesUiState(
    val places: Places? = null,
    val filter: PlaceFilter = PlaceFilter.ALL,
    val error: ErrorCode? = null,
)

/** The Places overview of the active instance (M-02, concept 6) and the undo of deletions (6.2). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlacesViewModel @Inject constructor(
    active: ActiveInstanceStore,
    repository: PlaceRepository,
    private val trash: PlaceTrash,
) : ViewModel() {
    private val filter = MutableStateFlow(PlaceFilter.ALL)
    private val failure = MutableStateFlow<ErrorCode?>(null)

    private val places: Flow<Places?> = active.activeServerId
        .flatMapLatest { id -> if (id == null) flowOf(Places()) else repository.places(id) }
        .map<Places, Places?> { it }
        .catch { e ->
            if (e !is HaacException) throw e
            failure.value = e.code
            emit(Places())
        }

    /** The current screen state. */
    val state: StateFlow<PlacesUiState> = combine(places, filter, failure, ::PlacesUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PlacesUiState())

    /** The deletion whose *Undo* snackbar is shown. */
    val pending: StateFlow<Deletion?> = trash.pending

    init {
        write { trash.purgeExpired() }
    }

    /** Selects a filter chip. */
    fun onFilter(selected: PlaceFilter) {
        filter.value = selected
    }

    /** *Undo* in the snackbar. */
    fun onUndo(deletion: Deletion) = write { trash.undo(deletion) }

    /** The snackbar closed without *Undo*. */
    fun onUndoExpired(deletion: Deletion) = write { trash.expire(deletion) }

    /** Runs a write; a failed one is shown with its code. */
    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: HaacException) {
                failure.value = e.code
            }
        }
    }

    /** Sharing timeout. */
    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
