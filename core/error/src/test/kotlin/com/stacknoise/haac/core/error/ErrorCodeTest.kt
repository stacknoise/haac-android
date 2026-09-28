package com.stacknoise.haac.core.error

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Checks the rules of concept 17.3 for every ErrorCode. */
class ErrorCodeTest {
    private val format = Regex("^HAAC-(NET|AUTH|SEC|BRG|SYNC|DB|LAY|ENT|INST|DISC|APP)-\\d{3}$")

    @Test
    fun `codes are unique`() {
        val codes = ErrorCode.entries.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun `codes match the format`() {
        ErrorCode.entries.forEach { assertTrue(format.matches(it.code), it.code) }
    }

    @Test
    fun `every code has a user text and a description`() {
        ErrorCode.entries.forEach {
            assertTrue(it.message != 0, it.code)
            assertTrue(it.description.isNotBlank(), it.code)
        }
    }

    @Test
    fun `enum name starts with the area of its code`() {
        ErrorCode.entries.forEach { assertTrue(it.name.startsWith(it.area + "_"), it.code) }
    }
}
