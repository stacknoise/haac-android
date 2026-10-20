package com.stacknoise.haac.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.settings.DeviceName
import com.stacknoise.haac.core.database.settings.DeviceNameSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The device name of *Settings → Device name* (concept 21.5) and the change of it. */
@HiltViewModel
class DeviceNameViewModel @Inject constructor(
    private val settings: DeviceNameSettings,
) : ViewModel() {
    /** The stored name, the device model until the user changes it, or empty when none is sent. */
    val name: StateFlow<String> = settings.deviceName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), "")

    /** Stores the name the user typed, cut to what the bridge accepts. */
    fun onNameChanged(name: String) {
        viewModelScope.launch { settings.setDeviceName(name) }
    }

    /** State flow timeout and the length limit shown to the field. */
    companion object {
        /** How long the flow stays active without a collector. */
        private const val STOP_TIMEOUT_MS = 5_000L

        /** The longest name the field accepts. */
        const val MAX_LENGTH = DeviceName.MAX_LENGTH
    }
}
