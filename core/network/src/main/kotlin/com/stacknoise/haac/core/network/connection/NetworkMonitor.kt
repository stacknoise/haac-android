package com.stacknoise.haac.core.network.connection

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map

/** Reports changes of the network the device uses (concept 4.5). */
fun interface NetworkMonitor {
    /** Emits whenever the default network changes, e.g. from Wi-Fi to mobile data; not for the current one. */
    fun changes(): Flow<Unit>
}

/** [NetworkMonitor] with `ConnectivityManager.registerDefaultNetworkCallback`. */
class ConnectivityNetworkMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NetworkMonitor {
    /** Skips the current network, which the flow starts with and the first callback repeats. */
    override fun changes(): Flow<Unit> = defaultNetwork().distinctUntilChanged().drop(1).map { }

    /** The default network, or null while there is none; starts with the current one. */
    private fun defaultNetwork(): Flow<Network?> = callbackFlow {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        trySend(manager.activeNetwork)
        val callback = object : ConnectivityManager.NetworkCallback() {
            /** A network became the default. */
            override fun onAvailable(network: Network) {
                trySend(network)
            }

            /** The default network is gone. */
            override fun onLost(network: Network) {
                trySend(null)
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }
}
