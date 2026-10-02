package com.stacknoise.haac.core.network.connection

import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.network.bridge.BridgeInfo
import com.stacknoise.haac.core.network.bridge.BridgeInfoClient
import com.stacknoise.haac.core.network.bridge.DefaultBridgeMessageFactory
import com.stacknoise.haac.core.network.demo.DemoInstance
import com.stacknoise.haac.core.network.demo.DemoWorld
import com.stacknoise.haac.core.network.demo.MemoryDemoWorldStore
import com.stacknoise.haac.core.network.endpoint.EndpointSelector
import com.stacknoise.haac.core.network.session.InstanceSessionFactory
import com.stacknoise.haac.core.network.websocket.FakeHaWebSocket
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DemoBridgeConnectorTest {
    private val realUrl = "https://ha.example.com/".toHttpUrl()
    private val messages = DefaultBridgeMessageFactory()
    private val errors = DefaultErrorFactory()
    private val world = DemoWorld(MemoryDemoWorldStore(), Json { ignoreUnknownKeys = true })

    /** Records what reaches the real connector and answers with a connection over a silent socket. */
    private inner class RecordingConnector : BridgeConnector {
        val selected = mutableListOf<String>()
        val connected = mutableListOf<String>()

        override suspend fun select(serverId: String): HttpUrl = realUrl.also { selected += serverId }

        override suspend fun connect(serverId: String, url: HttpUrl, scope: CoroutineScope): BridgeConnection {
            connected += serverId
            val info = BridgeInfo("0.1.0", 1, haVersion = "2026.9.0")
            return BridgeConnection(url, info, FakeHaWebSocket(), messages, errors, scope)
        }
    }

    private companion object {
        /** The built-in bridge answers on another thread, so this test runs in real time, not virtual time. */
        const val REAL_TIME_LIMIT_MS = 5_000L
    }

    private val real = RecordingConnector()
    private val connector = DemoBridgeConnector(real, world, messages, errors)

    @Test
    fun `the demo ID selects the demo address and connects over the built-in bridge`() = runBlocking {
        assertEquals("https://demo.haac.invalid/".toHttpUrl(), connector.select(DemoInstance.SERVER_ID))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val connection = connector.connect(DemoInstance.SERVER_ID, connector.select(DemoInstance.SERVER_ID), scope)
        assertEquals(DemoInstance.ADDRESS.toHttpUrl(), connection.url)
        assertEquals(DemoInstance.INSTANCE_ID, connection.info.instanceId)
        assertEquals(setOf("schedules"), connection.features)
        val info = withTimeout(REAL_TIME_LIMIT_MS) { connection.request("haac_bridge/info") }.jsonObject
        assertEquals("1", info["api_version"]!!.jsonPrimitive.content)
        assertTrue(real.selected.isEmpty() && real.connected.isEmpty())
        connection.close()
        scope.cancel()
    }

    @Test
    fun `every other ID goes to the real connector`() = runTest {
        assertEquals(realUrl, connector.select("a-random-uuid"))
        connector.connect("a-random-uuid", realUrl, backgroundScope).close()
        assertEquals(listOf("a-random-uuid"), real.selected)
        assertEquals(listOf("a-random-uuid"), real.connected)
    }

    @Test
    fun `the demo ID never touches the address selection, the session or the handshake client`() = runTest {
        val servers = mockk<ServerDao>()
        val endpoints = mockk<EndpointSelector>()
        val sessions = mockk<InstanceSessionFactory>()
        val bridge = mockk<BridgeInfoClient>()
        val default = DefaultBridgeConnector(servers, endpoints, sessions, bridge, errors)
        val productive = DemoBridgeConnector(default, world, messages, errors)
        val url = productive.select(DemoInstance.SERVER_ID)
        productive.connect(DemoInstance.SERVER_ID, url, backgroundScope).close()
        confirmVerified(servers, endpoints, sessions, bridge)
    }
}
