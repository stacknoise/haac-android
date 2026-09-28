package com.stacknoise.haac.feature.onboarding.data

import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.server.ServerEntity
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.StorageException
import com.stacknoise.haac.core.network.auth.AuthTokens
import com.stacknoise.haac.core.network.auth.LoginFlowClient
import com.stacknoise.haac.core.network.auth.TokenClient
import com.stacknoise.haac.core.network.bridge.BridgeInfo
import com.stacknoise.haac.core.network.bridge.BridgeInfoClient
import com.stacknoise.haac.core.network.bridge.BridgeUrls
import com.stacknoise.haac.core.network.bridge.DefaultBridgeMessageFactory
import com.stacknoise.haac.core.network.endpoint.AddressSlot
import com.stacknoise.haac.core.network.endpoint.EndpointSelector
import com.stacknoise.haac.core.network.endpoint.addresses
import com.stacknoise.haac.core.network.http.HaHttpClient
import com.stacknoise.haac.core.network.websocket.OkHttpWebSocketFactory
import com.stacknoise.haac.core.security.token.TokenProtection
import com.stacknoise.haac.core.security.token.TokenStore
import com.stacknoise.haac.feature.onboarding.domain.SignInResult
import com.stacknoise.haac.feature.onboarding.domain.SignInStep
import com.stacknoise.haac.feature.onboarding.domain.SignInTarget
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HaSignInRepositoryTest {
    private val server = MockWebServer()
    private val json = Json { ignoreUnknownKeys = true }
    private val http = HaHttpClient(OkHttpClient(), DefaultErrorFactory())

    private val rows = mutableMapOf<String, ServerEntity>()
    private var failInsert = false
    private val dao = object : ServerDao {
        override suspend fun insert(server: ServerEntity) {
            if (failInsert) throw StorageException(ErrorCode.DB_SAVE_FAILED)
            rows[server.id] = server
        }

        override suspend fun update(server: ServerEntity) {
            rows[server.id] = server
        }

        override suspend fun get(id: String) = rows[id]

        override suspend fun findByInstance(uuid: String, user: String) = rows.values.firstOrNull {
            it.instanceUuid == uuid && it.haUserName.equals(user, ignoreCase = true)
        }

        override suspend fun mostRecent() = rows.values.maxByOrNull { it.lastActiveAt }

        override fun observe(id: String): Flow<ServerEntity?> = flowOf(rows[id])

        override suspend fun count() = rows.size
    }
    private val saved = mutableMapOf<String, String>()
    private val tokenStore = object : TokenStore {
        override suspend fun save(serverId: String, refreshToken: String) {
            saved[serverId] = refreshToken
        }

        override suspend fun read(serverId: String) = saved[serverId]

        override suspend fun contains(serverId: String) = serverId in saved

        override suspend fun protection(serverId: String) = if (serverId in saved) TokenProtection.DeviceKey else null

        override suspend fun delete(serverId: String) {
            saved.remove(serverId)
        }
    }
    private val active = object : ActiveInstanceStore {
        override val activeServerId = MutableStateFlow<String?>(null)

        override suspend fun setActive(serverId: String?) {
            activeServerId.value = serverId
        }
    }

    private val registry = InstanceRegistry(dao, tokenStore, active, DefaultErrorFactory())
    private val firstAddress = EndpointSelector { row -> row.addresses.external ?: row.addresses.internal!! }
    private val repository = HaSignInRepository(
        LoginFlowClient(http, json),
        TokenClient(http, json),
        BridgeInfoClient(
            OkHttpWebSocketFactory(OkHttpClient(), json, DefaultErrorFactory()),
            json,
            DefaultBridgeMessageFactory(),
            DefaultErrorFactory(),
        ),
        registry,
        firstAddress,
    )

    @BeforeEach
    fun start() = server.start()

    @AfterEach
    fun stop() = server.close()

    private fun ha(vararg bodies: String) = bodies.forEach {
        server.enqueue(MockResponse.Builder().body(it).build())
    }

    private fun form(step: String, error: String = "", schema: String = "[]") =
        """{"type":"form","flow_id":"f1","step_id":"$step","data_schema":$schema,""" +
            """"errors":${if (error.isEmpty()) "{}" else """{"base":"$error"}"""}}"""

    private val tokenJson = """{"access_token":"acc","refresh_token":"ref","expires_in":1800}"""

    @Test
    fun `password login returns tokens`() = runTest {
        ha(form("init"), """{"type":"create_entry","flow_id":"f1","result":"code"}""", tokenJson)
        val step = repository.signIn(server.url("/"), "anna", "pw".toCharArray())
        assertEquals("ref", assertInstanceOf(SignInStep.Authorized::class.java, step).tokens.refreshToken)
    }

    @Test
    fun `wrong password is AUTH-001, wrong code AUTH-002`() = runTest {
        ha(form("init"), form("init", error = "invalid_auth"))
        val wrong = assertThrows<AuthException> { repository.signIn(server.url("/"), "anna", "x".toCharArray()) }
        assertEquals(ErrorCode.AUTH_INVALID_CREDENTIALS, wrong.code)
        ha(form("mfa", error = "invalid_code"))
        val code = assertThrows<AuthException> { repository.submitCode(server.url("/"), "f1", "1") }
        assertEquals(ErrorCode.AUTH_INVALID_MFA_CODE, code.code)
    }

    @Test
    fun `with several MFA modules the first is chosen`() = runTest {
        val select = form(
            "select_mfa_module",
            schema = """[{"name":"multi_factor_auth_module","options":[["totp","App"],["notify","Notify"]]}]""",
        )
        ha(form("init"), select, form("mfa"))
        val step = repository.signIn(server.url("/"), "anna", "pw".toCharArray())
        assertEquals(SignInStep.CodeRequired("f1"), step)
        server.takeRequest()
        server.takeRequest()
        assertTrue(server.takeRequest().body!!.utf8().contains("\"multi_factor_auth_module\":\"totp\""))
    }

    @Test
    fun `registry stores a new instance and makes it active`() = runTest {
        val bridge = BridgeInfo("0.1.0", 1, listOf("switch"), "2026.9.0")
        val id = registry.save(SignInTarget(server.url("/"), "Home"), "anna", bridge, "ref")
        assertEquals("ref", saved[id])
        assertEquals(id, active.activeServerId.value)
        assertEquals("Home", rows.getValue(id).displayName)
        assertEquals(0xFF4DFF7A, rows.getValue(id).accentColor)

        val again = registry.save(SignInTarget(server.url("/"), "Home", id), "anna", bridge, "ref2")
        assertEquals(id, again)
        assertEquals(1, rows.size)
        assertEquals("ref2", saved[id])
    }

    @Test
    fun `a failed insert removes the token again`() = runTest {
        failInsert = true
        val bridge = BridgeInfo("0.1.0", 1, emptyList(), "2026.9.0")
        assertThrows<StorageException> { registry.save(SignInTarget(server.url("/"), "Home"), "anna", bridge, "ref") }
        assertTrue(saved.isEmpty())
        assertEquals(null, active.activeServerId.value)
    }

    private val info = BridgeInfo(
        "0.1.0",
        1,
        listOf("switch"),
        "2026.9.0",
        instanceId = "f00d",
        urls = BridgeUrls(internal = "http://192.168.1.10:8123", cloud = "https://abc.ui.nabu.casa"),
    )

    /** A repository whose bridge check answers with [bridge] without a WebSocket. */
    private fun repositoryWith(bridge: BridgeInfo) = HaSignInRepository(
        LoginFlowClient(http, json),
        TokenClient(http, json),
        mockk<BridgeInfoClient> { coEvery { fetch(any(), any()) } returns bridge },
        registry,
        firstAddress,
    )

    private val newTokens = AuthTokens("acc", "ref-new", 1800)

    @Test
    fun `new instance stores instance ID and both addresses`() = runTest {
        val url = "https://abc.ui.nabu.casa/".toHttpUrl()
        val result = repositoryWith(info).finish(SignInTarget(url, "Home"), "anna", newTokens)
        val row = rows.getValue((result as SignInResult.Saved).serverId)
        assertEquals("f00d", row.instanceUuid)
        assertEquals("http://192.168.1.10:8123/", row.internalUrl)
        assertEquals("https://abc.ui.nabu.casa/", row.externalUrl)
    }

    @Test
    fun `same server and user becomes an address offer, another user a new instance`() = runTest {
        val lan = "http://homeassistant.local:8123/".toHttpUrl()
        val first = repositoryWith(info).finish(SignInTarget(lan, "Home"), "anna", newTokens) as SignInResult.Saved

        val offer = repositoryWith(info).finish(SignInTarget(lan, "Home 2"), "Anna", newTokens)
        assertEquals(SignInResult.SameInstance(first.serverId, "Home", AddressSlot.INTERNAL, null), offer)
        assertEquals(1, rows.size)

        val guest = repositoryWith(info).finish(SignInTarget(lan, "Home"), "guest", newTokens)
        assertInstanceOf(SignInResult.Saved::class.java, guest)
        assertEquals(2, rows.size)
    }

    @Test
    fun `adding an address keeps the stored token and revokes the new one`() = runTest {
        val cloud = "https://abc.ui.nabu.casa/".toHttpUrl()
        val id = registry.save(SignInTarget(cloud, "Home"), "anna", info.copy(urls = BridgeUrls()), "ref")
        server.enqueue(MockResponse.Builder().code(200).build())

        val lan = server.url("/")
        repositoryWith(info).addAddress(id, lan, newTokens)

        assertEquals(lan.toString(), rows.getValue(id).internalUrl)
        assertEquals("https://abc.ui.nabu.casa/", rows.getValue(id).externalUrl)
        assertEquals("ref", saved[id])
        assertTrue(server.takeRequest().body!!.utf8().contains("token=ref-new"))
    }

    @Test
    fun `stored instance answering with another instance ID is NET-008`() = runTest {
        val url = "https://abc.ui.nabu.casa/".toHttpUrl()
        val id = registry.save(SignInTarget(url, "Home"), "anna", info, "ref")
        val error = assertThrows<NetworkException> {
            repositoryWith(info.copy(instanceId = "beef")).finish(SignInTarget(url, "Home", id), "anna", newTokens)
        }
        assertEquals(ErrorCode.NET_WRONG_SERVER, error.code)
        assertEquals("ref", saved[id])
    }
}
