package com.stacknoise.haac.core.network.websocket

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.http.text
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** A scripted HA WebSocket: [reply] is called for every frame the app sends and may [push] answers. */
class FakeHaWebSocket(private val reply: FakeHaWebSocket.(JsonObject) -> Unit = {}) : HaWebSocket {
    private val incoming = Channel<JsonObject>(Channel.UNLIMITED)

    /** Every frame the app sent, parsed. */
    val sent = mutableListOf<JsonObject>()

    /** True after the app closed the socket. */
    var closed = false
        private set

    /** Queues a message from HA. */
    fun push(json: String) {
        incoming.trySend(Json.parseToJsonElement(json).jsonObject)
    }

    /** Lets the socket fail as if the network dropped. */
    fun fail(error: HaacException = NetworkException(ErrorCode.NET_UNREACHABLE)) {
        incoming.close(error)
    }

    /** Frames of [type] the app sent. */
    fun sentOfType(type: String): List<JsonObject> = sent.filter { it.text("type") == type }

    private var lastId = 0

    override fun send(text: String): Boolean {
        if (closed) return false
        val message = Json.parseToJsonElement(text).jsonObject
        sent += message
        // Like HA: the ids of one socket must increase, otherwise the command is answered with `id_reuse`.
        val id = message.messageId
        if (id != null && id <= lastId) {
            push(
                """{"id":$id,"type":"result","success":false,""" +
                    """"error":{"code":"id_reuse","message":"Identifier values have to increase."}}""",
            )
        } else {
            if (id != null) lastId = id
            reply(message)
        }
        return true
    }

    override suspend fun receive(): JsonObject {
        val result = incoming.receiveCatching()
        return result.getOrNull()
            ?: throw result.exceptionOrNull() as? HaacException ?: NetworkException(ErrorCode.NET_UNREACHABLE)
    }

    override fun close() {
        closed = true
        incoming.close()
    }

    companion object {
        /** `haac_bridge/info` result fields of a bridge with API 1 and instance [instanceId]. */
        fun infoResult(instanceId: String = "f00d") =
            """{"bridge_version":"0.1.0","api_version":1,"ha_version":"2026.9.0","instance_id":"$instanceId"}"""

        /**
         * A HA that answers auth with `auth_ok` (or `auth_invalid` if [acceptAuth] is false), `haac_bridge/info`
         * with [info] and `ping` with `pong`; [other] handles every other command.
         */
        fun ha(
            info: String = infoResult(),
            acceptAuth: Boolean = true,
            other: FakeHaWebSocket.(JsonObject) -> Unit = {},
        ) = FakeHaWebSocket { message ->
            val id = message.messageId
            when (message.text("type")) {
                "auth" -> push(if (acceptAuth) """{"type":"auth_ok"}""" else """{"type":"auth_invalid"}""")
                "haac_bridge/info" -> push("""{"id":$id,"type":"result","success":true,"result":$info}""")
                "ping" -> push("""{"id":$id,"type":"pong"}""")
                else -> other(message)
            }
        }.also { it.push("""{"type":"auth_required","ha_version":"2026.9.0"}""") }
    }
}
