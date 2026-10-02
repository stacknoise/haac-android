package com.stacknoise.haac.feature.schedules.ui

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Which schedules the list shows: all visible ones, or only the user's own (admins, design 3b A1). */
enum class ScheduleFilter {
    ALL,
    MINE,
}

/**
 * What the list shows; [schedules] is null until they are read. Editing works only while [connected]; [stale]
 * dims the cached schedules after a longer outage (concept 14.1, 19.7).
 */
data class SchedulesUiState(
    val schedules: List<ScheduleView>? = null,
    val filter: ScheduleFilter = ScheduleFilter.ALL,
    val connected: Boolean = false,
    val stale: Boolean = false,
    val error: ErrorCode? = null,
) {
    /** True if the cards can be switched; a stale list is never editable. */
    val editable: Boolean get() = connected && !stale

    /** True if there are schedules of other users, which only admins see: then the filter and owners show. */
    val admin: Boolean get() = schedules.orEmpty().any { !it.item.own }

    /** The schedules the filter lets through. */
    val visible: List<ScheduleView> get() = schedules.orEmpty().filter { filter == ScheduleFilter.ALL || it.item.own }

    /** The schedule that runs next among the shown ones, for the banner. */
    val nextUp: ScheduleView?
        get() = visible.filter { it.item.nextRun != null }.minByOrNull { it.item.nextRun ?: Long.MAX_VALUE }
}

/** The Schedules tab (M-10, M-11, M-16, concept 19.7): the cached schedules of the active instance. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SchedulesViewModel @Inject constructor(
    active: ActiveInstanceStore,
    repository: ScheduleRepository,
    live: LiveConnection,
    private val commands: ScheduleCommands,
    private val failures: ScheduleFailures,
) : ViewModel() {
    private val serverId = active.activeServerId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val filter = MutableStateFlow(ScheduleFilter.ALL)
    private val failure = MutableStateFlow<ErrorCode?>(null)

    private val schedules: Flow<List<ScheduleView>?> = active.activeServerId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.views(id)
    }

    private val link: Flow<Pair<Boolean, Boolean>> = combine(live.connected, live.stale) { c, s -> c to s }

    /** The state of the screen. */
    val state: StateFlow<SchedulesUiState> = combine(schedules, filter, link, failure) { list, shown, link, error ->
        SchedulesUiState(list, shown, link.first, link.second, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SchedulesUiState())

    /** Chooses which schedules are listed. */
    fun onFilter(value: ScheduleFilter) {
        filter.value = value
    }

    /** The switch of a card: turns [view] on or off on the server; a failure is shown and reported. */
    fun onToggle(view: ScheduleView, enabled: Boolean) {
        val id = serverId.value ?: return
        viewModelScope.launch { failure.value = failures.run(id) { commands.setEnabled(id, view.item, enabled) } }
    }

    /** The error message was shown. */
    fun onErrorShown() {
        failure.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
