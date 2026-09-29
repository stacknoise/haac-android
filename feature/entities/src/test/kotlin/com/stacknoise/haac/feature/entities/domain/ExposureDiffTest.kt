package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.entity.EntityStatus
import com.stacknoise.haac.core.database.entity.ExposedEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExposureDiffTest {
    private fun row(id: String, status: EntityStatus = EntityStatus.ACTIVE, state: String? = null) =
        ExposedEntity("s1", id, id.substringBefore('.'), id, status = status, lastState = state)

    @Test
    fun `new, withdrawn and restored entities are reported`() {
        val cached = listOf(
            row("switch.kept"),
            row("sensor.gone", state = """{"state":"21"}"""),
            row("switch.back", EntityStatus.WITHDRAWN),
            row("switch.still_gone", EntityStatus.WITHDRAWN),
        )
        val listed = listOf(row("switch.kept"), row("switch.back"), row("climate.new"))

        val change = ExposureDiff.compute(cached, listed, now = 42)

        assertEquals(SyncResult(listOf("climate.new"), listOf("sensor.gone"), listOf("switch.back")), change.result)
        val gone = change.rows.single { it.entityId == "sensor.gone" }
        assertEquals(EntityStatus.WITHDRAWN, gone.status)
        assertEquals(42L, gone.withdrawnAt)
        assertEquals("""{"state":"21"}""", gone.lastState)
        assertEquals(EntityStatus.ACTIVE, change.rows.single { it.entityId == "switch.back" }.status)
        assertEquals(4, change.rows.size)
    }

    @Test
    fun `first sync adds everything`() {
        val change = ExposureDiff.compute(emptyList(), listOf(row("switch.b"), row("switch.a")), now = 1)
        assertEquals(SyncResult(added = listOf("switch.a", "switch.b")), change.result)
    }
}
