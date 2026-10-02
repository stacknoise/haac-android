package com.stacknoise.haac.feature.onboarding.data

import com.stacknoise.haac.core.common.ui.theme.InstanceAccents
import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.server.ServerEntity
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.core.network.bridge.BridgeInfo
import com.stacknoise.haac.core.network.demo.DemoInstance
import com.stacknoise.haac.core.network.endpoint.AddressSlot
import com.stacknoise.haac.core.network.endpoint.InstanceAddresses
import com.stacknoise.haac.core.network.endpoint.addresses
import com.stacknoise.haac.core.network.endpoint.pinnedBy
import com.stacknoise.haac.core.network.endpoint.withAddresses
import com.stacknoise.haac.core.network.tls.PinRegistry
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
    private val pins: PinRegistry,
) {
    /** The stored instance [id], or null. */
    suspend fun find(id: String): ServerEntity? = errors.database { servers.get(id) }

    /** The stored instance of HA installation [instanceId] and [username], or null (concept 4.5). */
    suspend fun findSame(instanceId: String?, username: String): ServerEntity? =
        instanceId?.let { errors.database { servers.findByInstance(it, username) } }

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
            errors.database {
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
                        ).pinnedBy(pins),
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
     * Stores the demo instance (concept 20.4): the row with the fixed instance ID and the demo address, and the
     * placeholder token, which only makes the instance count as signed in. An existing demo is just made active.
     * Like [save], a row that cannot be written loses its token again.
     */
    suspend fun saveDemo(): String {
        val id = DemoInstance.SERVER_ID
        if (find(id) == null) {
            tokens.save(id, DemoInstance.PLACEHOLDER_TOKEN)
            try {
                errors.database { servers.insert(demoServer()) }
            } catch (e: HaacException) {
                tokens.delete(id)
                throw e
            }
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
        val updated = server.withAddresses(addresses).pinnedBy(pins).copy(lastActiveAt = System.currentTimeMillis())
        errors.database { servers.update(updated) }
        active.setActive(id)
        return saveToken
    }

    /** The row of the demo: its name and user "Demo", the fixed instance ID and the demo address as external one. */
    private suspend fun demoServer() = ServerEntity(
        id = DemoInstance.SERVER_ID,
        instanceUuid = DemoInstance.INSTANCE_ID,
        externalUrl = DemoInstance.ADDRESS,
        displayName = DemoInstance.DISPLAY_NAME,
        accentColor = InstanceAccents.forIndex(servers.count()),
        haUserName = DemoInstance.DISPLAY_NAME,
        haVersion = DemoInstance.info.haVersion,
        bridgeApiVersion = DemoInstance.info.apiVersion,
        lastActiveAt = System.currentTimeMillis(),
    )

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
    ).withAddresses(InstanceAddresses.afterSignIn(target.url, bridge.urls)).pinnedBy(pins)
}
