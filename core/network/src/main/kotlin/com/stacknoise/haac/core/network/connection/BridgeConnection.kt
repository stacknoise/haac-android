package com.stacknoise.haac.core.network.connection

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.bridge.BridgeCommand
import com.stacknoise.haac.core.network.bridge.BridgeInfo
import com.stacknoise.haac.core.network.bridge.BridgeMessageFactory
import com.stacknoise.haac.core.network.bridge.resultOrThrow
import com.stacknoise.haac.core.network.http.text
import com.stacknoise.haac.core.network.websocket.HaWebSocket
import com.stacknoise.haac.core.network.websocket.messageId
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl

/**
 * The live WebSocket of the active instance after auth and `haac_bridge/info` (concept 9.1, 11.4): replies
 * are matched to requests by message id, subscription events are passed on, and HA's `ping` runs every 30 s.
 * A missing `pong` or a failed socket ends the connection with HAAC-NET-002; [ConnectionSupervisor] then
 * reconnects.
 */
class BridgeConnection(
    /** The address this connection uses (concept 4.5). */
    val url: HttpUrl,
    /** The bridge info of the handshake. */
    val info: BridgeInfo,
    private val socket: HaWebSocket,
    private val messages: BridgeMessageFactory,
    private val errors: ErrorFactory,
    parent: CoroutineScope,
) : BridgeChannel {
    private val scope = CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
    private val replies = ConcurrentHashMap<Int, CompletableDeferred<JsonObject>>()
    private val subscriptions = ConcurrentHashMap<Int, Channel<JsonObject>>()
    private val ended = CompletableDeferred<HaacException>()

    /** Completes with the reason when the connection ends. */
    val end: Deferred<HaacException> get() = ended

    /** True until the connection ends. */
    override val isOpen: Boolean get() = !ended.isCompleted

    init {
        scope.launch { read() }
        scope.launch { heartbeat() }
    }

    /** Sends the command and waits for the reply with its message id. */
    override suspend fun request(type: String, fields: JsonObject): JsonElement =
        exchange(messages.command(type, fields)).resultOrThrow(errors)

    /** Events arrive in a channel registered before the command is sent; stopping sends `unsubscribe_events`. */
    override fun subscribe(type: String, fields: JsonObject): Flow<JsonObject> = flow {
        val command = messages.command(type, fields)
        val events = Channel<JsonObject>(Channel.UNLIMITED)
        subscriptions[command.id] = events
        try {
            exchange(command).resultOrThrow(errors)
            for (event in events) emit(event)
        } finally {
            subscriptions.remove(command.id)
            if (!ended.isCompleted) socket.send(unsubscribe(command.id))
        }
    }

    /** Closes the connection, e.g. on an address change or when the app goes to the background. */
    fun close() = end(NetworkException(ErrorCode.NET_CONNECTION_LOST))

    /** Sends [command] and waits for the message with its id (result or pong). */
    private suspend fun exchange(command: BridgeCommand): JsonObject {
        val reply = CompletableDeferred<JsonObject>()
        replies[command.id] = reply
        try {
            if (ended.isCompleted || !socket.send(command.json)) throw ended.getOrLost()
            return reply.await()
        } finally {
            replies.remove(command.id)
        }
    }

    /** Hands every incoming message to [dispatch] until the socket fails or closes. */
    private suspend fun read() {
        try {
            while (true) dispatch(socket.receive())
        } catch (e: HaacException) {
            end(NetworkException(ErrorCode.NET_CONNECTION_LOST, e))
        }
    }

    /** Events go to their subscription, every other message with an id completes the waiting request. */
    private fun dispatch(message: JsonObject) {
        val id = message.messageId ?: return
        if (message.text("type") == "event") {
            (message["event"] as? JsonObject)?.let { subscriptions[id]?.trySend(it) }
        } else {
            replies[id]?.complete(message)
        }
    }

    /** HA's `ping` every 30 s; no `pong` within 10 s means the connection is dead (concept 11.4). */
    private suspend fun heartbeat() {
        while (true) {
            delay(HEARTBEAT_MS)
            val pong = try {
                withTimeoutOrNull(PONG_TIMEOUT_MS) { exchange(messages.command(PING)) }
            } catch (_: HaacException) {
                return // The connection already ended; [ended] holds the reason.
            }
            if (pong == null) end(NetworkException(ErrorCode.NET_CONNECTION_LOST))
        }
    }

    /** Ends the connection once: closes the socket and fails waiting requests and subscriptions with [reason]. */
    private fun end(reason: HaacException) {
        if (!ended.complete(reason)) return
        socket.close()
        replies.values.forEach { it.completeExceptionally(reason) }
        subscriptions.values.forEach { it.close(reason) }
        scope.cancel()
    }

    /** The reason the connection ended, or NET-002 if it is ending right now. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun CompletableDeferred<HaacException>.getOrLost(): HaacException =
        if (isCompleted) getCompleted() else NetworkException(ErrorCode.NET_CONNECTION_LOST)

    /** `unsubscribe_events` for the subscription with message id [subscription]. */
    private fun unsubscribe(subscription: Int): String =
        messages.command(UNSUBSCRIBE, buildJsonObject { put("subscription", subscription) }).json

    /** Heartbeat timing and command names. */
    private companion object {
        const val HEARTBEAT_MS = 30_000L
        const val PONG_TIMEOUT_MS = 10_000L
        const val PING = "ping"
        const val UNSUBSCRIBE = "unsubscribe_events"
    }
}
