package com.stacknoise.haac.core.network.bridge

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** A WebSocket command with its message id. */
data class BridgeCommand(val id: Int, val json: String)

/** Builds the messages of HA's WebSocket API with increasing ids (concept 11, 17.2). */
interface BridgeMessageFactory {
    /** The `auth` message; contains the access token and must never be logged. */
    fun auth(accessToken: String): String

    /** A command such as `haac_bridge/info` with the next message id. */
    fun command(type: String): BridgeCommand
}

/** Message ids grow per factory; HA only requires them to increase on one connection. */
class DefaultBridgeMessageFactory @Inject constructor() : BridgeMessageFactory {
    private val ids = AtomicInteger()

    /** `{"type": "auth", "access_token": …}`. */
    override fun auth(accessToken: String): String = buildJsonObject {
        put("type", "auth")
        put("access_token", accessToken)
    }.toString()

    /** `{"id": n, "type": …}`. */
    override fun command(type: String): BridgeCommand {
        val id = ids.incrementAndGet()
        val json = buildJsonObject {
            put("id", id)
            put("type", type)
        }
        return BridgeCommand(id, json.toString())
    }
}
