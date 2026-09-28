package com.stacknoise.haac.feature.onboarding.data

import android.database.SQLException
import com.stacknoise.haac.core.common.ui.theme.InstanceAccents
import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.server.ServerEntity
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.network.bridge.BridgeInfo
import com.stacknoise.haac.core.security.token.TokenStore
import com.stacknoise.haac.feature.onboarding.domain.SignInTarget
import java.util.UUID
import javax.inject.Inject

/** Stores a signed-in instance: `server` row, encrypted refresh token and `activeServerId` (concept 4.4, 5.3, 12). */
class InstanceRegistry @Inject constructor(
    private val servers: ServerDao,
    private val tokens: TokenStore,
    private val active: ActiveInstanceStore,
    private val errors: ErrorFactory,
) {
    /** The stored instance [id], or null. */
    suspend fun find(id: String): ServerEntity? = database { servers.get(id) }

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

    /** Row of a new instance with the next accent colour. */
    private suspend fun newServer(
        id: String,
        target: SignInTarget,
        username: String,
        bridge: BridgeInfo,
        now: Long,
    ) = ServerEntity(
        id = id,
        baseUrl = target.url.toString(),
        displayName = target.displayName,
        accentColor = InstanceAccents.forIndex(servers.count()),
        haUserName = username,
        haVersion = bridge.haVersion,
        bridgeApiVersion = bridge.apiVersion,
        lastActiveAt = now,
    )

    /** Runs a database call and converts SQLite errors to HAAC-DB-001. */
    private suspend fun <T> database(block: suspend () -> T): T = try {
        block()
    } catch (e: SQLException) {
        throw errors.from(e)
    }
}
