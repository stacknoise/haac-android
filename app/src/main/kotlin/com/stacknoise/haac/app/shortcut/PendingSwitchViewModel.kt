package com.stacknoise.haac.app.shortcut

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** Hands the instance an app shortcut asked for to the main area (concept 4.4). */
@HiltViewModel
class PendingSwitchViewModel @Inject constructor(private val pending: PendingInstanceSwitch) : ViewModel() {
    /** The instance to switch to, or null. */
    val serverId: StateFlow<String?> = pending.serverId

    /** The main area took the request over. */
    fun onHandled() = pending.clear()
}
