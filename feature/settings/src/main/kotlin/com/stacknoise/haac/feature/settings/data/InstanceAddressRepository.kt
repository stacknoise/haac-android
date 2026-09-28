package com.stacknoise.haac.feature.settings.data

import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.server.ServerEntity
import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.error.UnexpectedException
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.core.network.bridge.BridgeInfo
import com.stacknoise.haac.core.network.bridge.BridgeInfoClient
import com.stacknoise.haac.core.network.endpoint.AddressSlot
import com.stacknoise.haac.core.network.endpoint.EndpointProbe
import com.stacknoise.haac.core.network.endpoint.EndpointSelector
import com.stacknoise.haac.core.network.endpoint.InstanceAddresses
import com.stacknoise.haac.core.network.endpoint.addresses
import com.stacknoise.haac.core.network.endpoint.requireSameInstance
import com.stacknoise.haac.core.network.endpoint.withAddresses
import com.stacknoise.haac.core.network.session.InstanceSessionFactory
import javax.inject.Inject
import okhttp3.HttpUrl

/** *Settings → Instance → Addresses* (concept 4.5): change, remove, take over from HA and the always option. */
class InstanceAddressRepository @Inject constructor(
    private val servers: ServerDao,
    private val endpoints: EndpointSelector,
    private val sessions: InstanceSessionFactory,
    private val bridge: BridgeInfoClient,
    private val probe: EndpointProbe,
    private val errors: ErrorFactory,
) {
    /** Turns *Always use the internal address* of instance [serverId] on or off. */
    suspend fun setAlwaysUseInternal(serverId: String, enabled: Boolean) {
        save(load(serverId).copy(alwaysUseInternal = enabled))
    }

    /** Removes the address in [slot]; the last address of an instance stays. */
    suspend fun remove(serverId: String, slot: AddressSlot) {
        val server = load(serverId)
        val left = server.addresses.with(slot, null)
        if (left.internal != null || left.external != null) save(server.withAddresses(left))
    }

    /**
     * Stores [url] in [slot] once HA answers there with the instance's own ID (HAAC-NET-008 otherwise).
     * The access token comes from the working address if there is one, so a foreign server never sees
     * the refresh token.
     */
    suspend fun change(serverId: String, slot: AddressSlot, url: HttpUrl) {
        val server = load(serverId)
        probe.check(url)
        val tokenUrl = try {
            endpoints.select(server)
        } catch (_: HaacException) {
            url
        }
        val info = infoAt(server, url, tokenUrl)
        save(server.withAddresses(server.addresses.with(slot, url)).withInstanceId(info))
    }

    /** Takes the internal and external address from HA; a slot HA leaves empty keeps its address. */
    suspend fun useAddressesFromHa(serverId: String) {
        val server = load(serverId)
        val url = endpoints.select(server)
        val info = infoAt(server, url, url)
        val fromHa = InstanceAddresses.fromHa(info.urls)
        val current = server.addresses
        val merged = InstanceAddresses(fromHa.internal ?: current.internal, fromHa.external ?: current.external)
        save(server.withAddresses(merged).withInstanceId(info))
    }

    /** `haac_bridge/info` at [url] with an access token refreshed at [tokenUrl]; checks the instance ID. */
    private suspend fun infoAt(server: ServerEntity, url: HttpUrl, tokenUrl: HttpUrl): BridgeInfo {
        val token = sessions.create(server.id, tokenUrl).accessToken()
        val info = try {
            bridge.fetch(url, token)
        } catch (e: AuthException) {
            // A valid token rejected at [url] means another HA answers there.
            throw NetworkException(ErrorCode.NET_WRONG_SERVER, e)
        }
        requireSameInstance(server.instanceUuid, info.instanceId)
        return info
    }

    /** The instance's ID, taken from [info] if the instance has none yet (schema 1). */
    private fun ServerEntity.withInstanceId(info: BridgeInfo) = copy(instanceUuid = instanceUuid ?: info.instanceId)

    /** The stored instance [serverId]; it exists while its settings are shown. */
    private suspend fun load(serverId: String): ServerEntity = errors.database { servers.get(serverId) }
        ?: throw UnexpectedException()

    /** Writes [server]. */
    private suspend fun save(server: ServerEntity) = errors.database { servers.update(server) }
}
