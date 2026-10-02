package com.stacknoise.haac.core.network.session

import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.network.demo.DemoInstance
import com.stacknoise.haac.core.network.demo.DemoWorld
import com.stacknoise.haac.core.network.demo.MemoryDemoWorldStore
import com.stacknoise.haac.core.network.endpoint.EndpointSelector
import com.stacknoise.haac.core.security.token.TokenStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class InstanceSignOutTest {
    private val store = MemoryDemoWorldStore()
    private val world = DemoWorld(store, Json { ignoreUnknownKeys = true })
    private val tokens = mockk<TokenStore>(relaxed = true)
    private val servers = mockk<ServerDao>()
    private val endpoints = mockk<EndpointSelector>()
    private val sessions = mockk<InstanceSessionFactory>()
    private val signOut = InstanceSignOut(servers, endpoints, sessions, tokens, world)

    @Test
    fun `the demo deletes its token and its saved state and calls no server`() = runTest {
        world.snapshot()
        assertNotNull(store.saved)
        signOut.signOut(DemoInstance.SERVER_ID)
        coVerify { tokens.delete(DemoInstance.SERVER_ID) }
        assertNull(store.saved)
        confirmVerified(servers, endpoints, sessions)
    }

    @Test
    fun `another instance without an answering address only loses its token and leaves the demo alone`() = runTest {
        world.snapshot()
        coEvery { servers.get("real") } returns null
        signOut.signOut("real")
        coVerify { tokens.delete("real") }
        assertNotNull(store.saved)
    }
}
