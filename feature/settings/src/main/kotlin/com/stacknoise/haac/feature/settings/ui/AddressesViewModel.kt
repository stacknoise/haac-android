package com.stacknoise.haac.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.common.ui.CertificateDialogKind
import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.network.endpoint.AddressSlot
import com.stacknoise.haac.core.network.server.CleartextPolicy
import com.stacknoise.haac.core.network.server.ServerUrlNormalizer
import com.stacknoise.haac.core.network.tls.CertificateProbe

import com.stacknoise.haac.feature.settings.data.CertificatePins
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
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

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
    val certificate: CertificateReview? = null,
)

/** Addresses of the active instance: edit, remove, take over from HA, always use the internal address. */
@Suppress("TooManyFunctions") // one small handler per action of the addresses section
@HiltViewModel
class AddressesViewModel @Inject constructor(
    active: ActiveInstanceStore,
    servers: ServerDao,
    private val repository: InstanceAddressRepository,
    private val certificates: CertificatePins,
    private val probe: CertificateProbe,
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
            try {
                repository.change(id, slot, url)
                progress.update { it.copy(editing = null, input = "") }
            } catch (e: HaacException) {
                // A certificate the device does not know can be trusted on first use (concept 4.3).
                if (e.code != ErrorCode.NET_CERTIFICATE_UNTRUSTED || !url.isHttps) throw e
                review(id, slot, url, saving = true)
            }
        }
    }

    /** *Certificate* of the address in [slot]: shows what the server presents and whether it is pinned. */
    fun onCertificate(slot: AddressSlot) = perform { id ->
        val current = state.value
        val address = if (slot == AddressSlot.INTERNAL) current.internal else current.external
        address?.toHttpUrlOrNull()?.let { review(id, slot, it, saving = false) }
    }

    /** Confirms the certificate dialog: pins, re-pins or unpins the address, or continues a pending save. */
    fun onCertificateConfirm() {
        val review = state.value.certificate ?: return
        progress.update { it.copy(certificate = null) }
        val hash = review.certificate.keyHash
        when {
            review.saving -> {
                certificates.trustForNow(review.url, hash)
                onSave()
            }
            review.kind == CertificateDialogKind.PINNED -> perform { id ->
                certificates.removePin(id, review.slot, review.url)
            }
            else -> perform { id -> certificates.pin(id, review.slot, review.url, hash) }
        }
    }

    /** Closes the certificate dialog; a pending new address stays unsaved. */
    fun onCertificateDismiss() {
        progress.update { it.copy(certificate = null) }
    }

    /** Reads the certificate of [url] and opens the dialog for it: new, changed or the pinned one. */
    private suspend fun review(id: String, slot: AddressSlot, url: HttpUrl, saving: Boolean) {
        val certificate = probe.inspect(url)
        val pinned = if (saving) null else certificates.pinOf(id, slot)
        val kind = when (pinned) {
            null -> CertificateDialogKind.TRUST_NEW
            certificate.keyHash -> CertificateDialogKind.PINNED
            else -> CertificateDialogKind.TRUST_CHANGED
        }
        progress.update { it.copy(certificate = CertificateReview(slot, url, certificate, kind, saving)) }
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
