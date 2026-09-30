package com.stacknoise.haac.core.network.discovery

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import com.stacknoise.haac.core.network.access.LocalNetworkAccess
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * LAN discovery with Android's NsdManager (concept 4.2, M-01).
 *
 * From targetSdk 37 the runtime permission ACCESS_LOCAL_NETWORK is required; the app asks for it (concept 4.2).
 */
class NsdServerDiscovery @Inject constructor(
    private val nsd: NsdManager,
    private val localNetwork: LocalNetworkAccess,
) : ServerDiscovery {
    /** Discovers while collected; without local network access the flow ends at once (concept 4.2). */
    override fun servers(): Flow<List<DiscoveredServer>> = callbackFlow {
        if (!localNetwork.granted()) {
            close()
            return@callbackFlow awaitClose()
        }
        val found = LinkedHashMap<String, DiscoveredServer>()
        /** Sends a snapshot of all resolved servers. */
        fun publish() = synchronized(found) { trySend(found.values.toList()) }

        val listener = object : NsdManager.DiscoveryListener {
            /** Discovery is running; nothing to do. */
            override fun onDiscoveryStarted(serviceType: String) = Unit

            /** Discovery stopped because the flow was closed. */
            override fun onDiscoveryStopped(serviceType: String) = Unit

            /** Ends the flow so the screen stops showing the scan indicator. */
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                channel.close()
            }

            /** Nothing to clean up if stopping fails. */
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit

            /** Resolves the service to get its address and TXT record, then publishes it. */
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                resolve(serviceInfo) { server ->
                    synchronized(found) { found[serviceInfo.serviceName] = server }
                    publish()
                }
            }

            /** Removes a server that left the network. */
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                synchronized(found) { found.remove(serviceInfo.serviceName) }
                publish()
            }
        }
        nsd.discoverServices(HaServiceParser.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        awaitClose { runCatching { nsd.stopServiceDiscovery(listener) } }
    }

    /** Resolves one service with the API of the running Android version. */
    private fun resolve(info: NsdServiceInfo, onResolved: (DiscoveredServer) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            resolveWithCallback(info, onResolved)
        } else {
            resolveLegacy(info, onResolved)
        }
    }

    /** Android 14+: one-shot service info callback. */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun resolveWithCallback(info: NsdServiceInfo, onResolved: (DiscoveredServer) -> Unit) {
        val callback = object : NsdManager.ServiceInfoCallback {
            /** Resolution failed; the server is simply not listed. */
            override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) = Unit

            /** Publishes the first complete update and stops listening. */
            override fun onServiceUpdated(serviceInfo: NsdServiceInfo) {
                val ip = serviceInfo.hostAddresses.firstOrNull()?.hostAddress ?: return
                onResolved(serviceInfo.toServer(ip))
                runCatching { nsd.unregisterServiceInfoCallback(this) }
            }

            /** The service went away before it was resolved. */
            override fun onServiceLost() = Unit

            /** Callback removed; nothing to do. */
            override fun onServiceInfoCallbackUnregistered() = Unit
        }
        nsd.registerServiceInfoCallback(info, Executor(Runnable::run), callback)
    }

    /** Parses a resolved service reached at [ip]. */
    private fun NsdServiceInfo.toServer(ip: String): DiscoveredServer =
        HaServiceParser.parse(serviceName, ip, port, attributes)

    /** Android 9–13: resolveService, deprecated from API 34 on. */
    @Suppress("DEPRECATION")
    private fun resolveLegacy(info: NsdServiceInfo, onResolved: (DiscoveredServer) -> Unit) {
        nsd.resolveService(
            info,
            object : NsdManager.ResolveListener {
                /** Resolution failed (e.g. another resolve is running); the server is not listed. */
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit

                /** Publishes the resolved server. */
                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val ip = serviceInfo.host?.hostAddress ?: return
                    onResolved(serviceInfo.toServer(ip))
                }
            },
        )
    }
}
