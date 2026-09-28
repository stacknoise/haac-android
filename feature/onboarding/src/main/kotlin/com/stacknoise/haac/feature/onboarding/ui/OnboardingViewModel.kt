package com.stacknoise.haac.feature.onboarding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.network.server.CleartextPolicy
import com.stacknoise.haac.feature.onboarding.domain.DiscoveredServer
import com.stacknoise.haac.feature.onboarding.domain.ServerDiscovery
import com.stacknoise.haac.feature.onboarding.domain.ServerUrlNormalizer
import com.stacknoise.haac.feature.onboarding.domain.ServerValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl

/** Everything the server part of M-01 shows. */
data class OnboardingUiState(
    val servers: List<DiscoveredServer> = emptyList(),
    val scanning: Boolean = true,
    val selectedUrl: String? = null,
    val manualEntry: Boolean = false,
    val manualUrl: String = "",
    val checking: Boolean = false,
    val error: ErrorCode? = null,
    val cleartextWarningFor: String? = null,
    val validatedUrl: String? = null,
)

/** Server selection and validation of the onboarding screen (concept 4.2, 4.3). */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val discovery: ServerDiscovery,
    private val validator: ServerValidator,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())

    /** The current screen state. */
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    private val confirmedCleartext = mutableSetOf<HttpUrl>()

    init {
        viewModelScope.launch {
            discovery.servers().collect { servers ->
                _state.update { it.copy(servers = servers, selectedUrl = it.selectedUrl ?: servers.firstOrNull()?.url) }
            }
            _state.update { it.copy(scanning = false) }
        }
    }

    /** Selects a discovered server and leaves manual entry. */
    fun onServerSelected(server: DiscoveredServer) {
        _state.update { it.copy(selectedUrl = server.url, manualEntry = false, error = null, validatedUrl = null) }
    }

    /** Opens the manual address field (*Other address…*). */
    fun onOtherAddress() {
        _state.update { it.copy(manualEntry = true, error = null, validatedUrl = null) }
    }

    /** Updates the manual address. */
    fun onManualUrlChanged(value: String) {
        _state.update { it.copy(manualUrl = value, error = null, validatedUrl = null) }
    }

    /** Normalises the chosen address, asks for cleartext confirmation if needed, then validates it. */
    fun onContinue() {
        val url = chosenUrl() ?: return
        if (CleartextPolicy.needsWarning(url) && url !in confirmedCleartext) {
            _state.update { it.copy(cleartextWarningFor = url.toString()) }
        } else {
            validate(url)
        }
    }

    /** The normalised address of the selected or entered server; shows the error and returns null if unusable. */
    private fun chosenUrl(): HttpUrl? {
        val current = _state.value
        val input = (if (current.manualEntry) current.manualUrl else current.selectedUrl) ?: return null
        return try {
            ServerUrlNormalizer.normalize(input).also(CleartextPolicy::requireAllowed)
        } catch (e: HaacException) {
            _state.update { it.copy(error = e.code) }
            null
        }
    }

    /** The user accepted the unencrypted connection in the warning dialog. */
    fun onCleartextConfirmed() {
        val url = _state.value.cleartextWarningFor ?: return
        _state.update { it.copy(cleartextWarningFor = null) }
        val parsed = ServerUrlNormalizer.normalize(url)
        confirmedCleartext += parsed
        validate(parsed)
    }

    /** The user declined the unencrypted connection. */
    fun onCleartextDismissed() {
        _state.update { it.copy(cleartextWarningFor = null) }
    }

    /** Runs the server check and shows its result or error code. */
    private fun validate(url: HttpUrl) {
        _state.update { it.copy(checking = true, error = null, validatedUrl = null) }
        viewModelScope.launch {
            try {
                val server = validator.validate(url)
                _state.update { it.copy(checking = false, validatedUrl = server.url.toString()) }
            } catch (e: HaacException) {
                _state.update { it.copy(checking = false, error = e.code) }
            }
        }
    }
}
