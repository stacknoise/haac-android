package com.stacknoise.haac.app.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.ErrorAction
import com.stacknoise.haac.core.network.connection.ConnectionState
import com.stacknoise.haac.core.network.connection.ConnectionSupervisor
import com.stacknoise.haac.core.network.demo.DemoInstance
import com.stacknoise.haac.feature.entities.data.EntitySync
import com.stacknoise.haac.feature.schedules.data.ScheduleAvailability
import com.stacknoise.haac.feature.schedules.data.ScheduleSync
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Connects the active instance while the main area is visible and syncs it on every new connection (concept
 * 9.1, 11.4): the WebSocket is closed in the background and rebuilt with the reconnect steps when the app returns.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val supervisor: ConnectionSupervisor,
    private val active: ActiveInstanceStore,
    private val sync: EntitySync,
    private val scheduleSync: ScheduleSync,
    availability: ScheduleAvailability,
    private val servers: ServerDao,
) : ViewModel() {
    private val serverId = MutableStateFlow<String?>(null)
    private var following: Job? = null

    /** The active instance while the main area is followed; a change resets the screens (concept 4.4). */
    val activeId: StateFlow<String?> = serverId

    /** True while the Schedules tab is shown: the bridge reports the feature or schedules are cached (concept 19.7). */
    val schedulesShown: StateFlow<Boolean> = serverId.flatMapLatest { id ->
        id?.let { availability.shown(it) } ?: flowOf(false)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** True while the active instance is the demo, which shows its banner on Rooms and Schedules (concept 20.4). */
    val isDemo: StateFlow<Boolean> = serverId.map { it != null && DemoInstance.isDemo(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** The connection state for the banner. */
    val state: StateFlow<ConnectionState> = supervisor.state

    /** True while the active instance has an internal address, which needs local network access (concept 4.5). */
    val hasInternalAddress: StateFlow<Boolean> = serverId.flatMapLatest { id ->
        id?.let { servers.observe(it) }?.map { it?.internalUrl != null } ?: flowOf(false)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** The instance whose token HA no longer accepts (action *Sign in*), or null (concept 14.1). */
    val signInRequired: StateFlow<String?> = combine(supervisor.state, serverId) { state, id ->
        id.takeIf { (state as? ConnectionState.Failed)?.error?.code?.action == ErrorAction.SIGN_IN }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The main area is visible: follow the active instance, also when it changes, and sync each connection. */
    fun onForeground() {
        following?.cancel()
        following = viewModelScope.launch {
            launch {
                supervisor.connection.collectLatest { connection ->
                    val id = serverId.value
                    if (connection != null && id != null) {
                        coroutineScope {
                            launch { sync.follow(id, connection) }
                            launch { scheduleSync.follow(id, connection) }
                        }
                    }
                }
            }
            active.activeServerId.collect { id ->
                serverId.value = id
                if (id == null) supervisor.stop() else supervisor.start(id)
            }
        }
    }

    /** The main area is hidden or the app went to the background: close the connection. */
    fun onBackground() {
        following?.cancel()
        following = null
        supervisor.stop()
    }

    /** *Try again* in the banner. */
    fun retry() = supervisor.retry()

    /** Leaving the main area, e.g. after sign-out or an app lock, ends the connection. */
    override fun onCleared() {
        supervisor.stop()
    }
}
