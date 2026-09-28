package com.stacknoise.haac.app.start

import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.server.ServerEntity
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.security.token.TokenProtection
import com.stacknoise.haac.core.security.token.TokenStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StartRouterTest {
    private var activeId: String? = null
    private val instances = mutableListOf<ServerEntity>()
    private val protections = mutableMapOf<String, TokenProtection>()

    private val router = StartRouter(
        active = object : ActiveInstanceStore {
            override val activeServerId: Flow<String?> get() = flowOf(activeId)

            override suspend fun setActive(serverId: String?) = error("not used")
        },
        servers = object : ServerDao {
            override suspend fun get(id: String) = instances.firstOrNull { it.id == id }

            override suspend fun findByInstance(uuid: String, user: String) = error("not used")

            override suspend fun mostRecent() = instances.maxByOrNull { it.lastActiveAt }

            override suspend fun insert(server: ServerEntity) = error("not used")

            override suspend fun update(server: ServerEntity) = error("not used")

            override fun observe(id: String): Flow<ServerEntity?> = error("not used")

            override suspend fun count() = instances.size
        },
        tokens = object : TokenStore {
            override suspend fun protection(serverId: String) = protections[serverId]

            override suspend fun contains(serverId: String) = error("not used")

            override suspend fun save(serverId: String, refreshToken: String) = error("not used")

            override suspend fun read(serverId: String): String? = error("not used")

            override suspend fun delete(serverId: String) = error("not used")
        },
    )

    private fun instance(id: String, lastActiveAt: Long) =
        ServerEntity(
            id = id,
            externalUrl = "https://$id.example.com/",
            displayName = id,
            accentColor = 0xFF4DFF7A,
            haUserName = "anna",
            haVersion = "2026.9.0",
            bridgeApiVersion = 1,
            lastActiveAt = lastActiveAt,
        )

    @Test
    fun `no instance opens the onboarding`() = runTest {
        assertEquals(StartRoute.Onboarding, router.route())
    }

    @Test
    fun `active instance with token opens directly, without token asks for the login`() = runTest {
        instances += instance("a", lastActiveAt = 1)
        instances += instance("b", lastActiveAt = 2)
        activeId = "a"
        assertEquals(StartRoute.SignIn("a"), router.route())
        protections["a"] = TokenProtection.DeviceKey
        assertEquals(StartRoute.Main("a"), router.route())
    }

    @Test
    fun `fingerprint-protected token opens the unlock screen`() = runTest {
        instances += instance("a", lastActiveAt = 1)
        protections["a"] = TokenProtection.Fingerprint(generation = 1, unlockWindowSeconds = 0)
        assertEquals(StartRoute.Unlock("a"), router.route())
    }

    @Test
    fun `unknown active id falls back to the most recent instance`() = runTest {
        instances += instance("a", lastActiveAt = 1)
        instances += instance("b", lastActiveAt = 2)
        activeId = "gone"
        assertEquals(StartRoute.SignIn("b"), router.route())
    }
}
