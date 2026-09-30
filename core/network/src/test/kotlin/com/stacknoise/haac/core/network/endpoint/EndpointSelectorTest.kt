package com.stacknoise.haac.core.network.endpoint

import com.stacknoise.haac.core.database.server.ServerEntity
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.bridge.BridgeUrls
import com.stacknoise.haac.core.network.discovery.DiscoveredServer
import com.stacknoise.haac.core.network.discovery.ServerDiscovery
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EndpointSelectorTest {
    private val internal = "http://192.168.1.10:8123/"
    private val external = "https://abc.ui.nabu.casa/"
    private val probed = mutableListOf<String>()
    private val unreachable = mutableSetOf<String>()
    private var home = false
    private var localNetwork = true
    private val homeChecks = mutableListOf<Pair<String, String>>()

    private val selector = DefaultEndpointSelector(
        homeNetwork = { uuid, host ->
            homeChecks += uuid to host
            home
        },
        probe = { url: HttpUrl ->
            probed += url.toString()
            if (url.toString() in unreachable) throw NetworkException(ErrorCode.NET_UNREACHABLE)
        },
        localNetwork = { localNetwork },
    )

    private fun server(
        internalUrl: String? = internal,
        externalUrl: String? = external,
        uuid: String? = "f00d",
        always: Boolean = false,
    ) = ServerEntity(
        id = "1",
        instanceUuid = uuid,
        internalUrl = internalUrl,
        externalUrl = externalUrl,
        alwaysUseInternal = always,
        displayName = "Home",
        accentColor = 0,
        haUserName = "anna",
        haVersion = "2026.9.0",
        bridgeApiVersion = 1,
        lastActiveAt = 0,
    )

    @Test
    fun `http internal address needs the home network check`() = runTest {
        assertEquals(external, selector.select(server()).toString())
        assertEquals(listOf("f00d" to "192.168.1.10"), homeChecks)
        assertEquals(listOf(external), probed)

        home = true
        assertEquals(internal, selector.select(server()).toString())
    }

    @Test
    fun `https internal address and the always option skip the check`() = runTest {
        val https = "https://ha.lan:8123/"
        assertEquals(https, selector.select(server(internalUrl = https)).toString())
        assertEquals(internal, selector.select(server(always = true)).toString())
        assertTrue(homeChecks.isEmpty())
    }

    @Test
    fun `instance without ID uses its stored internal address`() = runTest {
        assertEquals(internal, selector.select(server(externalUrl = null, uuid = null)).toString())
    }

    @Test
    fun `unreachable internal address falls back to external`() = runTest {
        home = true
        unreachable += internal
        assertEquals(external, selector.select(server()).toString())
        assertEquals(listOf(internal, external), probed)
    }

    @Test
    fun `no usable address reports the last error`() = runTest {
        unreachable += external
        val error = assertThrows<NetworkException> { selector.select(server()) }
        assertEquals(ErrorCode.NET_UNREACHABLE, error.code)
        val none = assertThrows<NetworkException> { selector.select(server(externalUrl = null)) }
        assertEquals(ErrorCode.NET_UNREACHABLE, none.code)
    }

    @Test
    fun `mDNS check matches uuid and host and gives up after the timeout`() = runTest {
        val found = DiscoveredServer("f00d", "Home", "", "", null, setOf("192.168.1.10", "homeassistant.local"))
        val other = found.copy(id = "beef")
        fun check(servers: Flow<List<DiscoveredServer>>) = NsdHomeNetworkCheck(
            object : ServerDiscovery {
                override fun servers() = servers
            },
        )

        assertTrue(check(flowOf(emptyList(), listOf(other, found))).confirms("f00d", "192.168.1.10"))
        assertTrue(check(flowOf(listOf(found))).confirms("f00d", "HomeAssistant.local"))
        assertFalse(check(flowOf(listOf(found))).confirms("f00d", "192.168.1.99"))
        assertFalse(check(flowOf(listOf(other))).confirms("f00d", "192.168.1.10"))
        assertFalse(check(flow { awaitCancellation() }).confirms("f00d", "192.168.1.10"))
    }

    @Test
    fun `internal address is skipped without local network access`() = runTest {
        home = true
        localNetwork = false
        assertEquals(external, selector.select(server(always = true)).toString())
        assertEquals(listOf(external), probed)
        assertEquals(emptyList<Pair<String, String>>(), homeChecks)
    }

    @Test
    fun `sign-in fills the empty slot from HA and skips cleartext to public hosts`() {
        val urls = BridgeUrls(
            internal = "http://192.168.1.10:8123",
            external = "http://ha.example.com",
            cloud = "https://abc.ui.nabu.casa",
        )
        val viaLan = InstanceAddresses.afterSignIn("http://homeassistant.local:8123/".toHttpUrl(), urls)
        assertEquals("http://homeassistant.local:8123/", viaLan.internal.toString())
        assertEquals(external, viaLan.external.toString())

        val viaCloud = InstanceAddresses.afterSignIn(external.toHttpUrl(), urls)
        assertEquals(internal, viaCloud.internal.toString())
        assertEquals(external, viaCloud.external.toString())
    }

    @Test
    fun `changed address drops its pin and a foreign instance ID is NET-008`() {
        val pinned = server().copy(internalPinnedKeyHash = "a", externalPinnedKeyHash = "b")
        val moved = pinned.addresses.with(AddressSlot.EXTERNAL, "https://ha.example.com".toHttpUrl())
        val changed = pinned.withAddresses(moved)
        assertEquals("a", changed.internalPinnedKeyHash)
        assertEquals(null, changed.externalPinnedKeyHash)

        requireSameInstance("f00d", "f00d")
        requireSameInstance(null, "f00d")
        requireSameInstance("f00d", null)
        val error = assertThrows<NetworkException> { requireSameInstance("f00d", "beef") }
        assertEquals(ErrorCode.NET_WRONG_SERVER, error.code)
    }
}
