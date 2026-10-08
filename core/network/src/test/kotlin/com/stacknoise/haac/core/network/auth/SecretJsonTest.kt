package com.stacknoise.haac.core.network.auth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SecretJsonTest {
    private fun parse(password: String): JsonObject {
        val bytes = SecretJson.credentials("https://x/", "anna", password.toCharArray())
        return Json.parseToJsonElement(String(bytes, Charsets.UTF_8)) as JsonObject
    }

    @Test
    fun `plain password round trips`() {
        val json = parse("secret")
        assertEquals("anna", json.getValue("username").jsonPrimitive.content)
        assertEquals("secret", json.getValue("password").jsonPrimitive.content)
        assertEquals("https://x/", json.getValue("client_id").jsonPrimitive.content)
    }

    @Test
    fun `quotes, backslashes and control characters are escaped`() {
        val password = "a\"b\\c\nd\te\u0001"
        assertEquals(password, parse(password).getValue("password").jsonPrimitive.content)
    }

    @Test
    fun `multibyte characters and emoji are encoded as UTF-8`() {
        val password = "Pässwörd€😀".repeat(40)
        assertEquals(password, parse(password).getValue("password").jsonPrimitive.content)
    }

    @Test
    fun `a lone surrogate becomes the replacement character`() {
        assertEquals("a�b", parse("a\uD800b").getValue("password").jsonPrimitive.content)
    }
}
