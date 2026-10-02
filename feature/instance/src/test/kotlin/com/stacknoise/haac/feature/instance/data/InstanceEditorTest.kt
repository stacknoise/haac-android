package com.stacknoise.haac.feature.instance.data

import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.server.ServerEntity
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.network.session.InstanceSignOut
import com.stacknoise.haac.feature.instance.domain.RemoveOutcome
import com.stacknoise.haac.feature.instance.domain.SwitchStep
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InstanceEditorTest {
    private val active = mockk<ActiveInstanceStore>(relaxed = true)
    private val servers = mockk<ServerDao>(relaxed = true)
    private val signOut = mockk<InstanceSignOut>(relaxed = true)
    private val switcher = mockk<InstanceSwitcher>(relaxed = true)
    private val editor = InstanceEditor(servers, active, signOut, switcher, DefaultErrorFactory())

    private fun server(id: String) = ServerEntity(
        id = id,
        externalUrl = "https://$id.example.com/",
        displayName = id,
        accentColor = 0xFF4DFF7A,
        haUserName = "anna",
        haVersion = "2026.9.0",
        bridgeApiVersion = 1,
        lastActiveAt = 0,
    )

    @Test
    fun `the name is trimmed before it is stored`() = runTest {
        editor.setAppearance("a", "  Cabin ", 0xFF4DC3FF)
        coVerify { servers.setAppearance("a", "Cabin", 0xFF4DC3FF) }
    }

    @Test
    fun `an inactive instance is signed out and deleted, nothing else changes`() = runTest {
        coEvery { active.activeServerId } returns flowOf("a")
        assertEquals(RemoveOutcome.Kept, editor.remove("b"))
        coVerifyOrder {
            signOut.signOut("b")
            servers.delete("b")
        }
        coVerify(exactly = 0) { active.setActive(any()) }
    }

    @Test
    fun `removing the active instance activates the most recent one`() = runTest {
        coEvery { active.activeServerId } returns flowOf("a")
        coEvery { servers.mostRecent() } returns server("b")
        coEvery { switcher.step("b") } returns SwitchStep.UNLOCK
        assertEquals(RemoveOutcome.Next("b", SwitchStep.UNLOCK), editor.remove("a"))
        coVerify { switcher.activate("b") }
    }

    @Test
    fun `removing the last instance clears the active one`() = runTest {
        coEvery { active.activeServerId } returns flowOf("a")
        coEvery { servers.mostRecent() } returns null
        assertEquals(RemoveOutcome.NoneLeft, editor.remove("a"))
        coVerify { active.setActive(null) }
    }

    @Test
    fun `the demo is removed like any instance and the last one leads back to the server screen`() = runTest {
        coEvery { active.activeServerId } returns flowOf("demo")
        coEvery { servers.mostRecent() } returns null
        assertEquals(RemoveOutcome.NoneLeft, editor.remove("demo"))
        coVerifyOrder {
            signOut.signOut("demo")
            servers.delete("demo")
            active.setActive(null)
        }
    }
}
