package com.stacknoise.haac.core.network.endpoint

import com.stacknoise.haac.core.network.discovery.HaServiceParser
import com.stacknoise.haac.core.network.discovery.ServerDiscovery
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

/** Decides whether the device is in the home network of an instance (concept 4.5). */
fun interface HomeNetworkCheck {
    /** True if HA installation [instanceUuid] announces itself on this network at [host]. */
    suspend fun confirms(instanceUuid: String, host: String): Boolean
}

/**
 * Home network check via mDNS (concept 4.5): browses `_home-assistant._tcp` for up to 2 s and looks for
 * a service whose TXT `uuid` is [instanceUuid] and whose IP or TXT URL host is the internal host.
 * Needs no location permission; fails in networks without mDNS.
 */
class NsdHomeNetworkCheck @Inject constructor(private val discovery: ServerDiscovery) : HomeNetworkCheck {
    /** Stops browsing as soon as the instance is found, after the timeout or when discovery fails. */
    override suspend fun confirms(instanceUuid: String, host: String): Boolean {
        val wanted = HaServiceParser.normalizeHost(host)
        val match = withTimeoutOrNull(TIMEOUT_MS) {
            discovery.servers().firstOrNull { servers ->
                servers.any { it.id == instanceUuid && wanted in it.hosts }
            }
        }
        return match != null
    }

    /** Browse time. */
    private companion object {
        const val TIMEOUT_MS = 2_000L
    }
}
