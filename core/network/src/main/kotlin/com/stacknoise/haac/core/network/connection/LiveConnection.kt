package com.stacknoise.haac.core.network.connection

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.NetworkException
import kotlinx.coroutines.flow.StateFlow

/** The open connection of the active instance for features that send commands (concept 8.1, 11.2). */
interface LiveConnection {
    /** The open connection, or null while there is none (controls are disabled then, concept 14.1). */
    val connection: StateFlow<BridgeChannel?>
}

/** The open connection; HAAC-NET-002 while there is none (concept 14.1). */
fun LiveConnection.requireOpen(): BridgeChannel =
    connection.value?.takeIf { it.isOpen } ?: throw NetworkException(ErrorCode.NET_CONNECTION_LOST)
