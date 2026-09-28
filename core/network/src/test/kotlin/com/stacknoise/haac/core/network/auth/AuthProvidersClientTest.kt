package com.stacknoise.haac.core.network.auth

import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.http.HaHttpClient
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AuthProvidersClientTest {
    private val server = MockWebServer()
    private val http = HaHttpClient(OkHttpClient(), DefaultErrorFactory())
    private val client = AuthProvidersClient(http, Json { ignoreUnknownKeys = true })

    @BeforeEach
    fun start() = server.start()

    @AfterEach
    fun stop() = server.close()

    private fun respond(code: Int, body: String) =
        server.enqueue(MockResponse.Builder().code(code).body(body).build())

    private suspend fun fetchError(): ErrorCode =
        assertThrows<NetworkException> { client.fetch(server.url("/")) }.code

    @Test
    fun `reads the current object format`() = runTest {
        respond(
            200,
            """{"providers":[{"name":"Home Assistant Local","id":null,"type":"homeassistant"}],""" +
                """"preselect_remember_me":false}""",
        )
        val providers = client.fetch(server.url("/"))
        assertEquals(listOf(AuthProvider("Home Assistant Local", "homeassistant")), providers)
        assertEquals("/auth/providers", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `reads the older list format`() = runTest {
        respond(200, """[{"name":"Trusted","id":"lan","type":"trusted_networks"}]""")
        assertEquals("trusted_networks", client.fetch(server.url("/")).single().type)
    }

    @Test
    fun `other servers are not Home Assistant`() = runTest {
        respond(404, "not found")
        assertEquals(ErrorCode.NET_NOT_HOME_ASSISTANT, fetchError())
        respond(200, "<html>router login</html>")
        assertEquals(ErrorCode.NET_NOT_HOME_ASSISTANT, fetchError())
    }

    @Test
    fun `unreachable server is NET-001`() = runTest {
        val url = server.url("/")
        server.close()
        assertEquals(ErrorCode.NET_UNREACHABLE, assertThrows<NetworkException> { client.fetch(url) }.code)
    }
}
