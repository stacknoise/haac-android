package com.stacknoise.haac.core.network.connection

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.NetworkException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest

/** The open connection of the active instance for features that send commands (concept 8.1, 11.2). */
interface LiveConnection {
    /** The open connection, or null while there is none (controls are disabled then, concept 14.1). */
    val connection: StateFlow<BridgeChannel?>
}

/** The open connection; HAAC-NET-002 while there is none (concept 14.1). */
fun LiveConnection.requireOpen(): BridgeChannel =
    connection.value?.takeIf { it.isOpen } ?: throw NetworkException(ErrorCode.NET_CONNECTION_LOST)

/** How long the connection may be down before the cached data is marked "stale" (concept 14.1). */
const val StaleDelayMs = 3_000L

/** True while a connection is open; controls and edits are disabled otherwise (concept 14.1). */
val LiveConnection.connected: Flow<Boolean> get() = connection.map { it != null }.distinctUntilChanged()

/**
 * True once the connection has been down for [StaleDelayMs]: the cached data is then marked "stale" (concept
 * 14.1). The delay keeps the marking away while the app is just connecting after a start.
 */
@OptIn(ExperimentalCoroutinesApi::class)
val LiveConnection.stale: Flow<Boolean>
    get() = connected.transformLatest { open ->
        if (open) {
            emit(false)
        } else {
            delay(StaleDelayMs)
            emit(true)
        }
    }.distinctUntilChanged()
