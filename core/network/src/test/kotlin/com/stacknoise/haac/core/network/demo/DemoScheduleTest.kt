package com.stacknoise.haac.core.network.demo

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private const val Socket = "switch.demo_socket"
private const val Kitchen = "switch.demo_kitchen_light"
private const val EveryDay = "[0,1,2,3,4,5,6]"

class DemoScheduleTest {
    private val demo = DemoHarness()

    /** The fields of `schedules/create`: a schedule that runs [trigger] and does [action] with [entities]. */
    private fun body(
        name: String = "Evening",
        trigger: String = time("21:00"),
        action: String = "turn_off",
        entities: String = "\"$Socket\"",
    ) = """{"name":"$name","when":$trigger,"action":"$action","entities":[$entities]}"""

    private fun time(at: String, days: String = "[1]") = """{"type":"time","time":"$at","days":$days}"""

    private fun sun(type: String, offset: Int = 0, days: String = EveryDay) =
        """{"type":"$type","days":$days,"offset_min":$offset}"""

    private fun create(fields: String) = demo.request("haac_bridge/schedules/create", fields)

    private fun update(id: String?, version: String?, fields: String) = demo.request(
        "haac_bridge/schedules/update",
        """{"schedule_id":"$id","updated_at":"$version",$fields}""",
    )

