package com.stacknoise.haac.core.network.session

import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.network.demo.DemoInstance
import com.stacknoise.haac.core.network.demo.DemoWorld
import com.stacknoise.haac.core.network.endpoint.EndpointSelector
import com.stacknoise.haac.core.security.token.TokenStore
import javax.inject.Inject
import okhttp3.HttpUrl

/**
 * Ends the login of one instance (concept 5.2, 4.4): the refresh token is revoked in HA at the address chosen by
 * 4.5 and deleted locally with its key. If no address answers, it is deleted anyway and expires in HA later.
 */
class InstanceSignOut @Inject constructor(
    private val servers: ServerDao,
    private val endpoints: EndpointSelector,
    private val sessions: InstanceSessionFactory,
    private val tokens: TokenStore,
    private val demo: DemoWorld,
) {
    /**
     * Revokes and deletes the token of [serverId]. The demo (concept 20.4) calls no server: its placeholder token is
     * deleted and its saved state with it.
     */
    suspend fun signOut(serverId: String) {
        if (DemoInstance.isDemo(serverId)) {
            tokens.delete(serverId)
            demo.discard()
            return
        }
        val url = address(serverId)
        if (url == null) tokens.delete(serverId) else sessions.create(serverId, url).signOut()
    }

    /** The address to revoke the token at (concept 4.5), or null if none answers. */
    private suspend fun address(serverId: String): HttpUrl? {
        val server = servers.get(serverId) ?: return null
        return try {
            endpoints.select(server)
        } catch (_: HaacException) {
            null
        }
    }
}
