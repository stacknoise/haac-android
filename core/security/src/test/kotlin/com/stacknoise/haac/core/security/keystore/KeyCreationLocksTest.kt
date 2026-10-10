package com.stacknoise.haac.core.security.keystore

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KeyCreationLocksTest {
    private val locks = KeyCreationLocks()

    @Test
    fun `two callers that miss the same key create it once`() {
        val stored = AtomicInteger(0)
        val created = AtomicInteger(0)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        val results = List(2) {
            pool.submit<Int> {
                start.await()
                locks.getOrCreate("token.s1", { stored.get().takeIf { it != 0 } }) {
                    Thread.sleep(CREATE_MS)
                    created.incrementAndGet().also(stored::set)
                }
            }
        }
        start.countDown()
        val keys = results.map { it.get(TIMEOUT_S, TimeUnit.SECONDS) }
        pool.shutdown()
        assertEquals(1, created.get())
        assertEquals(listOf(1, 1), keys)
    }

    @Test
    fun `different keys do not wait for each other`() {
        assertEquals(1, locks.getOrCreate("a", { null }) { 1 })
        assertEquals(2, locks.getOrCreate("b", { null }) { 2 })
    }

    /** Test timing. */
    private companion object {
        const val CREATE_MS = 100L
        const val TIMEOUT_S = 5L
    }
}
