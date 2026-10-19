package com.stacknoise.haac.feature.entities.guard

import com.stacknoise.haac.core.database.guard.EntityGuard
import com.stacknoise.haac.core.database.guard.EntityGuardDao
import com.stacknoise.haac.core.error.DefaultErrorFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** [SwitchGuards] against an in-memory table: separate per instance, a row only while the guard is on (concept 8.5). */
class SwitchGuardsTest {
    private val table = MutableStateFlow<List<EntityGuard>>(emptyList())
    private val dao = object : EntityGuardDao {
        override fun observe(serverId: String): Flow<List<EntityGuard>> =
            table.map { rows -> rows.filter { it.serverId == serverId } }

        override suspend fun set(guard: EntityGuard) {
            clear(guard.serverId, guard.entityId)
            table.value += guard
        }

        override suspend fun clear(serverId: String, entityId: String) {
            table.value = table.value.filterNot { it.serverId == serverId && it.entityId == entityId }
        }
    }
    private val guards = SwitchGuards(dao, DefaultErrorFactory())

    @Test
    fun `set replaces the direction and null removes the row`() = runTest {
        guards.set("s1", "switch.lamp", SwitchGuard(GuardDirection.ON))
        guards.set("s1", "switch.lamp", SwitchGuard(GuardDirection.OFF))
        assertEquals(mapOf("switch.lamp" to SwitchGuard(GuardDirection.OFF)), guards.observe("s1").first())
        guards.set("s1", "switch.lamp", null)
        assertEquals(emptyMap<String, SwitchGuard>(), guards.observe("s1").first())
        assertEquals(0, table.value.size)
    }

    @Test
    fun `guards of other instances stay apart`() = runTest {
        guards.set("s1", "switch.lamp", SwitchGuard())
        assertEquals(emptyMap<String, SwitchGuard>(), guards.observe("s2").first())
    }
}
