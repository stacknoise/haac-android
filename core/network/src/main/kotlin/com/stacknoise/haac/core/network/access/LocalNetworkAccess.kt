package com.stacknoise.haac.core.network.access

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Whether the app may reach devices in the local network (concept 4.2, 4.5). */
fun interface LocalNetworkAccess {
    /** True if discovery and connections to local addresses are allowed on this device. */
    fun granted(): Boolean

    /** The runtime permission and the first Android version that enforces it. */
    companion object {
        const val PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
        const val FIRST_API = 37
    }
}

/** Reads the `ACCESS_LOCAL_NETWORK` runtime permission; Android versions before API 37 never block local traffic. */
class AndroidLocalNetworkAccess @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocalNetworkAccess {
    /** Always true before API 37, otherwise the state of the permission. */
    override fun granted(): Boolean = Build.VERSION.SDK_INT < LocalNetworkAccess.FIRST_API ||
        ContextCompat.checkSelfPermission(context, LocalNetworkAccess.PERMISSION) == PackageManager.PERMISSION_GRANTED
}
