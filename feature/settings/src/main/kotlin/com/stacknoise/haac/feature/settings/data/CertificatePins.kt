package com.stacknoise.haac.feature.settings.data

import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.UnexpectedException
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.core.network.endpoint.AddressSlot
import com.stacknoise.haac.core.network.endpoint.pinOf
import com.stacknoise.haac.core.network.endpoint.withPin
import com.stacknoise.haac.core.network.tls.PinRegistry
import javax.inject.Inject
import okhttp3.HttpUrl

/** The pins of an instance's addresses (concept 4.3): trust on first use, re-pinning after a change, removal. */
class CertificatePins @Inject constructor(
    private val servers: ServerDao,
    private val pins: PinRegistry,
    private val errors: ErrorFactory,
) {
    /** The key hash pinned for the address in [slot] of instance [serverId], or null. */
    suspend fun pinOf(serverId: String, slot: AddressSlot): String? = load(serverId).pinOf(slot)

    /** Pins [keyHash] for the address [url] in [slot] of instance [serverId]. */
    suspend fun pin(serverId: String, slot: AddressSlot, url: HttpUrl, keyHash: String) {
        pins.trust(url, keyHash)
        val server = load(serverId)
        errors.database { servers.update(server.withPin(slot, keyHash)) }
    }

    /** Removes the pin of the address [url] in [slot]; the system's certificates decide again. */
    suspend fun removePin(serverId: String, slot: AddressSlot, url: HttpUrl) {
        pins.forget(url)
        val server = load(serverId)
        errors.database { servers.update(server.withPin(slot, null)) }
    }

    /** Trusts [keyHash] for [url] until the address is stored with it or the app closes. */
    fun trustForNow(url: HttpUrl, keyHash: String) = pins.trust(url, keyHash)

    /** The stored instance [serverId]; it exists while its settings are shown. */
    private suspend fun load(serverId: String) =
        errors.database { servers.get(serverId) } ?: throw UnexpectedException()
}
