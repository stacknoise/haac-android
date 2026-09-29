package com.stacknoise.haac.feature.instance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.ErrorReporter
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.network.connection.ConnectionState
import com.stacknoise.haac.core.network.connection.ConnectionSupervisor
import com.stacknoise.haac.feature.instance.data.InstanceEditor
import com.stacknoise.haac.feature.instance.data.InstanceSwitcher
import com.stacknoise.haac.feature.instance.domain.InstanceItem
import com.stacknoise.haac.feature.instance.domain.RemoveOutcome
import com.stacknoise.haac.feature.instance.domain.SwitchStep
import com.stacknoise.haac.feature.instance.domain.toItems
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The instances for the switcher and the settings list, and the switch itself (concept 4.4). */
@HiltViewModel
class InstanceSwitcherViewModel @Inject constructor(
    active: ActiveInstanceStore,
    servers: ServerDao,
    supervisor: ConnectionSupervisor,
    private val switcher: InstanceSwitcher,
    private val editor: InstanceEditor,
    private val reporter: ErrorReporter,
) : ViewModel() {
    /** All instances with the active one marked. */
    val items: StateFlow<List<InstanceItem>> = combine(servers.observeAll(), active.activeServerId) { rows, id ->
        rows.toItems(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** The connection state of the active instance; inactive instances hold no connection (concept 4.4). */
    val connection: StateFlow<ConnectionState> = supervisor.state

    /** Switches to [id]; [onStep] tells what follows (main area, unlock or login). Errors go to the list. */
    fun onSelect(id: String, onStep: (SwitchStep) -> Unit) {
        if (items.value.firstOrNull { it.id == id }?.active == true) return
        report(id) { onStep(switcher.switchTo(id)) }
    }

    /** Stores the local [name] and [accent] colour of instance [id]. */
    fun onSave(id: String, name: String, accent: Long) = report(id) { editor.setAppearance(id, name, accent) }

    /** Removes instance [id]; [onRemoved] tells what follows (concept 4.4). */
    fun onRemove(id: String, onRemoved: (RemoveOutcome) -> Unit) = report(id) { onRemoved(editor.remove(id)) }

    /** Runs [block] in the background; an error goes to the notification list for instance [id]. */
    private fun report(id: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: HaacException) {
                reporter.report(e, id)
            }
        }
    }

    /** State flow timeout. */
    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
