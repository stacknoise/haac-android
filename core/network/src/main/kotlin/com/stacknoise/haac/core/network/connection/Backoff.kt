package com.stacknoise.haac.core.network.connection

import javax.inject.Inject
import kotlin.random.Random

/** Wait time before the next connection attempt (concept 11.4). */
fun interface Backoff {
    /** Milliseconds to wait after [failures] failed attempts in a row (1 or more). */
    fun delayMs(failures: Int): Long
}

/** Exponential back-off 1 s, 2 s, 4 s … 60 s; the jitter spreads each wait over 50–100 % of its value. */
class ExponentialBackoff internal constructor(private val random: Random) : Backoff {
    /** Uses the default random source. */
    @Inject
    constructor() : this(Random.Default)

    /** Doubles per failure up to the maximum, then applies the jitter. */
    override fun delayMs(failures: Int): Long {
        val exponent = (failures - 1).coerceIn(0, MAX_EXPONENT)
        val base = (INITIAL_MS shl exponent).coerceAtMost(MAX_MS)
        return base / 2 + random.nextLong(base / 2 + 1)
    }

    /** Back-off limits. */
    private companion object {
        const val INITIAL_MS = 1_000L
        const val MAX_MS = 60_000L
        const val MAX_EXPONENT = 6
    }
}
