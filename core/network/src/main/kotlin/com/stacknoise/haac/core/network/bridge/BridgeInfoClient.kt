package com.stacknoise.haac.core.network.bridge

import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.BridgeException
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.http.decodeOrUnexpected
import com.stacknoise.haac.core.network.http.endpoint
import com.stacknoise.haac.core.network.http.text
import com.stacknoise.haac.core.network.server.CleartextPolicy
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/** Answer of `haac_bridge/info` (concept 11.2, 11.4). */
@Serializable
data class BridgeInfo(
    @SerialName("bridge_version") val bridgeVersion: String,
    @SerialName("api_version") val apiVersion: Int,
    val domains: List<String> = emptyList(),
    @SerialName("ha_version") val haVersion: String,
)

/**
 * Checks after login that HAAC Bridge is installed and speaks a supported API version (concept 4.2 step 4, 11.4).
 *
 * Opens a short-lived WebSocket: auth, `haac_bridge/info`, close. The long-lived connection with
 * heartbeat and reconnect belongs to the sync (chapter 9).
 */
class BridgeInfoClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val messages: BridgeMessageFactory,
    private val errors: ErrorFactory,
) {
    /** Returns the bridge info; BRG-001 if the bridge is missing, BRG-002 if its API version is not supported. */
    suspend fun fetch(baseUrl: HttpUrl, accessToken: String): BridgeInfo {
        CleartextPolicy.requireAllowed(baseUrl)
        val incoming = Channel<String>(Channel.UNLIMITED)
        val request = Request.Builder().url(baseUrl.endpoint("api/websocket")).build()
        val socket = client.newWebSocket(request, Forwarder(incoming))
        try {
            // Real time on the IO dispatcher, also under a test scheduler with virtual time.
            val info = withContext(Dispatchers.IO) {
                withTimeoutOrNull(TIMEOUT_MS) { converse(socket, incoming, accessToken) }
            }
            return info ?: throw NetworkException(ErrorCode.NET_UNREACHABLE)
        } finally {
            socket.close(NORMAL_CLOSURE, null)
        }
    }

    /** Auth handshake of HA's WebSocket API, then the info command. */
    private suspend fun converse(socket: WebSocket, incoming: Channel<String>, accessToken: String): BridgeInfo {
        requireType(receive(incoming), "auth_required")
        socket.send(messages.auth(accessToken))
        checkAuth(receive(incoming))
        val command = messages.command(INFO_COMMAND)
        socket.send(command.json)
        while (true) {
            val message = receive(incoming)
            if (message.text("type") == "result" && (message["id"] as? JsonPrimitive)?.intOrNull == command.id) {
                return result(message)
            }
        }
    }

    /** The first message of HA's WebSocket API; anything else is not a HA server. */
    private fun requireType(message: JsonObject, type: String) {
        if (message.text("type") != type) throw NetworkException(ErrorCode.NET_NOT_HOME_ASSISTANT)
    }

    /** `auth_ok` continues; `auth_invalid` means the access token was rejected (HAAC-AUTH-003). */
    private fun checkAuth(message: JsonObject) {
        when (message.text("type")) {
            "auth_ok" -> Unit
            "auth_invalid" -> throw AuthException(ErrorCode.AUTH_SESSION_EXPIRED)
            else -> throw NetworkException(ErrorCode.NET_NOT_HOME_ASSISTANT)
        }
    }

    /** Reads a result: the info on success, otherwise the matching bridge error. */
    private fun result(message: JsonObject): BridgeInfo {
        if ((message["success"] as? JsonPrimitive)?.contentOrNull != "true") throw bridgeError(message)
        val info = json.decodeOrUnexpected(BridgeInfo.serializer(), message["result"].toString())
        if (info.apiVersion !in SUPPORTED_API_VERSIONS) throw BridgeException(ErrorCode.BRG_UPDATE_REQUIRED)
        return info
    }

    /** `unknown_command` means the bridge is not installed (BRG-001); HAB codes map via [ErrorFactory]. */
    private fun bridgeError(message: JsonObject): HaacException {
        val code = (message["error"] as? JsonObject)?.text("code")
        return when {
            code == UNKNOWN_COMMAND -> BridgeException(ErrorCode.BRG_NOT_INSTALLED, bridgeCode = code)
            code != null && code.startsWith("HAB-") -> errors.fromBridgeError(code)
            else -> BridgeException(ErrorCode.BRG_ACTION_FAILED, bridgeCode = code)
        }
    }

    /** Next message as JSON object; a failed or closed socket becomes a HaacException. */
    private suspend fun receive(incoming: Channel<String>): JsonObject {
        val result = incoming.receiveCatching()
        return result.getOrNull()?.let(::parseMessage)
            ?: throw result.exceptionOrNull()?.let(errors::from) ?: NetworkException(ErrorCode.NET_UNREACHABLE)
    }

    /** One text frame as JSON object; anything else is not HA's WebSocket API. */
    private fun parseMessage(text: String): JsonObject = try {
        json.parseToJsonElement(text).jsonObject
    } catch (e: IllegalArgumentException) {
        throw NetworkException(ErrorCode.NET_NOT_HOME_ASSISTANT, e)
    }

    /** Passes WebSocket text frames into [channel] and closes it on failure or close. */
    private class Forwarder(private val channel: Channel<String>) : WebSocketListener() {
        /** Queues a text frame. */
        override fun onMessage(webSocket: WebSocket, text: String) {
            channel.trySend(text)
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

    /** Command name, supported API versions and timeouts. */
    companion object {
        /** API versions of HAAC Bridge this app understands (concept 11.4). */
        val SUPPORTED_API_VERSIONS = 1..1

        private const val INFO_COMMAND = "haac_bridge/info"
        private const val UNKNOWN_COMMAND = "unknown_command"
        private const val TIMEOUT_MS = 15_000L
        private const val NORMAL_CLOSURE = 1000
    }
}
