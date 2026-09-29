package com.stacknoise.haac.core.network.connection

import com.stacknoise.haac.core.network.bridge.BridgeMessageFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Commands and subscriptions on the live connection (concept 11.2); features use this, not the socket. */
interface BridgeChannel {
    /** False once the connection has ended. */
    val isOpen: Boolean

    /** Sends command [type] with [fields] and returns its `result`; a bridge error reply is thrown (18.3). */
    suspend fun request(type: String, fields: JsonObject = BridgeMessageFactory.NO_FIELDS): JsonElement

    /**
     * Subscribes with command [type] and emits the `event` objects until the collector stops, which ends the
     * subscription (concept 11.3). The flow fails with the reason when the connection ends.
     */
    fun subscribe(type: String, fields: JsonObject = BridgeMessageFactory.NO_FIELDS): Flow<JsonObject>
}
