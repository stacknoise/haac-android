package com.stacknoise.haac.feature.schedules.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.network.connection.LiveConnection
import com.stacknoise.haac.core.network.connection.connected
import com.stacknoise.haac.core.network.connection.stale
import com.stacknoise.haac.feature.schedules.data.ScheduleCommands
import com.stacknoise.haac.feature.schedules.data.ScheduleFailures
import com.stacknoise.haac.feature.schedules.data.ScheduleRepository
import com.stacknoise.haac.feature.schedules.data.ScheduleView
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What the detail screen shows; [view] is null while it is read and, with [loaded], once the schedule is gone.
 * Changes work only while [connected] (concept 19.7).
 */
data class ScheduleDetailUiState(
    val view: ScheduleView? = null,
    val loaded: Boolean = false,
    val connected: Boolean = false,
    val stale: Boolean = false,
    val error: ErrorCode? = null,
) {
    /** True if the schedule can be changed; a stale one never. */
    val editable: Boolean get() = connected && !stale
}

/** The detail screen of one schedule (M-15, M-17, concept 19.7). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScheduleDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    active: ActiveInstanceStore,
    repository: ScheduleRepository,
    live: LiveConnection,
    private val commands: ScheduleCommands,
    private val failures: ScheduleFailures,
) : ViewModel() {
    private val scheduleId: String = checkNotNull(savedState[ID_ARG])
    private val serverId = active.activeServerId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val failure = MutableStateFlow<ErrorCode?>(null)
    private val deletedFlow = MutableStateFlow(false)

    /** The schedule, or null once it is gone, with the flag that it has been read. */
    private val schedule: Flow<Pair<Boolean, ScheduleView?>> = active.activeServerId.flatMapLatest { id ->
        if (id == null) flowOf(false to null) else repository.view(id, scheduleId).map { true to it }
    }

    private val link: Flow<Pair<Boolean, Boolean>> = combine(live.connected, live.stale) { c, s -> c to s }

    /** The state of the screen. */
    val state: StateFlow<ScheduleDetailUiState> = combine(schedule, link, failure) { read, link, error ->
        ScheduleDetailUiState(read.second, read.first, link.first, link.second, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ScheduleDetailUiState())

    /** True once the schedule was deleted from this screen; the screen then closes. */
    val deleted: StateFlow<Boolean> = deletedFlow

    /** The *Enabled* switch. */
    fun onEnabled(enabled: Boolean) {
        val id = serverId.value ?: return
        val view = state.value.view ?: return
        viewModelScope.launch { failure.value = failures.run(id) { commands.setEnabled(id, view.item, enabled) } }
    }

    /** *Delete schedule*, confirmed. */
    fun onDelete() {
        val id = serverId.value ?: return
        viewModelScope.launch {
            failure.value = failures.run(id) {
                commands.delete(id, scheduleId)
                deletedFlow.value = true
            }
        }
    }

    /** The error message was shown. */
    fun onErrorShown() {
        failure.value = null
    }

    /** Argument and timing. */
    companion object {
        /** Name of the navigation argument that holds the schedule id. */
        const val ID_ARG = "scheduleId"
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
