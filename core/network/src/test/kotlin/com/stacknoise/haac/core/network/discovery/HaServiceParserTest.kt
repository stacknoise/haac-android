package com.stacknoise.haac.core.network.discovery

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HaServiceParserTest {
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
        val expected = DiscoveredServer(
            "abc",
            "Home",
            "192.168.1.10:8123",
            "http://homeassistant.local:8123",
            "2026.9.0",
            setOf("homeassistant.local", "192.168.1.10"),
        )
        assertEquals(expected, server)
    }

    @Test
    fun `falls back to IP and port without TXT`() {
        val server = HaServiceParser.parse("HA", "FD00::10%wlan0", 8123, emptyMap())
        val expected = DiscoveredServer("HA", "HA", "[FD00::10%wlan0]:8123", "http://[FD00::10%wlan0]:8123", null)
        assertEquals(expected.copy(hosts = setOf("fd00::10")), server)
    }
}
