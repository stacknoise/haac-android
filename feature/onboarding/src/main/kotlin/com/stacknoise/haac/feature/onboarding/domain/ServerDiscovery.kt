package com.stacknoise.haac.feature.onboarding.domain

import kotlinx.coroutines.flow.Flow

/** A HA server announced on the local network via zeroconf (concept 4.2, M-01). */
data class DiscoveredServer(
    val id: String,
    val name: String,
    val address: String,
    val url: String,
    val version: String?,
)

/** Finds HA servers on the local network while collected. */
interface ServerDiscovery {
    /** Emits the current list whenever a server appears or disappears; completes if discovery fails. */
    fun servers(): Flow<List<DiscoveredServer>>
}
