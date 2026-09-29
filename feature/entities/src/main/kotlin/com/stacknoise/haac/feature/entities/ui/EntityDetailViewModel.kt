package com.stacknoise.haac.feature.entities.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.feature.entities.data.AssignmentWriter
import com.stacknoise.haac.feature.entities.data.EntityCatalog
import com.stacknoise.haac.feature.entities.data.EntityController
import com.stacknoise.haac.feature.entities.domain.ControlRequest
import com.stacknoise.haac.feature.entities.domain.EntityDetail
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
 * What the detail screen shows: [detail] is null until read, and stays null ([missing]) if the cache does not
 * know the entity. Controls work only while [connected] (concept 14.1).
 */
data class EntityDetailUiState(
    val detail: EntityDetail? = null,
    val missing: Boolean = false,
    val connected: Boolean = false,
    val error: ErrorCode? = null,
)

/** The detail screen of one entity of the active instance (concept 8, 15.4); the route passes [ENTITY_ARG]. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EntityDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    active: ActiveInstanceStore,
    catalog: EntityCatalog,
    private val writer: AssignmentWriter,
    private val controller: EntityController,
) : ViewModel() {
    private val entityId: String = savedState[ENTITY_ARG] ?: ""
    private val serverId = active.activeServerId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val failure = MutableStateFlow<ErrorCode?>(null)

    /** The entity with a flag whether it was read yet. */
    private val detail: Flow<Pair<Boolean, EntityDetail?>> = active.activeServerId.flatMapLatest { id ->
        if (id == null) {
            flowOf(true to null)
        } else {
            catalog.detail(id, entityId)
                .map { true to it }
                .catch { e ->
                    if (e !is HaacException) throw e
                    failure.value = e.code
                    emit(true to null)
                }
        }
    }

    /** The current screen state. */
    val state: StateFlow<EntityDetailUiState> =
        combine(detail, controller.connected, failure) { (read, shown), connected, error ->
            EntityDetailUiState(shown, missing = read && shown == null, connected = connected, error = error)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), EntityDetailUiState())

    /** Failed service calls, shown as a snackbar with the code (concept 14.1, 17.4). */
    val failed: Flow<HaacException> = controller.failed

    /** Sends [request] (a control of the screen) while connected; nothing for a null request (limit reached). */
    fun onControl(request: ControlRequest?) {
        val id = serverId.value ?: return
        if (request != null && state.value.connected) controller.send(id, entityId, request)
    }

    /** Sets the local name; null restores the default name (M-07, concept 7.3). */
    fun onRename(alias: String?) {
        val id = serverId.value ?: return
        viewModelScope.launch {
            try {
                writer.rename(id, entityId, alias)
                failure.value = null
            } catch (e: HaacException) {
                failure.value = e.code
            }
        }
    }

    /** Route argument and sharing timeout. */
    companion object {
        /** Route argument with the `entity_id`. */
        const val ENTITY_ARG = "entityId"
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
