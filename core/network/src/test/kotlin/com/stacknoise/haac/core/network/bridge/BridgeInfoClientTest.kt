package com.stacknoise.haac.core.network.bridge

import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.BridgeException
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BridgeInfoClientTest {
    private val server = MockWebServer()
    private val received = mutableListOf<String>()
    private val client = BridgeInfoClient(
        OkHttpClient(),
        Json { ignoreUnknownKeys = true },
        DefaultBridgeMessageFactory(),
        DefaultErrorFactory(),
    )

    @BeforeEach
    fun start() = server.start()

    @AfterEach
    fun stop() = server.close()

    /** A fake HA WebSocket: auth_required, then [authReply], then [commandReply] for the command id. */
    private fun fakeHa(authReply: String, commandReply: (Int) -> String) {
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("""{"type":"auth_required","ha_version":"2026.9.0"}""")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                synchronized(received) { received += text }
                val id = Regex(""""id":(\d+)""").find(text)?.groupValues?.get(1)?.toInt()
                webSocket.send(if (id == null) authReply else commandReply(id))
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, null)
            }
        }
        server.enqueue(MockResponse.Builder().webSocketUpgrade(listener).build())
    }

    private val authOk = """{"type":"auth_ok","ha_version":"2026.9.0"}"""

    @Test
    fun `reads the bridge info after auth`() = runTest {
        fakeHa(authOk) { id ->
            """{"id":$id,"type":"result","success":true,"result":{"bridge_version":"0.1.0","api_version":1,""" +
                """"domains":["climate","sensor","switch"],"ha_version":"2026.9.0"}}"""
        }
        val info = client.fetch(server.url("/"), "acc")
        assertEquals(BridgeInfo("0.1.0", 1, listOf("climate", "sensor", "switch"), "2026.9.0"), info)
        assertEquals("""{"type":"auth","access_token":"acc"}""", received.first())
        assertEquals("/api/websocket", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `missing bridge is BRG-001`() = runTest {
        fakeHa(authOk) { id ->
            """{"id":$id,"type":"result","success":false,"error":{"code":"unknown_command","message":"Unknown"}}"""
        }
        assertEquals(ErrorCode.BRG_NOT_INSTALLED, assertThrows<BridgeException> { fetch() }.code)
    }

    @Test
    fun `unsupported api version is BRG-002`() = runTest {
        fakeHa(authOk) { id ->
            """{"id":$id,"type":"result","success":true,"result":{"bridge_version":"9.0.0","api_version":9,""" +
                """"ha_version":"2026.9.0"}}"""
        }
        assertEquals(ErrorCode.BRG_UPDATE_REQUIRED, assertThrows<BridgeException> { fetch() }.code)
    }

    @Test
    fun `rejected token is AUTH-003 and HAB codes are mapped`() = runTest {
        fakeHa("""{"type":"auth_invalid","message":"Invalid access token"}""") { "" }
        assertEquals(ErrorCode.AUTH_SESSION_EXPIRED, assertThrows<AuthException> { fetch() }.code)
        fakeHa(authOk) { id ->
            """{"id":$id,"type":"result","success":false,"error":{"code":"HAB-AUTH-001","message":"No user"}}"""
        }
        val error = assertThrows<HaacException> { fetch() }
        assertEquals(ErrorCode.AUTH_SESSION_EXPIRED, error.code)
        assertEquals("HAB-AUTH-001", error.bridgeCode)
    }

    private suspend fun fetch() = client.fetch(server.url("/"), "acc")
}
