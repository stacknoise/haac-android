package com.stacknoise.haac.feature.onboarding.ui

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.error.BridgeException
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.network.auth.AuthTokens
import com.stacknoise.haac.core.network.discovery.DiscoveredServer
import com.stacknoise.haac.core.network.discovery.ServerDiscovery
import com.stacknoise.haac.core.network.server.CleartextPolicy
import com.stacknoise.haac.core.network.server.ServerUrlNormalizer
import com.stacknoise.haac.core.network.tls.CertificateProbe
import com.stacknoise.haac.core.network.tls.PinRegistry
import com.stacknoise.haac.feature.onboarding.domain.DemoEntry
import com.stacknoise.haac.feature.onboarding.domain.FingerprintStep
import com.stacknoise.haac.feature.onboarding.domain.KnownServer
import com.stacknoise.haac.feature.onboarding.domain.ServerValidator
import com.stacknoise.haac.feature.onboarding.domain.SignInRepository
import com.stacknoise.haac.feature.onboarding.domain.SignInResult
import com.stacknoise.haac.feature.onboarding.domain.SignInStep
import com.stacknoise.haac.feature.onboarding.domain.SignInTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl

/** Which part of M-01 is shown. */
enum class SignInStage {
    /** Server, username and password. */
    CREDENTIALS,

    /** MFA code (concept 5.1). */
    CODE,

    /** Signed in, but HAAC Bridge is missing or too old (concept 4.2 step 4, 14.1). */
    BRIDGE,
}

/** Everything M-01 shows. */
data class OnboardingUiState(
    val servers: List<DiscoveredServer> = emptyList(),
    val scanning: Boolean = true,
    val selectedUrl: String? = null,
    val manualEntry: Boolean = false,
    val manualUrl: String = "",
    val knownServer: KnownServer? = null,
    val username: String = "",
    val code: String = "",
    val stage: SignInStage = SignInStage.CREDENTIALS,
    val busy: Boolean = false,
    val error: ErrorCode? = null,
    val cleartextWarningFor: String? = null,
    val addressOffer: SignInResult.SameInstance? = null,
    val certificateOffer: CertificateOffer? = null,
    val fingerprintOffer: FingerprintOffer? = null,
    val signedInServerId: String? = null,
    val demoAvailable: Boolean = false,
)

/** The instance [serverId] (called [name]) is stored; the user may now turn on fingerprint unlock (concept 4.4). */
data class FingerprintOffer(val serverId: String, val name: String)

/**
 * M-01 (concept 4.2, 4.3, 5.1): pick or enter a server, sign in with username, password and optional
 * MFA code, check HAAC Bridge and store the instance. With the navigation argument `serverId` it signs
 * in again to that stored instance (concept 4.1) instead of discovering servers.
 *
 * The password is never part of the state: the screen hands over a copy per attempt, which is wiped.
 */
