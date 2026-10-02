package com.stacknoise.haac.feature.schedules.data

import com.stacknoise.haac.feature.schedules.domain.ScheduleAction
import com.stacknoise.haac.feature.schedules.domain.ScheduleDraft
import com.stacknoise.haac.feature.schedules.domain.WhenType
import java.time.LocalTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ScheduleFieldsTest {
    private val draft = ScheduleDraft(
        name = " Morning light ",
        time = LocalTime.of(6, 5),
        days = setOf(4, 0, 1),
        entityIds = listOf("switch.a", "switch.b"),
    )

    private fun json(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    @Test
    fun `a new schedule sends every field, trimmed, with sorted days and a zero padded time`() {
        val expected = json(
            """{"name":"Morning light","enabled":true,"when":{"type":"time","days":[0,1,4],"time":"06:05"},""" +
                """"action":"turn_on","entities":["switch.a","switch.b"]}""",
        )
        assertEquals(expected, ScheduleFields.create(draft))
    }

    @Test
    fun `a sun event sends the offset and no time`() {
        val sun = draft.copy(whenType = WhenType.SUNSET, offsetMin = -30, action = ScheduleAction.TURN_OFF)
        val sent = ScheduleFields.create(sun)
        assertEquals(json("""{"type":"sunset","days":[0,1,4],"offset_min":-30}"""), sent["when"])
        assertEquals(json("""{"a":"turn_off"}""")["a"], sent["action"])
    }

    @Test
    fun `an update sends only what changed`() {
        val changed = draft.copy(name = "Evening light", action = ScheduleAction.TOGGLE)
        val sent = ScheduleFields.changes(draft, changed, true)
        assertEquals(json("""{"name":"Evening light","action":"toggle"}"""), sent)
        assertEquals(JsonObject(emptyMap()), ScheduleFields.changes(draft, draft.copy(name = "Morning light"), true))
    }

    @Test
    fun `the entities of a foreign schedule are never sent`() {
        val changed = draft.copy(entityIds = listOf("switch.c"), time = LocalTime.of(7, 0))
        val expected = json("""{"when":{"type":"time","days":[0,1,4],"time":"07:00"}}""")
        assertEquals(expected, ScheduleFields.changes(draft, changed, false))
        assertEquals(setOf("when", "entities"), ScheduleFields.changes(draft, changed, true).keys)
    }
}
