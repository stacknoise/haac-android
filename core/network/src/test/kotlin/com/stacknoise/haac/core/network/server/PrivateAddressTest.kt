package com.stacknoise.haac.core.network.server

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.NetworkException
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PrivateAddressTest {
    @ParameterizedTest
    @ValueSource(
        strings = [
            "192.168.1.10", "10.0.0.5", "172.16.0.1", "172.31.255.255", "127.0.0.1", "169.254.3.4",
            "homeassistant.local", "HA.LOCAL.", "localhost", "::1", "[fd00::1]", "fe80::1%wlan0",
        ],
    )
    fun `private addresses`(host: String) {
        assertTrue(PrivateAddress.isPrivate(host), host)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "8.8.8.8", "172.32.0.1", "192.169.0.1", "ha.example.com", "homeassistant", "2001:db8::1", "999.168.1.1",
        ],
    )
    fun `public or unknown addresses`(host: String) {
        assertFalse(PrivateAddress.isPrivate(host), host)
    }

    @Test
    fun `cleartext only to private hosts`() {
        CleartextPolicy.requireAllowed("http://192.168.1.10:8123/".toHttpUrl())
        CleartextPolicy.requireAllowed("https://ha.example.com/".toHttpUrl())
        val publicHttp = "http://ha.example.com/".toHttpUrl()
        val error = assertThrows<NetworkException> { CleartextPolicy.requireAllowed(publicHttp) }
        assertEquals(ErrorCode.NET_CLEARTEXT_NOT_ALLOWED, error.code)
        assertTrue(CleartextPolicy.needsWarning("http://192.168.1.10:8123/".toHttpUrl()))
        assertFalse(CleartextPolicy.needsWarning("https://ha.example.com/".toHttpUrl()))
    }
}
