package com.stacknoise.haac.feature.schedules.data

import com.stacknoise.haac.feature.schedules.domain.RepeatFreq
import com.stacknoise.haac.feature.schedules.domain.RepeatRule
import com.stacknoise.haac.feature.schedules.domain.ScheduleAction
import com.stacknoise.haac.feature.schedules.domain.ScheduleDraft
import com.stacknoise.haac.feature.schedules.domain.WhenType
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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

    @Test
    fun `a repeating rule is sent with its interval and dates only if the bridge has the feature`() {
        val monthly = draft.copy(repeat = RepeatRule(RepeatFreq.MONTHLY, 2, LocalDate.of(2026, 10, 15), End))
        val repeat = """{"freq":"monthly","interval":2,"start_date":"2026-10-15","end_date":"2026-12-31"}"""
        val sent = ScheduleFields.create(monthly, ScheduleFeatures(repeat = true))
        assertEquals(json(repeat), sent["repeat"])
        assertEquals(json("""{"type":"time","time":"06:05"}"""), sent["when"])
        assertNull(ScheduleFields.create(monthly)["repeat"])
    }

    @Test
    fun `a one-off schedule sends its date and neither interval nor end`() {
        val once = draft.copy(repeat = RepeatRule(RepeatFreq.ONCE, 1, LocalDate.of(2026, 10, 26), End))
        val sent = ScheduleFields.create(once, ScheduleFeatures(repeat = true))
        assertEquals(json("""{"freq":"once","start_date":"2026-10-26"}"""), sent["repeat"])
        assertEquals(sent["repeat"]!!.jsonObject.keys, setOf("freq", "start_date"))
    }

    @Test
    fun `notify is sent when it is on, or changed, and only with the feature`() {
        val features = ScheduleFeatures(repeat = true, runs = true)
        assertEquals(JsonPrimitive(true), ScheduleFields.create(draft.copy(notify = true), features)["notify"])
        assertNull(ScheduleFields.create(draft, features)["notify"])
        assertNull(ScheduleFields.create(draft.copy(notify = true))["notify"])
        val off = ScheduleFields.changes(draft.copy(notify = true), draft, true, features)
        assertEquals(json("""{"notify":false}"""), off)
        assertEquals(JsonObject(emptyMap()), ScheduleFields.changes(draft.copy(notify = true), draft, true))
    }

    @Test
    fun `a changed rule is sent whole, the weekdays follow when the rule becomes weekly`() {
        val daily = draft.copy(days = emptySet(), repeat = RepeatRule(RepeatFreq.DAILY, 1, Start))
        val features = ScheduleFeatures(repeat = true)
        val weekly = daily.copy(days = setOf(0), repeat = RepeatRule(RepeatFreq.WEEKLY, 2, Start))
        val sent = ScheduleFields.changes(daily, weekly, true, features)
        assertEquals(json("""{"freq":"weekly","interval":2,"start_date":"2026-10-19"}"""), sent["repeat"])
        assertEquals(json("""{"type":"time","days":[0],"time":"06:05"}"""), sent["when"])
        val yearly = daily.copy(repeat = RepeatRule(RepeatFreq.YEARLY, 1, Start))
        assertEquals(setOf("repeat"), ScheduleFields.changes(daily, yearly, true, features).keys)
    }

    @Test
    fun `the preview asks for the trigger and the rule of the draft`() {
        val sent = ScheduleFields.preview(draft.copy(repeat = RepeatRule(RepeatFreq.DAILY, 3, Start)))
        assertEquals(setOf("when", "repeat"), sent.keys)
        assertEquals(json("""{"freq":"daily","interval":3,"start_date":"2026-10-19"}"""), sent["repeat"])
    }

    private companion object {
        val Start: LocalDate = LocalDate.of(2026, 10, 19)
        val End: LocalDate = LocalDate.of(2026, 12, 31)
    }
}
