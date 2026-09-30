package com.stacknoise.haac.feature.layout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.layout.PlaceRepository
import com.stacknoise.haac.core.database.layout.Places
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.feature.layout.data.AreaImporter
import com.stacknoise.haac.feature.layout.data.AreaSource
import com.stacknoise.haac.feature.layout.data.ImportTarget
import com.stacknoise.haac.feature.layout.domain.HaAreas
import com.stacknoise.haac.feature.layout.domain.ImportPlan
import com.stacknoise.haac.feature.layout.domain.plan
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What the import wizard shows (concept 6.3): the [areas] of Home Assistant once loaded, the target home
 * ([homeId], or a new home called [newHomeName] when null) and the [selected] area ids.
 */
data class AreaImportUiState(
    val loading: Boolean = true,
    val areas: HaAreas? = null,
    val places: Places = Places(),
    val homeId: String? = null,
    val newHomeName: String = "",
    val selected: Set<String> = emptySet(),
    val busy: Boolean = false,
    val error: ErrorCode? = null,
    val done: Boolean = false,
) {
    /** What the import would add with the current choices. */
    val plan: ImportPlan get() = areas?.plan(selected, places, homeId) ?: ImportPlan()

    /** True if the target is set and there is something to add. */
    val canImport: Boolean get() = !busy && !plan.isEmpty && (homeId != null || newHomeName.isNotBlank())
}

/**
 * Import wizard: pre-fills levels and rooms from the areas of Home Assistant (concept 6.3). The result is an
 * ordinary local structure; nothing is synchronised afterwards.
 */
@HiltViewModel
class AreaImportViewModel @Inject constructor(
    private val active: ActiveInstanceStore,
    private val repository: PlaceRepository,
    private val source: AreaSource,
    private val importer: AreaImporter,
) : ViewModel() {
    private val _state = MutableStateFlow(AreaImportUiState())

    /** The current screen state. */
    val state: StateFlow<AreaImportUiState> = _state.asStateFlow()

    init {
        load()
    }

    /** Loads the places of the active instance and the areas of Home Assistant; *Try again* runs this again. */
    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val serverId = active.activeServerId.first() ?: return@launch
                val places = repository.places(serverId).first()
                val areas = source.load()
                _state.update {
                    it.copy(
                        loading = false,
                        areas = areas,
                        places = places,
                        homeId = places.homes.firstOrNull()?.id,
                        selected = areas.areas.mapTo(mutableSetOf()) { area -> area.id },
                    )
                }
            } catch (e: HaacException) {
                _state.update { it.copy(loading = false, error = e.code) }
            }
        }
    }

    /** Chooses the target home; null stands for a new home. */
    fun onHome(homeId: String?) = _state.update { it.copy(homeId = homeId) }

    /** The name of the new home. */
    fun onNewHomeName(name: String) = _state.update { it.copy(newHomeName = name) }

    /** Checks or unchecks area [areaId]. */
    fun onToggle(areaId: String) = _state.update {
        it.copy(selected = if (areaId in it.selected) it.selected - areaId else it.selected + areaId)
    }

    /** Checks all areas, or none if all are checked. */
    fun onToggleAll() = _state.update { current ->
        val all = current.areas?.areas.orEmpty().mapTo(mutableSetOf()) { it.id }
        current.copy(selected = if (current.selected.containsAll(all)) emptySet() else all)
    }

    /** Writes the plan; a failure stays on the screen with its code. */
    fun onImport() {
        val current = _state.value
        if (!current.canImport) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val serverId = active.activeServerId.first() ?: return@launch
                importer.import(serverId, ImportTarget(current.homeId, current.newHomeName), current.plan)
                _state.update { it.copy(busy = false, done = true) }
            } catch (e: HaacException) {
                _state.update { it.copy(busy = false, error = e.code) }
            }
        }
    }
}
