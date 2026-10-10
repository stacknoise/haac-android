package com.stacknoise.haac.feature.settings.data

import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.server.ServerEntity
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.bridge.BridgeInfo
import com.stacknoise.haac.core.network.bridge.BridgeInfoClient
import com.stacknoise.haac.core.network.endpoint.AddressSlot
import com.stacknoise.haac.core.network.endpoint.EndpointProbe
import com.stacknoise.haac.core.network.endpoint.EndpointSelector
import com.stacknoise.haac.core.network.endpoint.HomeNetworkCheck
import com.stacknoise.haac.core.network.session.InstanceSession
import com.stacknoise.haac.core.network.session.InstanceSessionFactory
import com.stacknoise.haac.core.network.tls.PinRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InstanceAddressRepositoryTest {
    private val stored = "https://ha.example.org".toHttpUrl()
    private val server = ServerEntity(
        id = "s1",
        instanceUuid = "uuid-1",
        externalUrl = stored.toString(),
        displayName = "Home",
        accentColor = 0L,
        haUserName = "anna",
        haVersion = "2026.1",
        bridgeApiVersion = 1,
        lastActiveAt = 0L,
    )
    private val servers = mockk<ServerDao>()
    private val endpoints = mockk<EndpointSelector>()
    private val sessions = mockk<InstanceSessionFactory>()
    private val bridge = mockk<BridgeInfoClient>()
    private val probe = mockk<EndpointProbe>(relaxed = true)
    private val homeNetwork = mockk<HomeNetworkCheck>()
    private val session = mockk<InstanceSession>()
    private val repository = InstanceAddressRepository(
        servers, endpoints, sessions, bridge, probe, DefaultErrorFactory(), PinRegistry(), homeNetwork,
    )

    init {
        coEvery { servers.get("s1") } returns server
        coEvery { servers.update(any()) } returns Unit
        coEvery { endpoints.select(server) } returns stored
        every { sessions.create("s1", stored, any()) } returns session
        coEvery { session.accessToken() } returns "access"
        coEvery { bridge.fetch(any(), "access") } returns
            BridgeInfo(bridgeVersion = "1", apiVersion = 1, haVersion = "2026.1", instanceId = "uuid-1")
    }

    @Test
    fun `refresh token is redeemed only at the stored address`() = runTest {
        val new = "https://new.example.org".toHttpUrl()
        repository.change("s1", AddressSlot.INTERNAL, new)
        verify(exactly = 1) { sessions.create(any(), any(), any()) }
        verify { sessions.create("s1", stored, any()) }
        coVerify { bridge.fetch(new, "access") }
    }

    @Test
    fun `no working address ends the change without sending anything to the new one`() = runTest {
        coEvery { endpoints.select(server) } throws NetworkException(ErrorCode.NET_UNREACHABLE)
        val code = assertThrows<NetworkException> {
            repository.change("s1", AddressSlot.INTERNAL, "https://new.example.org".toHttpUrl())
        }.code
        assertEquals(ErrorCode.NET_UNREACHABLE, code)
        verify(exactly = 0) { sessions.create(any(), any(), any()) }
        coVerify(exactly = 0) { bridge.fetch(any(), any()) }
    }

    @Test
    fun `http address without a home network match gets no token`() = runTest {
        coEvery { homeNetwork.confirms("uuid-1", "192.168.1.10") } returns false
        val code = assertThrows<NetworkException> {
            repository.change("s1", AddressSlot.INTERNAL, "http://192.168.1.10:8123".toHttpUrl())
        }.code
        assertEquals(ErrorCode.NET_CLEARTEXT_NOT_ALLOWED, code)
        coVerify(exactly = 0) { bridge.fetch(any(), any()) }
        coVerify(exactly = 0) { servers.update(any()) }
    }

    @Test
    fun `http address the home network check confirms is stored`() = runTest {
        val local = "http://192.168.1.10:8123".toHttpUrl()
        coEvery { homeNetwork.confirms("uuid-1", "192.168.1.10") } returns true
        repository.change("s1", AddressSlot.INTERNAL, local)
        coVerify { bridge.fetch(local, "access") }
        coVerify { servers.update(match { it.internalUrl == local.toString() }) }
    }

    @Test
    fun `http address of an instance without ID gets no token`() = runTest {
        val unknown = server.copy(instanceUuid = null)
        coEvery { servers.get("s1") } returns unknown
        assertThrows<NetworkException> {
            repository.change("s1", AddressSlot.INTERNAL, "http://192.168.1.10:8123".toHttpUrl())
        }
        coVerify(exactly = 0) { bridge.fetch(any(), any()) }
    }
}
