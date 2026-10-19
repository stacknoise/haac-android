package com.stacknoise.haac.feature.entities.disable.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.network.connection.LiveConnection
import com.stacknoise.haac.core.network.connection.connected
import com.stacknoise.haac.feature.entities.data.ControlFailures
import com.stacknoise.haac.feature.entities.disable.data.DisableCommands
import com.stacknoise.haac.feature.entities.disable.data.DisablePreviews
import com.stacknoise.haac.feature.entities.disable.domain.DisablePreview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Deactivating and activating entities of the active instance for the room grid and the detail screen (concept
 * 7.5, M-20, M-21). A failed command is shown like a failed service call: snackbar with the code and an entry in
 * the notification list.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DisableViewModel @Inject constructor(
    private val active: ActiveInstanceStore,
    live: LiveConnection,
    private val commands: DisableCommands,
    private val previews: DisablePreviews,
    private val failures: ControlFailures,
) : ViewModel() {
    private val serverId = active.activeServerId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val switchedOn =
        MutableSharedFlow<Int>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** True while a connection is open; the state lives on the bridge, so nothing changes offline (concept 7.5). */
    val connected: StateFlow<Boolean> =
        live.connected.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    /** The number of schedules the bridge switched on again after an activation, for the snackbar (M-21). */
    val reactivated: SharedFlow<Int> = switchedOn.asSharedFlow()

    /** The schedules a change of [entityId] switches off or on, for the dialog and the sheet. */
    fun preview(entityId: String): Flow<DisablePreview> = active.activeServerId.flatMapLatest { id ->
        if (id == null) flowOf(DisablePreview()) else previews.of(id, entityId)
    }

    /** Deactivates [entityId] on the bridge. */
    fun onDisable(entityId: String) = change(entityId, disabled = true)

    /** Activates [entityId] on the bridge. */
    fun onEnable(entityId: String) = change(entityId, disabled = false)

    /** Sends the change; a failure goes to the snackbar and the notification list. */
    private fun change(entityId: String, disabled: Boolean) {
        val id = serverId.value ?: return
        viewModelScope.launch {
            try {
                val schedules = commands.set(id, entityId, disabled)
                if (!disabled && schedules.isNotEmpty()) switchedOn.tryEmit(schedules.size)
            } catch (e: HaacException) {
                failures.handle(id, e)
            }
        }
    }

    /** Sharing timeout. */
    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
