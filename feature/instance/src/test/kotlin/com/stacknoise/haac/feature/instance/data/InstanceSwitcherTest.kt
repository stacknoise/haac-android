package com.stacknoise.haac.feature.instance.data

import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.security.token.TokenProtection
import com.stacknoise.haac.core.security.token.TokenStore
import com.stacknoise.haac.core.security.token.UnlockedTokens
import com.stacknoise.haac.feature.instance.domain.SwitchStep
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InstanceSwitcherTest {
    private val active = mockk<ActiveInstanceStore>(relaxed = true)
    private val servers = mockk<ServerDao>(relaxed = true)
    private val tokens = mockk<TokenStore>()
    private val unlocked = UnlockedTokens()
    private val switcher = InstanceSwitcher(active, servers, tokens, unlocked, DefaultErrorFactory())

    /** Asserts that the switch to `b` ends with [step] and did (or did not) activate `b`. */
    private suspend fun assertSwitch(step: SwitchStep, activated: Boolean) {
        assertEquals(step, switcher.switchTo("b"))
        coVerify(exactly = if (activated) 1 else 0) { active.setActive("b") }
    }

    @Test
    fun `an instance with a device-key token is activated`() = runTest {
        coEvery { tokens.protection("b") } returns TokenProtection.DeviceKey
        assertSwitch(SwitchStep.READY, activated = true)
        coVerify { servers.touch("b", any()) }
    }

    @Test
    fun `a fingerprint token that is still unlocked is activated without a prompt`() = runTest {
        coEvery { tokens.protection("b") } returns TokenProtection.Fingerprint(1, 0)
        unlocked.put("b", "token")
        assertSwitch(SwitchStep.READY, activated = true)
    }

    @Test
    fun `a locked fingerprint token asks for the unlock and stays inactive`() = runTest {
        coEvery { tokens.protection("b") } returns TokenProtection.Fingerprint(1, 0)
        assertSwitch(SwitchStep.UNLOCK, activated = false)
    }

    @Test
    fun `an instance without a token asks for the login and stays inactive`() = runTest {
        coEvery { tokens.protection("b") } returns null
        assertSwitch(SwitchStep.SIGN_IN, activated = false)
    }
}
