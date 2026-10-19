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
import com.stacknoise.haac.feature.entities.guard.GuardGate
import com.stacknoise.haac.feature.entities.guard.GuardPrompt
import com.stacknoise.haac.feature.entities.guard.SwitchGuard
import com.stacknoise.haac.feature.entities.guard.SwitchGuards
import com.stacknoise.haac.feature.entities.guard.withGuard
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
 * know the entity. Controls work only while [connected] (concept 14.1). [guard] is the entity's switch guard, already
 * applied to the tile of [detail] (concept 8.5).
 */
data class EntityDetailUiState(
    val detail: EntityDetail? = null,
    val missing: Boolean = false,
    val connected: Boolean = false,
    val error: ErrorCode? = null,
    val guard: SwitchGuard? = null,
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
    private val switchGuards: SwitchGuards,
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

    /** The switch guard of the entity (concept 8.5). */
    private val guard: Flow<SwitchGuard?> = active.activeServerId.flatMapLatest { id ->
        if (id == null) {
            flowOf(null)
        } else {
            switchGuards.observe(id).map { it[entityId] }.catch { e ->
                if (e !is HaacException) throw e
                failure.value = e.code
                emit(null)
            }
        }
    }

    /** The current screen state. */
    val state: StateFlow<EntityDetailUiState> =
        combine(detail, controller.connected, failure, guard) { (read, shown), connected, error, set ->
            EntityDetailUiState(
                detail = shown?.let { it.copy(tile = it.tile.withGuard(set)) },
                missing = read && shown == null,
                connected = connected,
                error = error,
                guard = set,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), EntityDetailUiState())

    /** Failed service calls, shown as a snackbar with the code (concept 14.1, 17.4). */
    val failed: Flow<HaacException> = controller.failed

    /** Sends go through the switch guard first (concept 8.5); a guarded one waits for the dialog. */
    private val gate = GuardGate { id, request ->
        val server = serverId.value
        if (server != null && state.value.connected) controller.send(server, id, request)
    }

    /** The open switch guard dialog, or null (M-27). */
    val guardPrompt: StateFlow<GuardPrompt?> = gate.prompt

    /** The user flipped the switch in the guard dialog: sends the command. */
    fun onGuardConfirm() = gate.confirm()

    /** The guard dialog was cancelled, timed out or dismissed: no command. */
    fun onGuardDismiss() = gate.dismiss()

    /** Turns the guard of the entity on with [guard]'s direction or, for null, off (concept 8.5, M-28). */
    fun onGuardChange(guard: SwitchGuard?) {
        val id = serverId.value ?: return
        viewModelScope.launch {
            try {
                switchGuards.set(id, entityId, guard)
                failure.value = null
            } catch (e: HaacException) {
                failure.value = e.code
            }
        }
    }

    /** Sends [request] (a control of the screen) while connected; nothing for a null request (limit reached). */
    fun onControl(request: ControlRequest?) {
        val tile = state.value.detail?.tile ?: return
        if (request != null && state.value.connected) gate.submit(tile, state.value.guard, request)
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
