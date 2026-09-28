package com.stacknoise.haac.core.network.endpoint

import com.stacknoise.haac.core.database.server.ServerEntity
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.bridge.BridgeUrls
import com.stacknoise.haac.core.network.server.PrivateAddress
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** The two address slots of an instance (concept 4.5). */
enum class AddressSlot {
    INTERNAL,
    EXTERNAL,
    ;

    /** Companion with the slot rule. */
    companion object {
        /** Internal for a private host (concept 4.3), otherwise external. */
        fun of(url: HttpUrl): AddressSlot = if (PrivateAddress.isPrivate(url.host)) INTERNAL else EXTERNAL
    }
}

/** Internal and external address of an instance. */
data class InstanceAddresses(val internal: HttpUrl?, val external: HttpUrl?) {
    /** The address in [slot]. */
    operator fun get(slot: AddressSlot): HttpUrl? = if (slot == AddressSlot.INTERNAL) internal else external

    /** A copy with [url] in [slot]. */
    fun with(slot: AddressSlot, url: HttpUrl?): InstanceAddresses =
        if (slot == AddressSlot.INTERNAL) copy(internal = url) else copy(external = url)

    /** Companion with the sign-in rule. */
    companion object {
        /**
         * Addresses after a sign-in at [entered] (concept 4.5): [entered] in its slot, the other slot from
         * HA's [urls] (external preferred over cloud); addresses the cleartext rule forbids are skipped.
         */
        fun afterSignIn(entered: HttpUrl, urls: BridgeUrls): InstanceAddresses =
            fromHa(urls).with(AddressSlot.of(entered), entered)

        /** The addresses HA reports; external is preferred over cloud, forbidden cleartext is skipped. */
        fun fromHa(urls: BridgeUrls): InstanceAddresses =
            InstanceAddresses(allowed(urls.internal), allowed(urls.external) ?: allowed(urls.cloud))

        /** [url] as HttpUrl if it parses and `http://` only goes to a private host. */
        private fun allowed(url: String?): HttpUrl? =
            url?.toHttpUrlOrNull()?.takeIf { it.isHttps || PrivateAddress.isPrivate(it.host) }
    }
}

/** The stored addresses of this instance. */
val ServerEntity.addresses: InstanceAddresses
    get() = InstanceAddresses(internalUrl?.toHttpUrlOrNull(), externalUrl?.toHttpUrlOrNull())

/** A copy with [addresses]; the pin of a changed address is dropped (concept 4.3). */
fun ServerEntity.withAddresses(addresses: InstanceAddresses): ServerEntity {
    val internal = addresses.internal?.toString()
    val external = addresses.external?.toString()
    return copy(
        internalUrl = internal,
        externalUrl = external,
        internalPinnedKeyHash = internalPinnedKeyHash.takeIf { internal == internalUrl },
        externalPinnedKeyHash = externalPinnedKeyHash.takeIf { external == externalUrl },
    )
}

/** Throws HAAC-NET-008 if the server reports an `instance_id` other than [expected] (concept 4.5). */
fun requireSameInstance(expected: String?, reported: String?) {
    if (expected != null && reported != null && expected != reported) {
        throw NetworkException(ErrorCode.NET_WRONG_SERVER)
    }
}
