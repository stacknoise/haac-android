package com.stacknoise.haac.core.network.demo

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.websocket.HaWebSocket
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * [HaWebSocket] over a [DemoBridge] (concept 20.2): [send] hands a command to the bridge, [receive] returns its
 * replies and events, including the answer to `ping`. There is no network and no handshake. Commands run one after
 * the other on [dispatcher], off the caller's thread, because the bridge writes the demo state to a file.
 */
class DemoSocket(
    world: DemoWorld,
    dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
) : HaWebSocket {
    private val incoming = Channel<JsonObject>(Channel.UNLIMITED)
    private val commands = Channel<JsonObject>(Channel.UNLIMITED)
    private val bridge = DemoBridge(world) { incoming.trySend(it) }
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    init {
        scope.launch { for (command in commands) bridge.handle(command) }
    }

    /** Queues [text] for the bridge; false once the socket is closed or if [text] is not a JSON object. */
    override fun send(text: String): Boolean = parse(text)?.let { commands.trySend(it).isSuccess } ?: false

    /** [text] as a JSON object, or null if it is not one. */
    private fun parse(text: String): JsonObject? = try {
        Json.parseToJsonElement(text).jsonObject
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    /** The next reply or event; HAAC-NET-001 after the socket is closed. */
    override suspend fun receive(): JsonObject {
        val result = incoming.receiveCatching()
        return result.getOrNull()
            ?: throw result.exceptionOrNull() as? HaacException ?: NetworkException(ErrorCode.NET_UNREACHABLE)
    }

    /** Stops the bridge and ends the conversation. */
    override fun close() {
        bridge.close()
        commands.close()
        incoming.close()
        scope.cancel()
    }
}
