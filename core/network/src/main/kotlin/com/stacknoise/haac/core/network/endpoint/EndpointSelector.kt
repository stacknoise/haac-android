package com.stacknoise.haac.core.network.endpoint

import com.stacknoise.haac.core.database.server.ServerEntity
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.access.LocalNetworkAccess
import javax.inject.Inject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Chooses the address of an instance to connect to (concept 4.5, 17.2). */
fun interface EndpointSelector {
    /** The first usable address of [server]; the error of the last attempt if none answers. */
    suspend fun select(server: ServerEntity): HttpUrl
}

/**
 * Selection rules of concept 4.5: the internal address first if it is `https://`, the user allowed it
 * always, the home network check confirms it, or the instance has no ID yet (schema 1) and no external address;
 * then the external address. Each candidate is probed before it is returned.
 */
class DefaultEndpointSelector @Inject constructor(
    private val homeNetwork: HomeNetworkCheck,
    private val probe: EndpointProbe,
    private val localNetwork: LocalNetworkAccess,
) : EndpointSelector {
    /** Tries the candidates in order. */
    override suspend fun select(server: ServerEntity): HttpUrl {
        var failure: HaacException = NetworkException(ErrorCode.NET_UNREACHABLE)
        for (candidate in candidates(server)) {
            try {
                probe.check(candidate)
                return candidate
            } catch (e: HaacException) {
                failure = e
            }
        }
        throw failure
    }

    /** Internal address if allowed and local network access is granted, then external address. */
    private suspend fun candidates(server: ServerEntity): List<HttpUrl> = listOfNotNull(
        server.internalUrl?.toHttpUrlOrNull()?.takeIf { localNetwork.granted() && internalAllowed(server, it) },
        server.externalUrl?.toHttpUrlOrNull(),
    )

    /**
     * A device that is not this server cannot complete TLS; plain `http://` needs the home network check or the
     * user's choice. An instance without ID (schema 1) gets no exception while another address exists, because the
     * refresh token must not go to an unverified `http://` address (review S-01).
     */
    private suspend fun internalAllowed(server: ServerEntity, url: HttpUrl): Boolean {
        if (url.isHttps || server.alwaysUseInternal) return true
        val uuid = server.instanceUuid ?: return server.externalUrl == null
        return homeNetwork.confirms(uuid, url.host)
    }
}
