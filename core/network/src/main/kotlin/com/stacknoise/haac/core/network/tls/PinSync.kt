package com.stacknoise.haac.core.network.tls

import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.server.ServerEntity
import com.stacknoise.haac.core.network.di.ConnectionScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Keeps the [PinRegistry] equal to the pins of the `server` table while the app runs (concept 4.3). */
@Singleton
class PinSync @Inject constructor(
    private val servers: ServerDao,
    private val pins: PinRegistry,
    @param:ConnectionScope private val scope: CoroutineScope,
) {
    private var started = false

    /** Starts following the table; a second call does nothing. */
    fun start() {
        if (started) return
        started = true
        servers.observeAll().onEach { rows -> pins.replaceStored(rows.pinnedAddresses()) }.launchIn(scope)
    }
}

/** The pins of these instances by registry key; addresses without a pin are left out. */
private fun List<ServerEntity>.pinnedAddresses(): Map<String, String> = flatMap { row ->
    listOf(row.internalUrl to row.internalPinnedKeyHash, row.externalUrl to row.externalPinnedKeyHash)
}.mapNotNull { (address, hash) ->
    val url = address?.toHttpUrlOrNull()
    if (url == null || hash == null) null else PinRegistry.key(url) to hash
}.toMap()
