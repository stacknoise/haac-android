package com.stacknoise.haac.feature.instance.data

import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.core.security.token.TokenProtection
import com.stacknoise.haac.core.security.token.TokenStore
import com.stacknoise.haac.core.security.token.UnlockedTokens
import com.stacknoise.haac.feature.instance.domain.SwitchStep
import javax.inject.Inject

/** Makes another instance the active one (concept 4.4): only an instance whose token is usable is activated. */
class InstanceSwitcher @Inject constructor(
    private val active: ActiveInstanceStore,
    private val servers: ServerDao,
    private val tokens: TokenStore,
    private val unlocked: UnlockedTokens,
    private val errors: ErrorFactory,
) {
    /** Activates [id] if its token is usable now; otherwise tells what is missing and changes nothing. */
    suspend fun switchTo(id: String): SwitchStep {
        val step = step(id)
        if (step == SwitchStep.READY) activate(id)
        return step
    }

    /** What [id] needs before it can be used: nothing, the fingerprint unlock or the HA login. */
    suspend fun step(id: String): SwitchStep = when (tokens.protection(id)) {
        null -> SwitchStep.SIGN_IN
        TokenProtection.DeviceKey -> SwitchStep.READY
        is TokenProtection.Fingerprint -> if (unlocked.get(id) != null) SwitchStep.READY else SwitchStep.UNLOCK
    }

    /** Makes [id] the active instance and remembers when it was used. */
    suspend fun activate(id: String) {
        errors.database {
            servers.touch(id, System.currentTimeMillis())
            active.setActive(id)
        }
    }
}
