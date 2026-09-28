package com.stacknoise.haac.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.network.session.InstanceSessionFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl

/** The active instance as the settings show it. */
data class ActiveInstance(val id: String, val name: String, val url: String, val userName: String)

/** What the settings screen shows. */
data class SettingsUiState(
    val instance: ActiveInstance? = null,
    val busy: Boolean = false,
    val signedOutServerId: String? = null,
)

/** Settings of the active instance; for now account and logout (concept 5.2, 15.4). */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    active: ActiveInstanceStore,
    servers: ServerDao,
    private val sessions: InstanceSessionFactory,
) : ViewModel() {
    private val progress = MutableStateFlow(SettingsUiState())

    /** The current screen state. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<SettingsUiState> = combine(
        active.activeServerId.flatMapLatest { id -> if (id == null) flowOf(null) else servers.observe(id) },
        progress,
    ) { server, current ->
        current.copy(instance = server?.let { ActiveInstance(it.id, it.displayName, it.baseUrl, it.haUserName) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState())

    /**
     * Logout: revokes the refresh token in HA and deletes it locally (concept 5.2). The instance and its
     * layout stay, so the next start asks for the login of this instance (4.1). If HA is unreachable the
     * token is deleted anyway and expires in HA later.
     */
    fun onSignOut() {
        val instance = state.value.instance ?: return
        if (progress.value.busy) return
        progress.value = progress.value.copy(busy = true)
        viewModelScope.launch {
            sessions.create(instance.id, instance.url.toHttpUrl()).signOut()
            progress.value = progress.value.copy(busy = false, signedOutServerId = instance.id)
        }
    }

    /** How long the state flow stays active without collectors. */
    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
