package com.stacknoise.haac.core.network.auth

import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.network.http.HaHttpClient
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TokenClientTest {
    private val server = MockWebServer()
    private val http = HaHttpClient(OkHttpClient(), DefaultErrorFactory())
    private val tokens = TokenClient(http, Json { ignoreUnknownKeys = true })

    @BeforeEach
    fun start() = server.start()

    @AfterEach
    fun stop() = server.close()

    private fun respond(code: Int, body: String = "") =
        server.enqueue(MockResponse.Builder().code(code).body(body).build())

    @Test
    fun `code is exchanged for both tokens`() = runTest {
        respond(
            200,
            """{"access_token":"acc","token_type":"Bearer","refresh_token":"ref","expires_in":1800,""" +
                """"ha_auth_provider":"homeassistant"}""",
        )
        val result = tokens.exchangeCode(server.url("/"), "code-1")
        assertEquals("acc", result.accessToken)
        assertEquals("ref", result.refreshToken)
        assertEquals(1800, result.expiresInSeconds)
        assertFalse(result.toString().contains("ref"))
        val form = server.takeRequest().body!!.utf8()
        assertEquals(
            "grant_type=authorization_code&code=code-1&client_id=https%3A%2F%2Fstacknoise.com%2Fhaac%2F",
            form,
        )
    }

    @Test
    fun `refresh returns a new access token`() = runTest {
        respond(200, """{"access_token":"acc2","token_type":"Bearer","expires_in":1800}""")
        assertEquals("acc2", tokens.refresh(server.url("/"), "ref").token)
        assertEquals("/auth/token", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `revoked refresh token is AUTH-003, blocked user AUTH-006`() = runTest {
        respond(400, """{"error":"invalid_grant"}""")
        assertEquals(ErrorCode.AUTH_SESSION_EXPIRED, refreshError())
        respond(403, """{"error":"access_denied"}""")
        assertEquals(ErrorCode.AUTH_USER_BLOCKED, refreshError())
    }

    private suspend fun refreshError(): ErrorCode =
        assertThrows<AuthException> { tokens.refresh(server.url("/"), "r") }.code

    @Test
    fun `revoke posts the token to auth revoke`() = runTest {
        respond(200)
        tokens.revoke(server.url("/"), "ref")
        val request = server.takeRequest()
        assertEquals("/auth/revoke", request.url.encodedPath)
        assertEquals("token=ref", request.body!!.utf8())
    }
}
