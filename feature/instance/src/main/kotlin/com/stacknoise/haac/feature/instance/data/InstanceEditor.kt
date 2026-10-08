package com.stacknoise.haac.feature.instance.data

import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.ErrorReporter
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.core.network.session.InstanceSignOut
import com.stacknoise.haac.feature.instance.domain.RemoveOutcome
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Renames, recolours and removes instances (concept 4.4); names and colours stay on the device. */
class InstanceEditor @Inject constructor(
    private val servers: ServerDao,
    private val active: ActiveInstanceStore,
    private val signOut: InstanceSignOut,
    private val switcher: InstanceSwitcher,
    private val errors: ErrorFactory,
    private val reporter: ErrorReporter,
) {
    /** Stores the local [name] and [accent] colour of instance [id]. */
    suspend fun setAppearance(id: String, name: String, accent: Long) {
        errors.database { servers.setAppearance(id, name.trim(), accent) }
    }

    /**
     * Removes instance [id]: the refresh token is revoked in HA if it is reachable, then token and key are deleted,
     * and the row with its cache, layout, assignments and aliases follows by cascade. Removing the active instance
     * makes the most recently used one active.
     */
    suspend fun remove(id: String): RemoveOutcome {
        val wasActive = errors.database { active.activeServerId.first() } == id
        if (!signOut.signOut(id)) reporter.report(AuthException(ErrorCode.AUTH_REVOKE_FAILED), null)
        errors.database { servers.delete(id) }
        return if (wasActive) next() else RemoveOutcome.Kept
    }

    /** Activates the most recently used instance, or clears the active one when none is left. */
    private suspend fun next(): RemoveOutcome {
        val server = errors.database { servers.mostRecent() }
        if (server == null) errors.database { active.setActive(null) } else switcher.activate(server.id)
        return server?.let { RemoveOutcome.Next(it.id, switcher.step(it.id)) } ?: RemoveOutcome.NoneLeft
    }
}
