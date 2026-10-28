package com.stacknoise.haac.core.network.http

import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.network.auth.TokenClient
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserAgentInterceptorTest {
    @Test
    fun `token refresh carries the HAAC user agent`() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(
                MockResponse.Builder().code(200)
                    .body("""{"access_token":"acc","token_type":"Bearer","expires_in":1800}""").build(),
            )
            val client = OkHttpClient.Builder().addInterceptor(UserAgentInterceptor("0.8.1", 36)).build()
            val tokens = TokenClient(HaHttpClient(client, DefaultErrorFactory()), Json { ignoreUnknownKeys = true })
            tokens.refresh(server.url("/"), "ref")
            assertEquals("HAAC/0.8.1 (Android 36)", server.takeRequest().headers["User-Agent"])
        }
    }
}
