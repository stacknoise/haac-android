package com.stacknoise.haac.app.lock

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.settings.SecuritySettings
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.security.biometric.FingerprintOutcome
import com.stacknoise.haac.core.security.biometric.FingerprintTarget
import com.stacknoise.haac.core.security.biometric.FingerprintUnlock
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where the unlock screen leads. */
enum class UnlockResult {
    /** Token unlocked: main area. */
    UNLOCKED,

    /** *Use password* or a fingerprint change: HA login of this instance (concept 5.4 steps 5, 6). */
    SIGN_IN,
}

/** What the unlock screen shows. */
data class UnlockUiState(
    val instanceName: String = "",
    val busy: Boolean = false,
    val error: ErrorCode? = null,
    val result: UnlockResult? = null,
)

/** Unlock screen of one fingerprint-protected instance (concept 5.4, 5.5, 15.4). */
@HiltViewModel
class UnlockViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val servers: ServerDao,
    private val fingerprint: FingerprintUnlock,
    private val settings: SecuritySettings,
) : ViewModel() {
    private val serverId: String = savedState.get<String>(SERVER_ID_ARG).orEmpty()
    private val _state = MutableStateFlow(UnlockUiState())

    /** The current screen state. */
    val state: StateFlow<UnlockUiState> = _state.asStateFlow()

    /** Shows the fingerprint prompt in [activity]; a changed fingerprint set leads to the password login. */
    fun onUnlock(activity: FragmentActivity) {
        if (_state.value.busy || _state.value.result != null) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val name = servers.get(serverId)?.displayName.orEmpty()
                _state.update { it.copy(instanceName = name) }
                val window = settings.unlockWindowMinutes.first() * SECONDS_PER_MINUTE
                val target = FingerprintTarget(activity, serverId, name)
                val result = when (fingerprint.unlock(target, window)) {
                    FingerprintOutcome.DONE -> UnlockResult.UNLOCKED
                    FingerprintOutcome.USE_PASSWORD -> UnlockResult.SIGN_IN
                    FingerprintOutcome.CANCELLED -> null
                }
                _state.update { it.copy(busy = false, result = result) }
            } catch (e: HaacException) {
                _state.update { it.copy(busy = false, error = e.code) }
            }
        }
    }

    /** *Use password*, also after HAAC-SEC-001. */
    fun onUsePassword() {
        _state.update { it.copy(result = UnlockResult.SIGN_IN) }
    }

    /** Navigation argument and time conversion. */
    companion object {
        /** Id of the instance to unlock. */
        const val SERVER_ID_ARG = "serverId"

        private const val SECONDS_PER_MINUTE = 60
    }
}
