package com.stacknoise.haac.feature.settings.ui

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.database.settings.SecuritySettings
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.network.session.InstanceSignOut
import com.stacknoise.haac.core.security.biometric.FingerprintOutcome
import com.stacknoise.haac.core.security.biometric.FingerprintTarget
import com.stacknoise.haac.core.security.biometric.FingerprintUnlock
import com.stacknoise.haac.core.security.token.TokenProtection
import com.stacknoise.haac.core.security.token.TokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The active instance as the settings show it. */
data class ActiveInstance(val id: String, val name: String, val userName: String)

/** Settings → Security (concept 5.4, 5.5). */
data class SecurityUiState(
    val fingerprintAvailable: Boolean = false,
    val fingerprintEnabled: Boolean = false,
    val unlockWindowSelectable: Boolean = false,
    val unlockWindowMinutes: Int = 0,
    val lockTimeoutMinutes: Int = SecuritySettings.DEFAULT_LOCK_TIMEOUT_MINUTES,
)

/** What the settings screen shows. */
data class SettingsUiState(
    val instance: ActiveInstance? = null,
    val security: SecurityUiState = SecurityUiState(),
    val busy: Boolean = false,
    val error: ErrorCode? = null,
    val signedOutServerId: String? = null,
)

/** Settings of the active instance: account, logout and security (concept 5.2, 5.4, 5.5, 15.4). */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    active: ActiveInstanceStore,
    private val servers: ServerDao,
    private val signOut: InstanceSignOut,
    private val tokens: TokenStore,
    private val fingerprint: FingerprintUnlock,
    private val security: SecuritySettings,
) : ViewModel() {
    private val progress = MutableStateFlow(SettingsUiState())
    private val fingerprintEnabled = MutableStateFlow(false)

    /** The current screen state. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<SettingsUiState> = combine(
        active.activeServerId.flatMapLatest { id -> if (id == null) flowOf(null) else servers.observe(id) },
        progress,
        fingerprintEnabled,
        security.unlockWindowMinutes,
        security.lockTimeoutMinutes,
    ) { server, current, enabled, window, timeout ->
        current.copy(
            instance = server?.let { ActiveInstance(it.id, it.displayName, it.haUserName) },
            security = SecurityUiState(
                fingerprintAvailable = fingerprint.isAvailable(),
                fingerprintEnabled = enabled,
                unlockWindowSelectable = fingerprint.supportsUnlockWindow,
                unlockWindowMinutes = window,
                lockTimeoutMinutes = timeout,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState())

    init {
        viewModelScope.launch {
            active.activeServerId.collect { id ->
                fingerprintEnabled.value = try {
                    id != null && tokens.protection(id) is TokenProtection.Fingerprint
                } catch (e: HaacException) {
                    progress.update { it.copy(error = e.code) }
                    false
                }
            }
        }
    }

    /**
     * Logout: revokes the refresh token in HA and deletes it locally (concept 5.2). The instance and its
     * layout stay, so the next start asks for the login of this instance (4.1). The token is revoked at the
     * address chosen by 4.5; if no address answers it is deleted anyway and expires in HA later.
     */
    fun onSignOut() {
        val instance = state.value.instance ?: return
        if (progress.value.busy) return
        progress.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                signOut.signOut(instance.id)
                progress.update { it.copy(busy = false, signedOutServerId = instance.id) }
            } catch (e: HaacException) {
                progress.update { it.copy(busy = false, error = e.code) }
            }
        }
    }

    /** Turns fingerprint unlock of the active instance on or off; both need a fingerprint (concept 5.4). */
    fun onFingerprintChanged(activity: FragmentActivity, enabled: Boolean) = withFingerprint(activity) { target ->
        val outcome = if (enabled) {
            fingerprint.enable(target, security.unlockWindowMinutes.first() * SECONDS_PER_MINUTE)
        } else {
            fingerprint.disable(target)
        }
        if (outcome == FingerprintOutcome.DONE) fingerprintEnabled.value = enabled
    }

    /** Stores a new unlock window; with fingerprint unlock on, the token is re-keyed first (concept 5.4). */
    fun onUnlockWindowChanged(activity: FragmentActivity, minutes: Int) = withFingerprint(activity) { target ->
        val done = !fingerprintEnabled.value ||
            fingerprint.changeUnlockWindow(target, minutes * SECONDS_PER_MINUTE) == FingerprintOutcome.DONE
        if (done) security.setUnlockWindowMinutes(minutes)
    }

    /** Stores the app lock timeout (concept 5.5). */
    fun onLockTimeoutChanged(minutes: Int) {
        viewModelScope.launch { security.setLockTimeoutMinutes(minutes) }
    }

    /**
     * Runs a fingerprint step for the active instance with the busy indicator. A changed fingerprint set
     * deletes the token (HAAC-SEC-001), so the screen leads to the login of this instance.
     */
    private fun withFingerprint(activity: FragmentActivity, block: suspend (FingerprintTarget) -> Unit) {
        val instance = state.value.instance ?: return
        if (progress.value.busy) return
        progress.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                block(FingerprintTarget(activity, instance.id, instance.name))
                progress.update { it.copy(busy = false) }
            } catch (e: HaacException) {
                val signedOut = if (e.code == ErrorCode.SEC_BIOMETRICS_CHANGED) instance.id else null
                progress.update { it.copy(busy = false, error = e.code, signedOutServerId = signedOut) }
            }
        }
    }

    /** State flow timeout and time conversion. */
    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val SECONDS_PER_MINUTE = 60
    }
}
