package com.stacknoise.haac.core.network.tls

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.HttpUrl

/**
 * The pinned public-key hashes by `host:port` (concept 4.3). [stored] pins mirror the `server` table;
 * pins the user trusted during a sign-in that is not saved yet are kept in memory only, until [forget] or
 * until the instance is stored. A change of a pin is reported to the listeners of [onChange], so open connections
 * that were trusted under the old pins can be dropped (review S-11).
 */
@Singleton
class PinRegistry @Inject constructor() {
    private val stored = ConcurrentHashMap<String, String>()
    private val trusted = ConcurrentHashMap<String, String>()
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    /** Calls [listener] after every change of a pin, on the thread that changed it. */
    fun onChange(listener: () -> Unit) {
        listeners.add(listener)
    }

    /** The pinned key hash for [host] and [port], or null if that address is not pinned. */
    fun pinFor(host: String, port: Int): String? = stored[key(host, port)] ?: trusted[key(host, port)]

    /** The pinned key hash for the address [url], or null. */
    fun pinFor(url: HttpUrl): String? = pinFor(url.host, url.port)

    /** Replaces all stored pins with [pins] (`host:port` to key hash), as read from the database. */
    fun replaceStored(pins: Map<String, String>) {
        if (stored.toMap() == pins) return
        stored.keys.retainAll(pins.keys)
        stored.putAll(pins)
        listeners.forEach { it() }
    }

    /** Trusts [keyHash] for [url] until the app closes or [forget] is called. */
    fun trust(url: HttpUrl, keyHash: String) {
        if (trusted.put(key(url.host, url.port), keyHash) != keyHash) listeners.forEach { it() }
    }

    /** Removes the pin of [url] everywhere. */
    fun forget(url: HttpUrl) {
        val removed = stored.remove(key(url.host, url.port)) ?: trusted.remove(key(url.host, url.port))
        trusted.remove(key(url.host, url.port))
        if (removed != null) listeners.forEach { it() }
    }

    /** Key helpers. */
    companion object {
        /** The registry key of an address: lower-case host, colon, port. */
        fun key(host: String, port: Int): String = "${host.lowercase()}:$port"

        /** The registry key of [url]. */
        fun key(url: HttpUrl): String = key(url.host, url.port)
    }
}
