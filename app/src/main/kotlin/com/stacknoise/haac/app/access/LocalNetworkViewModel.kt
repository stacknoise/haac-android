package com.stacknoise.haac.app.access

import androidx.lifecycle.ViewModel
import com.stacknoise.haac.core.network.access.LocalNetworkAccess
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Gives the composable below the current state of the local network permission (concept 4.2). */
@HiltViewModel
class LocalNetworkViewModel @Inject constructor(private val access: LocalNetworkAccess) : ViewModel() {
    /** True if the permission is granted or not needed on this Android version. */
    fun granted(): Boolean = access.granted()
}
