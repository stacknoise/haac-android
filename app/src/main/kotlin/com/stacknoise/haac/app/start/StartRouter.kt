package com.stacknoise.haac.app.start

import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.security.token.TokenStore
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Where the app opens (concept 4.1). */
sealed interface StartRoute {
    /** No instance yet: server entry. */
    data object Onboarding : StartRoute

    /** The last active instance [serverId] has no refresh token: HA login for it. */
    data class SignIn(val serverId: String) : StartRoute

    /** Token stored: the app opens directly (biometric unlock follows with concept 5.4). */
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
        val server = activeId?.let { servers.get(it) } ?: servers.mostRecent()
        return when {
            server == null -> StartRoute.Onboarding
            // TODO(concept 5.4): biometric prompt when fingerprint unlock is enabled for the instance (part 3).
            tokens.contains(server.id) -> StartRoute.Main(server.id)
            else -> StartRoute.SignIn(server.id)
        }
    }
}
