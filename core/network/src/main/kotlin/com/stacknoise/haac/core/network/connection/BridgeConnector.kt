package com.stacknoise.haac.core.network.connection

import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.server.ServerEntity
import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.error.UnexpectedException
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.core.network.bridge.BridgeInfo
import com.stacknoise.haac.core.network.bridge.BridgeInfoClient
import com.stacknoise.haac.core.network.endpoint.EndpointSelector
import com.stacknoise.haac.core.network.endpoint.requireSameInstance
import com.stacknoise.haac.core.network.session.InstanceSession
import com.stacknoise.haac.core.network.session.InstanceSessionFactory
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import okhttp3.HttpUrl

/** Opens the live connection of an instance (concept 4.5, 9.1 step 1). */
interface BridgeConnector {
    /** The address instance [serverId] should use now (concept 4.5). */
    suspend fun select(serverId: String): HttpUrl

    /** Connects instance [serverId] at [url]; the connection runs in [scope] until it ends. */
    suspend fun connect(serverId: String, url: HttpUrl, scope: CoroutineScope): BridgeConnection
}

/**
 * Access token, WebSocket auth and `haac_bridge/info` with the `instance_id` check (HAAC-NET-008, concept
 * 4.5). A rejected access token is refreshed once (concept 5.2). After connecting, the instance ID (if the
 * instance has none yet), the HA version and the bridge API version are saved.
 */
class DefaultBridgeConnector @Inject constructor(
    private val servers: ServerDao,
    private val endpoints: EndpointSelector,
    private val sessions: InstanceSessionFactory,
    private val bridge: BridgeInfoClient,
    private val errors: ErrorFactory,
) : BridgeConnector {
    /** Runs the selection rules of [EndpointSelector] for the stored instance. */
    override suspend fun select(serverId: String): HttpUrl = endpoints.select(load(serverId))

    /** Handshake at [url], retried once with a fresh access token after `auth_invalid`. */
    override suspend fun connect(serverId: String, url: HttpUrl, scope: CoroutineScope): BridgeConnection {
        val server = load(serverId)
        val session = sessions.create(serverId, url)
        val connection = try {
            open(server, session, scope)
        } catch (_: AuthException) {
            session.invalidate()
            open(server, session, scope)
        }
        remember(server, connection.info)
        return connection
    }

    /** One handshake; a different `instance_id` closes the socket before anything else is sent. */
    private suspend fun open(server: ServerEntity, session: InstanceSession, scope: CoroutineScope): BridgeConnection {
        val handshake = bridge.open(session.baseUrl, session.accessToken())
        try {
            requireSameInstance(server.instanceUuid, handshake.info.instanceId)
        } catch (e: NetworkException) {
            handshake.socket.close()
            throw e
        }
        return BridgeConnection(session.baseUrl, handshake.info, handshake.socket, handshake.messages, errors, scope)
    }

    /** Saves what the handshake reported, if it changed; schema-1 instances get their ID here (4.5). */
    private suspend fun remember(server: ServerEntity, info: BridgeInfo) {
        val updated = server.copy(
            instanceUuid = server.instanceUuid ?: info.instanceId,
            haVersion = info.haVersion,
            bridgeApiVersion = info.apiVersion,
        )
        if (updated != server) errors.database { servers.update(updated) }
    }

    /** The stored instance [serverId]; it exists while it is active. */
    private suspend fun load(serverId: String): ServerEntity = errors.database { servers.get(serverId) }
        ?: throw UnexpectedException()
}
