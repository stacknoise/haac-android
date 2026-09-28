package com.stacknoise.haac.app.lock

import com.stacknoise.haac.core.database.settings.SecuritySettings
import com.stacknoise.haac.core.security.token.UnlockedTokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AppLockTest {
    private var now = 0L
    private val timeout = MutableStateFlow(5)
    private val unlocked = UnlockedTokens().apply { put("a", "token") }

    private val lock = AppLock(
        settings = object : SecuritySettings {
            override val unlockWindowMinutes: Flow<Int> = MutableStateFlow(0)
            override val lockTimeoutMinutes: Flow<Int> = timeout

            override suspend fun setUnlockWindowMinutes(minutes: Int) = error("not used")

            override suspend fun setLockTimeoutMinutes(minutes: Int) = error("not used")
        },
        unlocked = unlocked,
        clock = { now },
    )

    @Test
    fun `a short trip to the background keeps the app unlocked`() = runTest {
        lock.onBackground()
        now = 4 * 60_000L
        lock.onForeground()
        assertEquals(0, lock.locks.value)
        assertEquals("token", unlocked.get("a"))
    }

    @Test
    fun `after the timeout the tokens are dropped and the start routing runs again`() = runTest {
        lock.onBackground()
        now = 5 * 60_000L
        lock.onForeground()
        assertEquals(1, lock.locks.value)
        assertNull(unlocked.get("a"))
    }

    @Test
    fun `timeout 0 locks on every return and a start without background does not`() = runTest {
        lock.onForeground()
        assertEquals(0, lock.locks.value)
        timeout.value = 0
        lock.onBackground()
        lock.onForeground()
        assertEquals(1, lock.locks.value)
    }
}
