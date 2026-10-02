package com.stacknoise.haac.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.settings.AppearanceSettings
import com.stacknoise.haac.core.database.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The design mode of *Settings → Appearance* (concept 15.2) and the change of it. */
@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val settings: AppearanceSettings,
) : ViewModel() {
    /** The current mode. */
    val mode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ThemeMode.SYSTEM)

    /** Stores the mode the user picked; the app theme follows at once. */
    fun onModeChanged(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    /** State flow timeout. */
    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
