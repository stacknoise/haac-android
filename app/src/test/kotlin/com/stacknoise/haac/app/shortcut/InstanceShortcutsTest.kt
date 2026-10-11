package com.stacknoise.haac.app.shortcut

import app.cash.turbine.test
import com.stacknoise.haac.core.database.server.ServerEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InstanceShortcutsTest {
    private val home = ServerEntity(
        id = "s1",
        displayName = "Home",
        accentColor = 0L,
        haUserName = "anna",
        haVersion = "2026.1",
        bridgeApiVersion = 1,
        lastActiveAt = 0L,
    )
    private val table = MutableStateFlow(listOf(home))

    @Test
    fun `a sync or a visit changes no label, a new or renamed instance does`() = runTest {
        table.instanceLabels().test {
            assertEquals(listOf(InstanceLabel("s1", "Home")), awaitItem())
            table.value = listOf(home.copy(lastSyncAt = 5L, lastActiveAt = 7L, haVersion = "2026.2"))
            expectNoEvents()
            table.value = listOf(home.copy(displayName = "Flat"))
            assertEquals(listOf(InstanceLabel("s1", "Flat")), awaitItem())
            table.value = listOf(home.copy(displayName = "Flat"), home.copy(id = "s2", displayName = "Cabin"))
            assertEquals(2, awaitItem().size)
        }
    }
}
