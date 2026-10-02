package com.stacknoise.haac.core.network.bridge

import com.stacknoise.haac.core.error.BridgeException
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.http.decodeOrUnexpected
import com.stacknoise.haac.core.network.websocket.HaWebSocket
import com.stacknoise.haac.core.network.websocket.HaWebSocketFactory
import com.stacknoise.haac.core.network.websocket.authenticate
import com.stacknoise.haac.core.network.websocket.awaitReply
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl

/** Answer of `haac_bridge/info` (concept 11.2, 11.4); older bridges send no [instanceId], [urls] and [features]. */
@Serializable
data class BridgeInfo(
    @SerialName("bridge_version") val bridgeVersion: String,
    @SerialName("api_version") val apiVersion: Int,
    val domains: List<String> = emptyList(),
    @SerialName("ha_version") val haVersion: String,
    @SerialName("instance_id") val instanceId: String? = null,
    val urls: BridgeUrls = BridgeUrls(),
    val features: List<String> = emptyList(),
)

/** Addresses configured in HA (concept 4.5); each is null if not set. */
@Serializable
data class BridgeUrls(
    val internal: String? = null,
    val external: String? = null,
    val cloud: String? = null,
)

/**
 * An open, authenticated WebSocket, the bridge info it returned and the [messages] factory whose ids the handshake
 * used: the connection must go on with it, because HA requires message ids on one socket to keep increasing.
 */
class BridgeHandshake(val socket: HaWebSocket, val info: BridgeInfo, val messages: BridgeMessageFactory)

/**
 * Checks that HAAC Bridge is installed and speaks a supported API version (concept 4.2 step 4, 11.4):
 * auth on the WebSocket, then `haac_bridge/info`.
 */
class BridgeInfoClient @Inject constructor(
    private val sockets: HaWebSocketFactory,
    private val json: Json,
    private val messages: BridgeMessageFactory,
    private val errors: ErrorFactory,
) {
    /** The bridge info from a short-lived socket that is closed again, e.g. after login. */
    suspend fun fetch(baseUrl: HttpUrl, accessToken: String): BridgeInfo {
        val handshake = open(baseUrl, accessToken)
        handshake.socket.close()
        return handshake.info
    }

    /**
     * Opens a socket and runs the handshake; the caller owns the open socket. AUTH-003 if the access token
     * is rejected, BRG-001 if the bridge is missing, BRG-002 if its API version is not supported.
     */
    suspend fun open(baseUrl: HttpUrl, accessToken: String): BridgeHandshake {
        val socket = sockets.open(baseUrl)
        var done = false
        try {
            // Real time on the IO dispatcher, also under a test scheduler with virtual time.
            val info = withContext(Dispatchers.IO) {
                withTimeoutOrNull(TIMEOUT_MS) { converse(socket, accessToken) }
            } ?: throw NetworkException(ErrorCode.NET_UNREACHABLE)
            done = true
            return BridgeHandshake(socket, info, messages)
        } finally {
            if (!done) socket.close()
        }
    }

    /** Auth, then the info command. */
    private suspend fun converse(socket: HaWebSocket, accessToken: String): BridgeInfo {
        socket.authenticate(messages.auth(accessToken))
        val command = messages.command(INFO_COMMAND)
        socket.send(command.json)
        val result = socket.awaitReply(command.id).resultOrThrow(errors)
        val info = json.decodeOrUnexpected(BridgeInfo.serializer(), result.toString())
        if (info.apiVersion !in SUPPORTED_API_VERSIONS) throw BridgeException(ErrorCode.BRG_UPDATE_REQUIRED)
        return info
    }

    /** Command name, supported API versions and timeouts. */
    companion object {
        /** API versions of HAAC Bridge this app understands (concept 11.4). */
        val SUPPORTED_API_VERSIONS = 1..1

        private const val INFO_COMMAND = "haac_bridge/info"
        private const val TIMEOUT_MS = 15_000L
    }
}
