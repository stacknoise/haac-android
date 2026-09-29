package com.stacknoise.haac.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.network.connection.ConnectionState
import com.stacknoise.haac.core.network.connection.ConnectionSupervisor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * *Settings → Diagnostics* (concept 9.3): the live [connection] and what the last sync stored for the active
 * instance: [lastSyncAt] (epoch milliseconds), [revision], the HA version and the bridge API version.
 */
data class DiagnosticsUiState(
    val connection: ConnectionState = ConnectionState.Idle,
    val haVersion: String? = null,
    val bridgeApiVersion: Int? = null,
    val lastSyncAt: Long? = null,
    val revision: String? = null,
)

/** Connection and sync facts of the active instance for the diagnostics section. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    active: ActiveInstanceStore,
    servers: ServerDao,
    supervisor: ConnectionSupervisor,
) : ViewModel() {
    /** The current section state. */
    val state: StateFlow<DiagnosticsUiState> = combine(
        active.activeServerId.flatMapLatest { id -> if (id == null) flowOf(null) else servers.observe(id) },
        supervisor.state,
    ) { server, connection ->
        DiagnosticsUiState(
            connection = connection,
            haVersion = server?.haVersion,
            bridgeApiVersion = server?.bridgeApiVersion,
            lastSyncAt = server?.lastSyncAt,
            revision = server?.exposureRevision,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DiagnosticsUiState())

    /** State flow timeout. */
    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
