package com.stacknoise.haac.feature.entities.disable.domain

import com.stacknoise.haac.core.database.entity.EntityStatus
import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.database.schedule.ScheduleEntity
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DisabledRulesTest {
    private fun row(
        id: String,
        disabled: Boolean = false,
        at: Long? = null,
        status: EntityStatus = EntityStatus.ACTIVE,
    ) =
        ExposedEntity("s1", id, id.substringBefore('.'), id, status = status, disabled = disabled, disabledAt = at)

    private fun schedule(
        name: String,
        entities: String,
        enabled: Boolean = true,
        own: Boolean = true,
        reason: String? = null,
    ) = ScheduleEntity(
        serverId = "s1",
        scheduleId = name,
        owner = "u",
        own = own,
        name = name,
        enabled = enabled,
        whenType = "time",
        days = 1,
        action = "turn_on",
        entityIds = entities,
        createdAt = 0,
        updatedAt = "t",
        syncedAt = 0,
        offReason = reason,
    )

    @Test
    fun `the list of disabled_changed is a plain array or an object with disabled`() {
        assertEquals(listOf("a", "b"), disabledList(Json.parseToJsonElement("""["a","b"]""")))
        assertEquals(listOf("a"), disabledList(Json.parseToJsonElement("""{"disabled":["a"]}""")))
        assertNull(disabledList(Json.parseToJsonElement("""{"other":1}""")))
        assertNull(disabledList(null))
    }

    @Test
    fun `a withdrawn entity that was deactivated stays deactivated after the list`() {
        val cached = listOf(row("switch.gone", disabled = true, status = EntityStatus.WITHDRAWN), row("switch.a"))
        assertEquals(
            listOf("switch.b", "switch.gone"),
            disabledAfterList(listOf("switch.b"), setOf("switch.a", "switch.b"), cached),
        )
    }

    @Test
    fun `an entity the list no longer marks is not deactivated any more`() {
        val cached = listOf(row("switch.a", disabled = true, at = 5))
        assertEquals(emptyList<String>(), disabledAfterList(emptyList(), setOf("switch.a"), cached))
    }

    @Test
    fun `the time of a deactivation that was already cached is taken over`() {
        val cached = listOf(row("switch.a", disabled = true, at = 5), row("switch.b", disabled = true, at = 7))
        val listed = listOf(row("switch.a", disabled = true), row("switch.b"), row("switch.c", disabled = true))
        val rows = withDisabledTimes(listed, cached).associateBy { it.entityId }
        assertEquals(5L, rows.getValue("switch.a").disabledAt)
        assertNull(rows.getValue("switch.b").disabledAt)
        assertNull(rows.getValue("switch.c").disabledAt)
    }

    @Test
    fun `deactivating names the own enabled schedules with the entity`() {
        val schedules = listOf(
            schedule("Morning", """["switch.a","switch.b"]"""),
            schedule("Off already", """["switch.a"]""", enabled = false),
            schedule("Other entity", """["switch.b"]"""),
            schedule("Foreign", """["switch.a"]""", own = false),
        )
        assertEquals(DisablePreview(off = listOf("Morning")), disablePreview("switch.a", schedules, emptySet()))
    }

    @Test
    fun `activating names the schedules that come on again unless another entity is still deactivated`() {
        val schedules = listOf(
            schedule("Again", """["switch.a"]""", enabled = false, reason = OffReasonEntityDisabled),
            schedule("Still blocked", """["switch.a","switch.b"]""", enabled = false, reason = OffReasonEntityDisabled),
            schedule("By the user", """["switch.a"]""", enabled = false),
        )
        val preview = disablePreview("switch.a", schedules, setOf("switch.a", "switch.b"))
        assertEquals(listOf("Again"), preview.on)
        assertEquals(emptyList<String>(), preview.off)
    }

    @Test
    fun `a damaged entity list names no entity`() {
        assertEquals(DisablePreview(), disablePreview("switch.a", listOf(schedule("X", "not json")), emptySet()))
    }
}
