package com.stacknoise.haac.app.start

import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.security.token.TokenProtection
import com.stacknoise.haac.core.security.token.TokenStore
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Where the app opens (concept 4.1). */
sealed interface StartRoute {
    /** No instance yet: server entry. */
    data object Onboarding : StartRoute

    /** The last active instance [serverId] has no refresh token: HA login for it. */
    data class SignIn(val serverId: String) : StartRoute

    /** The token of [serverId] is fingerprint-protected: unlock screen (concept 5.4). */
    data class Unlock(val serverId: String) : StartRoute

    /** Token stored with the device-bound key: the app opens directly. */
    data class Main(val serverId: String) : StartRoute
}

/** Decides the start screen from the stored instances and tokens (concept 4.1). */
class StartRouter @Inject constructor(
    private val active: ActiveInstanceStore,
    private val servers: ServerDao,
    private val tokens: TokenStore,
) {
    /** The last active instance, else the most recently used one; see [StartRoute]. */
    suspend fun route(): StartRoute {
        val activeId = active.activeServerId.first()
        val server = activeId?.let { servers.get(it) } ?: servers.mostRecent() ?: return StartRoute.Onboarding
        return when (tokens.protection(server.id)) {
            null -> StartRoute.SignIn(server.id)
            is TokenProtection.Fingerprint -> StartRoute.Unlock(server.id)
            TokenProtection.DeviceKey -> StartRoute.Main(server.id)
        }
    }
}