    private fun text(obj: JsonObject, key: String) = obj[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content

    private fun fresh(name: String = "Evening", entity: String = Socket): JsonObject =
        demo.result(create(body(name, time("21:00", EveryDay), entities = "\"$entity\""))).jsonObject

    private fun schedules() = demo.list(demo.ask("haac_bridge/schedules/list"), "schedules").map { it.jsonObject }

    private fun strings(array: JsonObject, key: String) = array[key]!!.jsonArray.map { it.jsonPrimitive.content }

    @Test
    fun `the demo starts with one schedule Morning light owned by the demo user`() {
        val list = demo.ask("haac_bridge/schedules/list")
        assertEquals("own", text(list, "scope"))
        val morning = demo.list(list, "schedules").single().jsonObject
        assertEquals("Morning light", text(morning, "name"))
        assertEquals("true", text(morning, "own"))
        assertEquals("Demo", text(morning, "owner_name"))
        assertEquals("true", text(morning, "enabled"))
        assertEquals("07:00", text(morning.obj("when"), "time"))
        assertEquals(listOf("0", "1", "2", "3", "4"), strings(morning.obj("when"), "days"))
        assertEquals(listOf("switch.demo_living_room_light"), strings(morning, "entities"))
    }

    @Test
    fun `the next run of Morning light is the next weekday at 07-00 in the time zone of the phone`() {
        // The clock stands on Friday 12:00 Berlin time, so the next weekday morning is Monday.
        assertEquals("2026-10-05T07:00:00+02:00", text(schedules().single(), "next_run"))
    }

    @Test
    fun `sunrise and sunset runs use the fixed demo times plus the offset`() {
        val sunrise = demo.result(create(body(trigger = sun("sunrise", offset = -30)))).jsonObject
        assertEquals("2026-10-03T06:00:00+02:00", text(sunrise, "next_run"))
        val sunset = demo.result(create(body(trigger = sun("sunset")))).jsonObject
        assertEquals("2026-10-02T19:30:00+02:00", text(sunset, "next_run"))
        assertNull(text(sunset.obj("when"), "time"))
        assertEquals("0", text(sunset.obj("when"), "offset_min"))
    }

    @Test
    fun `a created schedule is owned by the caller and appears in the list`() {
        val created = fresh()
        assertEquals("true", text(created, "own"))
        assertNotNull(text(created, "id"))
        assertEquals(created["updated_at"], created["created_at"])
        assertEquals(2, schedules().size)
    }

    @Test
    fun `an invalid schedule is HAB-SCH-001`() {
        val invalid = listOf(
            body(name = ""),
            body(name = "x".repeat(61)),
            body(trigger = time("25:00")),
            body(trigger = time("21:00", days = "[]")),
            body(trigger = time("21:00", days = "[7]")),
            body(trigger = sun("sunrise", offset = 200)),
            body(action = "explode"),
            body(entities = ""),
            body(entities = "\"sensor.demo_energy\""),
            body(entities = "\"switch.nobody\""),
        )
        invalid.forEach { assertEquals("HAB-SCH-001", demo.code(create(it)), it) }
        assertEquals(1, schedules().size)
    }

    @Test
    fun `at most 50 schedules per user, then HAB-SCH-005`() {
        repeat(49) { fresh("S$it") }
        assertEquals(50, schedules().size)
        assertEquals("HAB-SCH-005", demo.code(create(body(name = "Over"))))
    }

    @Test
    fun `an update with the current updated_at changes the schedule and gives it a new version`() {
        val created = fresh()
        val updated = demo.result(
            update(text(created, "id"), text(created, "updated_at"), """"name":"Night","enabled":false"""),
        ).jsonObject
        assertEquals("Night", text(updated, "name"))
        assertEquals("false", text(updated, "enabled"))
        assertNull(text(updated, "next_run"))
        assertNotEquals(text(created, "updated_at"), text(updated, "updated_at"))
        assertEquals(text(created, "created_at"), text(updated, "created_at"))
    }

    @Test
    fun `an update of an old version is HAB-SCH-004 and of an unknown schedule HAB-SCH-003`() {
        val created = fresh()
        val id = text(created, "id")
        val stale = text(created, "updated_at")
        update(id, stale, """"name":"One"""")
        assertEquals("HAB-SCH-004", demo.code(update(id, stale, """"name":"Two"""")))
        assertEquals("One", text(schedules().first { text(it, "id") == id }, "name"))
        assertEquals("HAB-SCH-003", demo.code(update("nope", stale, """"name":"Two"""")))
    }

    @Test
    fun `an update can change the trigger and the entities, and an invalid result is HAB-SCH-001`() {
        val created = fresh()
        val id = text(created, "id")
        val version = text(created, "updated_at")
        assertEquals("HAB-SCH-001", demo.code(update(id, version, """"when":${time("99:99")}""")))
        val changed = demo.result(
            update(id, version, """"when":${sun("sunset", offset = 15, days = "[5]")},"entities":["$Kitchen"]"""),
        ).jsonObject
        assertEquals("sunset", text(changed.obj("when"), "type"))
        assertEquals("15", text(changed.obj("when"), "offset_min"))
        assertEquals(listOf(Kitchen), strings(changed, "entities"))
    }

    @Test
    fun `delete removes a schedule and an unknown schedule is not an error`() {
        val id = text(fresh(), "id")
        val delete = """{"schedule_id":"$id"}"""
        assertEquals(JsonPrimitive(true), demo.request("haac_bridge/schedules/delete", delete)["success"])
        assertEquals(1, schedules().size)
        assertEquals(JsonPrimitive(true), demo.request("haac_bridge/schedules/delete", delete)["success"])
    }

    @Test
    fun `the revision follows the schedules and is the same for the same data`() {
        val first = demo.ask("haac_bridge/schedules/revision")
        assertEquals(first["revision"], demo.ask("haac_bridge/schedules/list")["revision"])
        assertEquals(first["revision"], DemoHarness().ask("haac_bridge/schedules/revision")["revision"])
        fresh()
        assertNotEquals(first["revision"], demo.ask("haac_bridge/schedules/revision")["revision"])
        assertEquals("own", text(first, "scope"))
    }

    @Test
    fun `a subscription gets schedules_changed with the new revision after every change`() {
        demo.request("haac_bridge/subscribe_schedules")
        val subscription = demo.lastMessageId
        assertTrue(demo.events(subscription).isEmpty())
        fresh()
        val event = demo.events(subscription).single().obj("schedules_changed")
        assertEquals(demo.ask("haac_bridge/schedules/revision")["revision"], event["revision"])
    }

    @Test
    fun `run now switches the entities at once, sets last_run and keeps the plan`() {
        // The kitchen light starts off; a schedule that turns it on shows the change.
        val created = demo.result(create(body(action = "turn_on", entities = "\"$Kitchen\""))).jsonObject
        val id = text(created, "id")
        demo.request("haac_bridge/subscribe_entities")
        val states = demo.lastMessageId
        demo.request("haac_bridge/subscribe_schedules")
        val changes = demo.lastMessageId
        val before = schedules().first { text(it, "id") == id }
        val run = demo.request("haac_bridge/schedules/run_now", """{"schedule_id":"$id"}""")
        assertEquals(JsonPrimitive(true), run["success"])
        val diff = demo.events(states).last().obj("c").obj(Kitchen).obj("+")
        assertEquals("on", diff.str("s"))
        assertEquals(1, demo.events(changes).size)
        val after = schedules().first { text(it, "id") == id }
        assertEquals("ok", text(after.obj("last_run"), "result"))
        assertEquals(JsonNull, before["last_run"])
        assertEquals(before["when"], after["when"])
        assertEquals(before["updated_at"], after["updated_at"])
    }

    @Test
    fun `run now of an unknown schedule is HAB-SCH-003`() {
        val unknown = demo.request("haac_bridge/schedules/run_now", """{"schedule_id":"nope"}""")
        assertEquals("HAB-SCH-003", demo.code(unknown))
    }
}
