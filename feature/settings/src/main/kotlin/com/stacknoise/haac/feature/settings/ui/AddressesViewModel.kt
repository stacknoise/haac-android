package com.stacknoise.haac.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.network.endpoint.AddressSlot
import com.stacknoise.haac.core.network.server.CleartextPolicy
import com.stacknoise.haac.core.network.server.ServerUrlNormalizer
import com.stacknoise.haac.feature.settings.data.InstanceAddressRepository
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** *Settings → Addresses* of the active instance (concept 4.5). */
data class AddressesUiState(
    val serverId: String? = null,
    val internal: String? = null,
    val external: String? = null,
    val alwaysUseInternal: Boolean = false,
    val editing: AddressSlot? = null,
    val input: String = "",
    val busy: Boolean = false,
    val error: ErrorCode? = null,
)

/** Addresses of the active instance: edit, remove, take over from HA, always use the internal address. */
@HiltViewModel
class AddressesViewModel @Inject constructor(
    active: ActiveInstanceStore,
    servers: ServerDao,
    private val repository: InstanceAddressRepository,
) : ViewModel() {
    private val progress = MutableStateFlow(AddressesUiState())

    /** The current section state. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<AddressesUiState> = combine(
        active.activeServerId.flatMapLatest { id -> if (id == null) flowOf(null) else servers.observe(id) },
        progress,
    ) { server, current ->
        current.copy(
            serverId = server?.id,
            internal = server?.internalUrl,
            external = server?.externalUrl,
            alwaysUseInternal = server?.alwaysUseInternal ?: false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AddressesUiState())

    /** Turns *Always use the internal address* on or off. */
    fun onAlwaysUseInternalChanged(enabled: Boolean) = perform { id -> repository.setAlwaysUseInternal(id, enabled) }

    /** Opens the edit dialog for [slot] with its current address. */
    fun onEdit(slot: AddressSlot) {
        val current = if (slot == AddressSlot.INTERNAL) state.value.internal else state.value.external
        progress.update { it.copy(editing = slot, input = current.orEmpty(), error = null) }
    }

    /** Updates the address in the edit dialog. */
    fun onInputChanged(value: String) {
        progress.update { it.copy(input = value, error = null) }
    }

    /** Closes the edit dialog without changes. */
    fun onDismissEdit() {
        progress.update { it.copy(editing = null, input = "", error = null) }
    }

    /** Checks and stores the address of the edit dialog; errors keep the dialog open. */
    fun onSave() {
        val slot = state.value.editing ?: return
        val input = state.value.input
        perform { id ->
            val url = ServerUrlNormalizer.normalize(input).also(CleartextPolicy::requireAllowed)
            repository.change(id, slot, url)
            progress.update { it.copy(editing = null, input = "") }
        }
    }

    /** Removes the address in [slot]. */
    fun onRemove(slot: AddressSlot) = perform { id -> repository.remove(id, slot) }

    /** Takes the addresses from HA's network settings. */
    fun onUseHaAddresses() = perform { id -> repository.useAddressesFromHa(id) }

    /** Runs [block] for the active instance with the busy indicator; errors are shown with their code. */
    private fun perform(block: suspend (String) -> Unit) {
        val id = state.value.serverId
        if (id == null || progress.value.busy) return
        progress.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                block(id)
                progress.update { it.copy(busy = false) }
            } catch (e: HaacException) {
                progress.update { it.copy(busy = false, error = e.code) }
            }
        }
    }

    /** State flow timeout. */
    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
