package com.stacknoise.haac.core.network.server

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.NetworkException
import org.junit.jupiter.api.Assertions.assertEquals
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
}
