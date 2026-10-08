package com.stacknoise.haac.core.network.websocket

import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.http.endpoint
import com.stacknoise.haac.core.network.http.text
import com.stacknoise.haac.core.network.server.CleartextPolicy
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/** One WebSocket to HA's `/api/websocket` (concept 11); incoming text frames arrive as JSON objects. */
interface HaWebSocket {
    /** Sends [text]; false if the socket is already closing or closed. */
    fun send(text: String): Boolean

    /** The next message; HAAC-NET-001 if the socket failed or closed, NET-004 if it is not JSON. */
    suspend fun receive(): JsonObject

    /** Closes the socket with the normal closure code. */
    fun close()
}

/** Opens WebSockets to HA (concept 17.2). */
fun interface HaWebSocketFactory {
    /** A socket to `<baseUrl>/api/websocket`; the cleartext rule (concept 4.3) is checked first. */
    fun open(baseUrl: HttpUrl): HaWebSocket
}

/** [HaWebSocketFactory] with OkHttp; OkHttp turns off the read timeout of upgraded sockets. */
class OkHttpWebSocketFactory @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val errors: ErrorFactory,
) : HaWebSocketFactory {
    /** Connects in the background; failures arrive through [HaWebSocket.receive]. */
    override fun open(baseUrl: HttpUrl): HaWebSocket {
        CleartextPolicy.requireAllowed(baseUrl)
        val incoming = Channel<JsonObject>(MaxQueuedMessages)
        val request = Request.Builder().url(baseUrl.endpoint("api/websocket")).build()
        return OkHttpWebSocket(client.newWebSocket(request, Forwarder(incoming, json)), incoming, errors)
    }
}

/** Messages that may wait for the reader before the connection is ended. */
private const val MaxQueuedMessages = 4096

/** [HaWebSocket] over an OkHttp [WebSocket] whose messages [Forwarder] puts into [incoming]. */
private class OkHttpWebSocket(
    private val socket: WebSocket,
    private val incoming: Channel<JsonObject>,
    private val errors: ErrorFactory,
) : HaWebSocket {
    /** Queues the frame in OkHttp. */
    override fun send(text: String): Boolean = socket.send(text)

    /** Waits for the next message; the cause of a failed socket is mapped by [ErrorFactory]. */
    override suspend fun receive(): JsonObject {
        val result = incoming.receiveCatching()
        return result.getOrNull()
            ?: throw result.exceptionOrNull()?.let(errors::from) ?: NetworkException(ErrorCode.NET_UNREACHABLE)
    }

    /** Normal closure. */
    override fun close() {
        socket.close(NORMAL_CLOSURE, null)
    }

    /** WebSocket close code. */
    private companion object {
        const val NORMAL_CLOSURE = 1000
    }
}

/**
 * Parses WebSocket text frames on OkHttp's reader thread, so large replies never block the caller's thread,
 * and passes them into [channel]; closes it on failure or close.
 */
private class Forwarder(private val channel: Channel<JsonObject>, private val json: Json) : WebSocketListener() {
    /** Queues a message; a frame that is not a JSON object is not HA's WebSocket API (NET-004). */
    override fun onMessage(webSocket: WebSocket, text: String) {
        try {
            if (channel.trySend(json.parseToJsonElement(text).jsonObject).isFailure) {
                // The reader does not keep up: end the connection instead of filling the memory (review S-06).
                channel.close(NetworkException(ErrorCode.NET_CONNECTION_LOST))
                webSocket.cancel()
            }
        } catch (e: IllegalArgumentException) {
            channel.close(NetworkException(ErrorCode.NET_NOT_HOME_ASSISTANT, e))
            webSocket.cancel()
        }
    }

    /** Ends the conversation with the cause, e.g. an IOException or SSL error. */
    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        channel.close(t)
    }

    /** The server closed the connection. */
    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        channel.close()
    }
}

/**
 * Auth handshake of HA's WebSocket API: `auth_required`, the `auth` message [authMessage], `auth_ok`.
 * `auth_invalid` means the access token was rejected (HAAC-AUTH-003); anything else is not HA (NET-004).
 */
suspend fun HaWebSocket.authenticate(authMessage: String) {
    requireHomeAssistant(receive().text("type") == "auth_required")
    send(authMessage)
    val answer = receive().text("type")
    if (answer == "auth_invalid") throw AuthException(ErrorCode.AUTH_SESSION_EXPIRED)
    requireHomeAssistant(answer == "auth_ok")
}

/** Throws HAAC-NET-004 unless [expected] holds: the other side does not speak HA's WebSocket API. */
private fun requireHomeAssistant(expected: Boolean) {
    if (!expected) throw NetworkException(ErrorCode.NET_NOT_HOME_ASSISTANT)
}

/** Reads messages until the reply with message id [id] arrives; used before the reader loop runs. */
suspend fun HaWebSocket.awaitReply(id: Int): JsonObject {
    while (true) {
        val message = receive()
        if (message.messageId == id) return message
    }
}

/** The `id` of a message, or null for messages without one (auth). */
val JsonObject.messageId: Int? get() = (this["id"] as? JsonPrimitive)?.intOrNull
