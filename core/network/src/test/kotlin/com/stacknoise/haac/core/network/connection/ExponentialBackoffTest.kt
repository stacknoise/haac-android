package com.stacknoise.haac.core.network.connection

import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExponentialBackoffTest {
    @Test
    fun `doubles from 1 s up to 60 s with jitter between half and full`() {
        val backoff = ExponentialBackoff(Random(42))
        val bases = listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 32_000L, 60_000L, 60_000L, 60_000L)
        bases.forEachIndexed { index, base ->
            repeat(50) {
                val delay = backoff.delayMs(index + 1)
                assertTrue(delay in base / 2..base, "failure ${index + 1}: $delay")
            }
        }
    }

    @Test
    fun `without jitter the wait is the full value`() {
        val maxJitter = object : Random() {
            override fun nextBits(bitCount: Int) = 0

            override fun nextLong(until: Long) = until - 1
        }
        assertEquals(listOf(1_000L, 2_000L, 60_000L), listOf(1, 2, 20).map(ExponentialBackoff(maxJitter)::delayMs))
    }
}
