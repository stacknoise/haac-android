package com.stacknoise.haac.feature.onboarding.data

import android.database.SQLException
import com.stacknoise.haac.core.common.ui.theme.InstanceAccents
import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.server.ServerEntity
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.network.bridge.BridgeInfo
import com.stacknoise.haac.core.network.endpoint.AddressSlot
import com.stacknoise.haac.core.network.endpoint.InstanceAddresses
import com.stacknoise.haac.core.network.endpoint.addresses
import com.stacknoise.haac.core.network.endpoint.withAddresses
import com.stacknoise.haac.core.security.token.TokenStore
import com.stacknoise.haac.feature.onboarding.domain.SignInTarget
import java.util.UUID
import javax.inject.Inject
import okhttp3.HttpUrl

/** Stores a signed-in instance: `server` row, encrypted refresh token and `activeServerId` (concept 4.4, 5.3, 12). */
class InstanceRegistry @Inject constructor(
    private val servers: ServerDao,
    private val tokens: TokenStore,
    private val active: ActiveInstanceStore,
    private val errors: ErrorFactory,
) {
    /** The stored instance [id], or null. */
    suspend fun find(id: String): ServerEntity? = database { servers.get(id) }

    /** The stored instance of HA installation [instanceId] and [username], or null (concept 4.5). */
    suspend fun findSame(instanceId: String?, username: String): ServerEntity? =
        instanceId?.let { database { servers.findByInstance(it, username) } }

    /**
     * Creates the instance (or updates the stored one of [target]) and makes it active; returns its id.
     * A new instance whose row cannot be written loses its token again, so nothing half-saved remains.
     */
    suspend fun save(target: SignInTarget, username: String, bridge: BridgeInfo, refreshToken: String): String {
        val now = System.currentTimeMillis()
        val existing = target.serverId?.let { find(it) }
        val id = existing?.id ?: UUID.randomUUID().toString()
        tokens.save(id, refreshToken)
        try {
            database {
                if (existing == null) {
                    servers.insert(newServer(id, target, username, bridge, now))
                } else {
                    servers.update(
                        existing.copy(
                            instanceUuid = existing.instanceUuid ?: bridge.instanceId,
                            haUserName = username,
                            haVersion = bridge.haVersion,
                            bridgeApiVersion = bridge.apiVersion,
                            lastActiveAt = now,
                        ),
                    )
                }
            }
        } catch (e: HaacException) {
            if (existing == null) tokens.delete(id)
            throw e
        }
        active.setActive(id)
        return id
    }

    /**
     * Puts [url] into its slot of instance [id] and makes it active (concept 4.5). Saves [refreshToken]
     * only if the instance has none; returns true if it was saved.
     */
    suspend fun addAddress(id: String, url: HttpUrl, refreshToken: String): Boolean {
        val server = find(id) ?: return false
        val saveToken = !tokens.contains(id)
        if (saveToken) tokens.save(id, refreshToken)
        val addresses = server.addresses.with(AddressSlot.of(url), url)
        database { servers.update(server.withAddresses(addresses).copy(lastActiveAt = System.currentTimeMillis())) }
        active.setActive(id)
        return saveToken
    }

    /** Row of a new instance with the next accent colour and the addresses of concept 4.5. */
    private suspend fun newServer(
        id: String,
        target: SignInTarget,
        username: String,
        bridge: BridgeInfo,
        now: Long,
    ) = ServerEntity(
        id = id,
        instanceUuid = bridge.instanceId,
        displayName = target.displayName,
        accentColor = InstanceAccents.forIndex(servers.count()),
        haUserName = username,
        haVersion = bridge.haVersion,
        bridgeApiVersion = bridge.apiVersion,
        lastActiveAt = now,
    ).withAddresses(InstanceAddresses.afterSignIn(target.url, bridge.urls))

    /** Runs a database call and converts SQLite errors to HAAC-DB-001. */
    private suspend fun <T> database(block: suspend () -> T): T = try {
        block()
    } catch (e: SQLException) {
        throw errors.from(e)
    }
}
