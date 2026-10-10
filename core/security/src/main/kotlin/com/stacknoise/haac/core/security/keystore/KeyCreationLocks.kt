package com.stacknoise.haac.core.security.keystore

import java.util.concurrent.ConcurrentHashMap

/** One lock per key name, so two callers that miss the same key do not both create it (review S-14). */
internal class KeyCreationLocks {
    private val locks = ConcurrentHashMap<String, Any>()

    /** The key [name] from [load], or the one [create] makes; only one caller per name runs [create]. */
    fun <T : Any> getOrCreate(name: String, load: () -> T?, create: () -> T): T =
        synchronized(locks.getOrPut(name) { Any() }) { load() ?: create() }
}
