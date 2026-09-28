package com.stacknoise.haac.app.lock

import android.os.SystemClock
import com.stacknoise.haac.core.database.settings.SecuritySettings
import com.stacknoise.haac.core.security.token.UnlockedTokens
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

/**
 * App lock (concept 5.5): after the app was in the background longer than the lock timeout, the unlocked
 * tokens are dropped and the start sequence runs again. Fingerprint-protected instances then show the unlock
 * screen; the others open directly (4.1).
 */
@Singleton
class AppLock internal constructor(
    private val settings: SecuritySettings,
    private val unlocked: UnlockedTokens,
    private val clock: () -> Long,
) {
    /** Uses the monotonic clock, so changing the device time cannot skip the lock. */
    @Inject
    constructor(settings: SecuritySettings, unlocked: UnlockedTokens) :
        this(settings, unlocked, SystemClock::elapsedRealtime)

    private var backgroundSince: Long? = null
    private val _locks = MutableStateFlow(0)

    /** Number of locks since the process started; every increase restarts the start routing. */
    val locks: StateFlow<Int> = _locks.asStateFlow()

    /** The app went to the background. */
    fun onBackground() {
        backgroundSince = clock()
    }

    /** The app is visible again; locks if the timeout has passed. */
    suspend fun onForeground() {
        val since = backgroundSince ?: return
        backgroundSince = null
        val timeoutMs = settings.lockTimeoutMinutes.first() * MS_PER_MINUTE
        if (clock() - since >= timeoutMs) lock()
    }

    /** Drops all unlocked tokens and asks for the start routing again. */
    fun lock() {
        unlocked.clear()
        _locks.update { it + 1 }
    }

    /** Time conversion. */
    private companion object {
        const val MS_PER_MINUTE = 60_000L
    }
}
