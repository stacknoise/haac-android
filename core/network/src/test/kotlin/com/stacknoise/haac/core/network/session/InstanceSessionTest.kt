package com.stacknoise.haac.core.network.session

import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.network.auth.TokenClient
import com.stacknoise.haac.core.network.http.HaHttpClient
import com.stacknoise.haac.core.security.token.TokenStore
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InstanceSessionTest {
    private val server = MockWebServer()
    private val stored = mutableMapOf("s1" to "ref")
    private var now = 0L

    private val store = object : TokenStore {
        override suspend fun save(serverId: String, refreshToken: String) {
            stored[serverId] = refreshToken
        }

        override suspend fun read(serverId: String) = stored[serverId]

        override suspend fun contains(serverId: String) = serverId in stored

        override suspend fun delete(serverId: String) {
            stored.remove(serverId)
        }
    }

    private lateinit var session: InstanceSession

    @BeforeEach
    fun start() {
        server.start()
        val tokens = TokenClient(HaHttpClient(OkHttpClient(), DefaultErrorFactory()), Json { ignoreUnknownKeys = true })
        session = InstanceSession("s1", server.url("/"), tokens, store) { now }
    }

    @AfterEach
    fun stop() = server.close()

    private fun accessToken(token: String) = server.enqueue(
        MockResponse.Builder().body("""{"access_token":"$token","token_type":"Bearer","expires_in":1800}""").build(),
    )

    @Test
    fun `access token is cached until shortly before expiry`() = runTest {
        accessToken("a1")
        accessToken("a2")
        assertEquals("a1", session.accessToken())
        now = 1_000_000L
        assertEquals("a1", session.accessToken())
        now = 1_750_000L
        assertEquals("a2", session.accessToken())
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `revoked refresh token is deleted`() = runTest {
        server.enqueue(MockResponse.Builder().code(400).body("""{"error":"invalid_grant"}""").build())
        assertEquals(ErrorCode.AUTH_SESSION_EXPIRED, assertThrows<AuthException> { session.accessToken() }.code)
        assertFalse("s1" in stored)
    }

    @Test
    fun `sign out revokes and deletes, also when the server is unreachable`() = runTest {
        server.enqueue(MockResponse.Builder().code(200).build())
        assertTrue(session.signOut())
        assertEquals("token=ref", server.takeRequest().body!!.utf8())
        assertFalse("s1" in stored)

        stored["s1"] = "ref2"
        server.close()
        assertFalse(session.signOut())
        assertFalse("s1" in stored)
    }
}
