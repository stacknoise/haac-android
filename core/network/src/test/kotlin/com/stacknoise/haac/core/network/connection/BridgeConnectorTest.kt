package com.stacknoise.haac.core.network.connection

import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.server.ServerEntity
import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.bridge.BridgeInfoClient
import com.stacknoise.haac.core.network.bridge.DefaultBridgeMessageFactory
import com.stacknoise.haac.core.network.session.InstanceSession
import com.stacknoise.haac.core.network.session.InstanceSessionFactory
import com.stacknoise.haac.core.network.websocket.FakeHaWebSocket
import com.stacknoise.haac.core.network.websocket.messageId
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BridgeConnectorTest {
    private val url = "https://ha.example.com/".toHttpUrl()
    private var stored = ServerEntity(
        id = "s1",
        externalUrl = url.toString(),
        displayName = "Home",
        accentColor = 0,
        haUserName = "anna",
        haVersion = "2026.8.0",
        bridgeApiVersion = 1,
        lastActiveAt = 0,
    )
    private val sockets = ArrayDeque<FakeHaWebSocket>()
    private val opened = mutableListOf<FakeHaWebSocket>()

    private val servers = mockk<ServerDao>().also { dao ->
        coEvery { dao.get("s1") } answers { stored }
        coEvery { dao.update(any()) } answers { stored = firstArg() }
    }
    private val session = mockk<InstanceSession> {
        every { baseUrl } returns url
        coEvery { accessToken() } returnsMany listOf("acc1", "acc2")
        coEvery { invalidate() } just Runs
    }
    private val json = Json { ignoreUnknownKeys = true }
    private val messages = DefaultBridgeMessageFactory()
    private val errors = DefaultErrorFactory()
    private val bridge = BridgeInfoClient({ sockets.removeFirst().also { opened += it } }, json, messages, errors)
    private val connector = DefaultBridgeConnector(
        servers = servers,
        endpoints = { url },
        sessions = object : InstanceSessionFactory {
            override fun create(serverId: String, baseUrl: HttpUrl) = session
        },
        bridge = bridge,
        errors = errors,
    )

    @Test
    fun `connects and saves instance ID and versions of a schema-1 instance`() = runTest {
        sockets += FakeHaWebSocket.ha()
        assertEquals(url, connector.select("s1"))
        val connection = connector.connect("s1", url, backgroundScope)
        assertEquals(url, connection.url)
        assertEquals("f00d", stored.instanceUuid)
        assertEquals("2026.9.0", stored.haVersion)
        assertFalse(opened.single().closed)
    }

    @Test
    fun `a rejected access token is refreshed once`() = runTest {
        sockets += FakeHaWebSocket.ha(acceptAuth = false)
        sockets += FakeHaWebSocket.ha()
        connector.connect("s1", url, backgroundScope)
        coVerify(exactly = 1) { session.invalidate() }
        assertTrue(opened.first().closed)
        assertEquals(""""acc2"""", opened.last().sentOfType("auth").single()["access_token"].toString())
    }

    @Test
    fun `rejected twice is AUTH-003`() = runTest {
        sockets += FakeHaWebSocket.ha(acceptAuth = false)
        sockets += FakeHaWebSocket.ha(acceptAuth = false)
        val error = assertThrows<AuthException> { connector.connect("s1", url, backgroundScope) }
        assertEquals(ErrorCode.AUTH_SESSION_EXPIRED, error.code)
    }

    @Test
    fun `another instance ID closes the socket with NET-008 and keeps the stored ID`() = runTest {
        stored = stored.copy(instanceUuid = "f00d")
        sockets += FakeHaWebSocket.ha(info = FakeHaWebSocket.infoResult("beef"))
        val error = assertThrows<NetworkException> { connector.connect("s1", url, backgroundScope) }
        assertEquals(ErrorCode.NET_WRONG_SERVER, error.code)
        assertTrue(opened.single().closed)
        assertEquals("f00d", stored.instanceUuid)
        assertTrue(opened.single().sentOfType("ping").isEmpty())
    }

    @Test
    fun `the connection goes on with the message ids of the handshake`() = runTest {
        sockets += FakeHaWebSocket.ha(other = { message ->
            push("""{"id":${message.messageId},"type":"result","success":true,"result":[]}""")
        })
        val connection = connector.connect("s1", url, backgroundScope)
        // HA answers `id_reuse` if the first command reuses the id of `haac_bridge/info`.
        connection.request("haac_bridge/entities/list")
        val ids = opened.single().sent.mapNotNull { it.messageId }
        assertEquals(ids.sorted().distinct(), ids)
    }
}
