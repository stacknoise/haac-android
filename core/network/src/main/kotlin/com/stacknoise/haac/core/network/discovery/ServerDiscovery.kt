package com.stacknoise.haac.core.network.discovery

import kotlinx.coroutines.flow.Flow

/**
 * A HA server announced on the local network via zeroconf (concept 4.2, M-01). [id] is the TXT `uuid`
 * (HA's `instance_id`); [hosts] holds the service's IP address and the hosts of its TXT URLs (concept 4.5).
 */
data class DiscoveredServer(
    val id: String,
    val name: String,
    val address: String,
    val url: String,
    val version: String?,
    val hosts: Set<String> = emptySet(),
)

/** Finds HA servers on the local network while collected. */
interface ServerDiscovery {
    /** Emits the current list whenever a server appears or disappears; completes if discovery fails. */
    fun servers(): Flow<List<DiscoveredServer>>
}
