package com.stacknoise.haac.core.network.connection

import com.stacknoise.haac.core.error.BridgeException
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.network.bridge.BridgeInfo
import com.stacknoise.haac.core.network.bridge.DefaultBridgeMessageFactory
import com.stacknoise.haac.core.network.http.text
import com.stacknoise.haac.core.network.websocket.FakeHaWebSocket
import com.stacknoise.haac.core.network.websocket.messageId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalCoroutinesApi::class)
class BridgeConnectionTest {
    private var answerPings = true

    /** HA side: `echo` returns its fields, `fail` a HAB error, `subscribe` a result and two events. */
    private val socket = FakeHaWebSocket { message ->
        val id = message.messageId
        when (message.text("type")) {
            "ping" -> if (answerPings) push("""{"id":$id,"type":"pong"}""")
            "echo" -> push("""{"id":$id,"type":"result","success":true,"result":{"value":${message["value"]}}}""")
            "fail" -> push(
                """{"id":$id,"type":"result","success":false,"error":{"code":"HAB-SVC-001","message":"No"}}""",
            )
            "subscribe" -> {
                push("""{"id":$id,"type":"result","success":true,"result":null}""")
                push("""{"id":$id,"type":"event","event":{"a":{"switch.x":{"s":"on"}}}}""")
                push("""{"id":$id,"type":"event","event":{"exposure_changed":{"revision":"r2"}}}""")
            }
        }
    }

    private fun TestScope.connect(ws: FakeHaWebSocket = socket) = BridgeConnection(
        "https://ha.example.com/".toHttpUrl(),
        BridgeInfo("0.1.0", 1, haVersion = "2026.9.0"),
        ws,
        DefaultBridgeMessageFactory(),
        DefaultErrorFactory(),
        backgroundScope,
    )

    @Test
    fun `replies are matched to their requests by id`() = runTest {
        val connection = connect()
        val first = async { connection.request("echo", buildJsonObject { put("value", 1) }) }
        val second = async { connection.request("echo", buildJsonObject { put("value", 2) }) }
        assertEquals(JsonPrimitive(1), (first.await() as JsonObject)["value"])
        assertEquals(JsonPrimitive(2), (second.await() as JsonObject)["value"])
    }

    @Test
    fun `bridge error replies are mapped to HAAC codes`() = runTest {
        val error = assertThrows<BridgeException> { connect().request("fail") }
        assertEquals(ErrorCode.BRG_NOT_ALLOWED, error.code)
        assertEquals("HAB-SVC-001", error.bridgeCode)
    }

    @Test
    fun `subscription emits events and unsubscribes when the collector stops`() = runTest {
        val connection = connect()
        val events = connection.subscribe("subscribe").take(2).toList()
        assertEquals(setOf("a", "exposure_changed"), events.flatMap { it.keys }.toSet())
        runCurrent()
        val unsubscribe = socket.sentOfType("unsubscribe_events").single()
        assertEquals(socket.sentOfType("subscribe").single().messageId, unsubscribe["subscription"]?.let {
            (it as JsonPrimitive).content.toInt()
        })
    }

    @Test
    fun `heartbeat pings every 30 s and a missing pong ends the connection`() = runTest {
        val connection = connect()
        advanceTimeBy(30_001)
        assertEquals(1, socket.sentOfType("ping").size)
        assertFalse(connection.end.isCompleted)
        answerPings = false
        advanceTimeBy(30_000 + 10_001)
        assertEquals(ErrorCode.NET_CONNECTION_LOST, connection.end.await().code)
        assertTrue(socket.closed)
    }

    @Test
    fun `a failed socket fails waiting requests and subscriptions with NET-002`() = runTest {
        val silent = FakeHaWebSocket()
        val connection = connect(silent)
        val request = async { runCatching { connection.request("echo") } }
        runCurrent()
        silent.fail()
        val error = request.await().exceptionOrNull() as HaacException
        assertEquals(ErrorCode.NET_CONNECTION_LOST, error.code)
        assertEquals(ErrorCode.NET_CONNECTION_LOST, connection.end.await().code)
        assertEquals(ErrorCode.NET_CONNECTION_LOST, assertThrows<HaacException> { connection.request("echo") }.code)
    }

    @Test
    fun `close ends the connection once and sends nothing more`() = runTest {
        val connection = connect()
        connection.close()
        connection.close()
        assertTrue(socket.closed)
        assertEquals(ErrorCode.NET_CONNECTION_LOST, connection.end.await().code)
        advanceTimeBy(60_000)
        assertTrue(socket.sentOfType("ping").isEmpty())
    }
}