@Suppress("TooManyFunctions") // one small handler per input of M-01
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val discovery: ServerDiscovery,
    private val validator: ServerValidator,
    private val repository: SignInRepository,
    private val probe: CertificateProbe,
    private val pins: PinRegistry,
    private val fingerprint: FingerprintStep,
    private val demo: DemoEntry,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())

    /** The current screen state. */
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    private val confirmedCleartext = mutableSetOf<HttpUrl>()
    private var target: SignInTarget? = null
    private var flowId: String? = null
    private var pendingTokens: AuthTokens? = null
    private var discoveryJob: Job? = null

    init {
        val serverId = savedState.get<String>(SERVER_ID_ARG)
        if (serverId == null) {
            discover()
            offerDemo()
        } else {
            loadKnownServer(serverId)
        }
    }

    /** *Try the demo* is shown while no demo instance exists (concept 20.4); an error here only hides it. */
    private fun offerDemo() {
        viewModelScope.launch {
            val available = try {
                demo.available()
            } catch (_: HaacException) {
                false
            }
            _state.update { it.copy(demoAvailable = available) }
        }
    }

    /** *Try the demo*: creates the demo instance and opens it; there is no login and no fingerprint offer. */
    fun onTryDemo() {
        if (_state.value.busy) return
        perform {
            val id = demo.start()
            _state.update { it.copy(busy = false, signedInServerId = id) }
        }
    }

    /**
     * LAN discovery while the screen is open. The scan indicator stops after [SCAN_WINDOW_MS] or when discovery
     * ends; [onRescan] then starts it again, which also recovers a discovery that got stuck.
     */
    private fun discover() {
        discoveryJob?.cancel()
        _state.update { it.copy(scanning = true, servers = emptyList()) }
        discoveryJob = viewModelScope.launch {
            launch {
                delay(SCAN_WINDOW_MS)
                _state.update { it.copy(scanning = false) }
            }
            discovery.servers().collect { servers ->
                _state.update { it.copy(servers = servers, selectedUrl = it.selectedUrl ?: servers.firstOrNull()?.url) }
            }
            _state.update { it.copy(scanning = false) }
        }
    }

    /** *Scan again*: searches the network once more, unless a scan is running. */
    fun onRescan() {
        if (!_state.value.scanning && _state.value.knownServer == null) discover()
    }

    /** Shows the stored instance and its user; falls back to discovery if it was removed meanwhile. */
    private fun loadKnownServer(serverId: String) {
        viewModelScope.launch {
            val known = try {
                repository.knownServer(serverId)
            } catch (e: HaacException) {
                _state.update { it.copy(error = e.code) }
                null
            }
            if (known == null) {
                discover()
            } else {
                _state.update { it.copy(knownServer = known, username = known.userName, scanning = false) }
            }
        }
    }

    /** Selects a discovered server and leaves manual entry. */
    fun onServerSelected(server: DiscoveredServer) {
        _state.update { it.copy(selectedUrl = server.url, manualEntry = false, error = null) }
    }

    /** Opens the manual address field (*Other address…*). */
    fun onOtherAddress() {
        _state.update { it.copy(manualEntry = true, error = null) }
    }

    /** Updates the manual address. */
    fun onManualUrlChanged(value: String) {
        _state.update { it.copy(manualUrl = value, error = null) }
    }

    /** Updates the username. */
    fun onUsernameChanged(value: String) {
        _state.update { it.copy(username = value, error = null) }
    }

    /** Updates the MFA code. */
    fun onCodeChanged(value: String) {
        _state.update { it.copy(code = value.filter(Char::isLetterOrDigit), error = null) }
    }

    /**
     * Signs in with [password]; the array is wiped when the attempt ends. For an unencrypted address
     * the warning dialog comes first; after confirming, the screen calls this again.
     */
    fun onSignIn(password: CharArray) {
        val current = _state.value
        val url = if (current.busy || current.username.isBlank()) null else chosenUrl()
        if (url == null || needsCleartextWarning(url)) {
            password.fill('\u0000')
            return
        }
        target = SignInTarget(url, displayName(url), current.knownServer?.id)
        perform {
            try {
                validator.validate(url)
                handle(repository.signIn(url, current.username.trim(), password))
            } finally {
                password.fill('\u0000')
            }
        }
    }

    /** Sends the MFA code; a wrong code keeps the code step. */
    fun onSubmitCode() {
        val url = target?.url
        val flow = flowId
        val code = _state.value.code
        val ready = code.isNotBlank() && !_state.value.busy
        if (url != null && flow != null && ready) {
            perform { handle(repository.submitCode(url, flow, code)) }
        }
    }

    /** Repeats the bridge check with the tokens of the last sign-in, e.g. after installing HAAC Bridge. */
    fun onRetryBridgeCheck() {
        if (!_state.value.busy) perform { finish() }
    }

    /** Leaves the code or bridge step; unused tokens are revoked. */
    fun onStartOver() {
        discardTokens()
        flowId = null
        _state.update { it.copy(stage = SignInStage.CREDENTIALS, code = "", error = null, busy = false) }
    }

    /** The user accepted the unencrypted connection in the warning dialog. */
    fun onCleartextConfirmed() {
        val url = _state.value.cleartextWarningFor ?: return
        confirmedCleartext += ServerUrlNormalizer.normalize(url)
        _state.update { it.copy(cleartextWarningFor = null) }
    }

    /** Adds the address to the stored instance of the offer and opens it (concept 4.5). */
    fun onAddAddress() {
        val offer = _state.value.addressOffer
        val url = target?.url
        val tokens = pendingTokens
        if (offer == null || url == null || tokens == null) return
        _state.update { it.copy(addressOffer = null) }
        perform {
            repository.addAddress(offer.serverId, url, tokens)
            pendingTokens = null
            _state.update { it.copy(busy = false, signedInServerId = offer.serverId) }
        }
    }

    /** Turns fingerprint unlock on for the new instance, then opens it; a failed or cancelled prompt leaves it off. */
    fun onEnableFingerprint(activity: FragmentActivity) {
        val offer = _state.value.fingerprintOffer ?: return
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            fingerprint.enable(activity, offer.serverId, offer.name)
            _state.update { it.copy(busy = false, fingerprintOffer = null, signedInServerId = offer.serverId) }
        }
    }

    /** *Not now*: opens the instance without fingerprint unlock; the settings can turn it on later. */
    fun onSkipFingerprint() {
        val offer = _state.value.fingerprintOffer ?: return
        _state.update { it.copy(fingerprintOffer = null, signedInServerId = offer.serverId) }
    }

    /** Declines the offer; the tokens of the new sign-in are revoked. */
    fun onAddressOfferDismissed() {
        _state.update { it.copy(addressOffer = null) }
        onStartOver()
    }

    /** The user declined the unencrypted connection. */
    fun onCleartextDismissed() {
        _state.update { it.copy(cleartextWarningFor = null) }
    }

    /** Shows the warning dialog for `http://` unless it was confirmed or the instance is already stored. */
    private fun needsCleartextWarning(url: HttpUrl): Boolean {
        val needed = CleartextPolicy.needsWarning(url) && url !in confirmedCleartext && _state.value.knownServer == null
        if (needed) _state.update { it.copy(cleartextWarningFor = url.toString()) }
        return needed
    }

    /** Normalised address of the stored, selected or entered server; shows the error and returns null if unusable. */
    private fun chosenUrl(): HttpUrl? {
        val current = _state.value
        val input = current.knownServer?.url ?: (if (current.manualEntry) current.manualUrl else current.selectedUrl)
        return try {
            ServerUrlNormalizer.normalize(input ?: return null).also(CleartextPolicy::requireAllowed)
        } catch (e: HaacException) {
            _state.update { it.copy(error = e.code) }
            null
        }
    }

    /** Name of a new instance: stored name, zeroconf `location_name` or host (concept 4.4). */
    private fun displayName(url: HttpUrl): String {
        val current = _state.value
        current.knownServer?.let { return it.displayName }
        val discovered = current.servers.firstOrNull { !current.manualEntry && it.url == current.selectedUrl }
        return discovered?.name ?: url.host
    }

    /** Continues after a flow step: code step, or bridge check and storage. */
    private suspend fun handle(step: SignInStep) {
        when (step) {
            is SignInStep.CodeRequired -> {
                flowId = step.flowId
                _state.update { it.copy(stage = SignInStage.CODE, code = "", busy = false) }
            }
            is SignInStep.Authorized -> {
                pendingTokens = step.tokens
                finish()
            }
        }
    }

    /**
     * Bridge check and storage; a missing or outdated bridge keeps the tokens for another try, and a known
     * server with the same user leads to the address offer (concept 4.5).
     */
    private suspend fun finish() {
        val signInTarget = target ?: return
        val tokens = pendingTokens ?: return
        try {
            when (val result = repository.finish(signInTarget, _state.value.username.trim(), tokens)) {
                is SignInResult.Saved -> {
                    pendingTokens = null
                    val offer = FingerprintOffer(result.serverId, signInTarget.displayName)
                    _state.update {
                        if (fingerprint.available()) {
                            it.copy(busy = false, fingerprintOffer = offer)
                        } else {
                            it.copy(busy = false, signedInServerId = result.serverId)
                        }
                    }
                }
                is SignInResult.SameInstance -> _state.update { it.copy(busy = false, addressOffer = result) }
            }
        } catch (e: BridgeException) {
            if (e.code == ErrorCode.BRG_NOT_INSTALLED || e.code == ErrorCode.BRG_UPDATE_REQUIRED) {
                _state.update { it.copy(stage = SignInStage.BRIDGE, busy = false, error = e.code) }
            } else {
                throw e
            }
        }
    }

    /** Runs one step with the busy indicator; errors are shown with their code (concept 17.4). */
    private fun perform(block: suspend () -> Unit) {
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: HaacException) {
                fail(e.code)
            }
        }
    }

    /** The user trusts the certificate of the offer: its key is pinned for the address; the sign-in can be retried. */
    fun onCertificateTrusted() {
        val offer = _state.value.certificateOffer ?: return
        pins.trust(offer.url, offer.certificate.keyHash)
        _state.update { it.copy(certificateOffer = null, error = null) }
    }

    /** The user does not trust the certificate; the sign-in stays blocked with HAAC-NET-007. */
    fun onCertificateDismissed() {
        _state.update { it.copy(certificateOffer = null, error = ErrorCode.NET_CERTIFICATE_UNTRUSTED) }
    }

    /** Reads the certificate of [url] and asks the user to trust it (concept 4.3); without one the error is shown. */
    private fun offerCertificate(url: HttpUrl) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(busy = false, certificateOffer = CertificateOffer(url, probe.inspect(url))) }
            } catch (e: HaacException) {
                _state.update { it.copy(busy = false, error = e.code) }
            }
        }
    }

    /** Shows [code]; an aborted flow or failed save returns to the credentials. */
    private fun fail(code: ErrorCode) {
        val untrusted = target?.url?.takeIf { it.isHttps && code == ErrorCode.NET_CERTIFICATE_UNTRUSTED }
        if (untrusted != null) {
            offerCertificate(untrusted)
            return
        }
        val keepStage = code == ErrorCode.AUTH_INVALID_MFA_CODE ||
            (_state.value.stage == SignInStage.BRIDGE && code.area == "NET")
        if (!keepStage) {
            discardTokens()
            flowId = null
        }
        _state.update {
            it.copy(
                busy = false,
                error = code,
                code = if (code == ErrorCode.AUTH_INVALID_MFA_CODE) "" else it.code,
                stage = if (keepStage) it.stage else SignInStage.CREDENTIALS,
            )
        }
    }

    /** Revokes tokens of a sign-in that will not be completed. */
    private fun discardTokens() {
        val tokens = pendingTokens ?: return
        val url = target?.url ?: return
        pendingTokens = null
        viewModelScope.launch { repository.discard(url, tokens) }
    }

    /** Navigation argument names. */
    companion object {
        /** Id of the stored instance to sign in to again (concept 4.1). */
        const val SERVER_ID_ARG = "serverId"

        /** How long the scan indicator shows before *Scan again* is offered. */
        const val SCAN_WINDOW_MS = 10_000L
    }
}
