package com.stacknoise.haac.feature.onboarding.domain

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.feature.onboarding.data.HaServiceParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class ServerUrlNormalizerTest {
    @ParameterizedTest
    @CsvSource(
        "ha.example.com, https://ha.example.com/",
        "  https://ha.example.com/lovelace/0  , https://ha.example.com/",
        "http://192.168.1.10:8123/, http://192.168.1.10:8123/",
        "192.168.1.10:8123, https://192.168.1.10:8123/",
        "HTTPS://HA.Example.com?x=1#top, https://ha.example.com/",
        "https://user:pw@ha.example.com, https://ha.example.com/",
    )
    fun `normalises input`(input: String, expected: String) {
        assertEquals(expected, ServerUrlNormalizer.normalize(input).toString())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   ", "ftp://ha.example.com", "ha example.com", "https://"])
    fun `rejects unusable input`(input: String) {
        val error = assertThrows<NetworkException> { ServerUrlNormalizer.normalize(input) }
        assertEquals(ErrorCode.NET_INVALID_ADDRESS, error.code)
    }

    @Test
    fun `parses the zeroconf TXT record`() {
        val txt = mapOf(
            "location_name" to "Home".toByteArray(),
            "uuid" to "abc".toByteArray(),
            "version" to "2026.9.0".toByteArray(),
            "base_url" to "http://homeassistant.local:8123".toByteArray(),
            "internal_url" to null,
        )
        val server = HaServiceParser.parse("Home._home-assistant._tcp", "192.168.1.10", 8123, txt)
        val expected =
            DiscoveredServer("abc", "Home", "192.168.1.10:8123", "http://homeassistant.local:8123", "2026.9.0")
        assertEquals(expected, server)
    }

    @Test
    fun `falls back to IP and port without TXT`() {
        val server = HaServiceParser.parse("HA", "fd00::10", 8123, emptyMap())
        assertEquals(DiscoveredServer("HA", "HA", "[fd00::10]:8123", "http://[fd00::10]:8123", null), server)
    }
}
