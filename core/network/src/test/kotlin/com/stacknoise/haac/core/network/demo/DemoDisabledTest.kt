package com.stacknoise.haac.core.network.demo

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private const val Light = "switch.demo_living_room_light"
private const val Morning = "demo-morning-light"

class DemoDisabledTest {
    private val demo = DemoHarness()

    private fun setDisabled(entity: String, disabled: Boolean) =
        demo.request("haac_bridge/entities/set_disabled", """{"entity_id":"$entity","disabled":$disabled}""")

    private fun morning(): JsonObject = demo.list(demo.ask("haac_bridge/schedules/list"), "schedules")
        .map { it.jsonObject }
        .single { it.str("id") == Morning }

    private fun strings(obj: JsonObject, key: String) = obj[key]!!.jsonArray.map { it.jsonPrimitive.content }

    @Test
    fun `deactivating answers with the full list and the schedules it switched off`() {
        val result = demo.result(setDisabled(Light, true)).jsonObject
        assertEquals(listOf(Light), strings(result, "disabled"))
        assertEquals(listOf(Morning), strings(result, "schedules_changed"))
        val schedule = morning()
        assertEquals("false", schedule.str("enabled"))
        assertEquals("entity_disabled", schedule.str("off_reason"))
        assertEquals(listOf(Light), strings(schedule, "blocked_by"))
    }

    @Test
    fun `the entity list marks the deactivated entity`() {
        setDisabled(Light, true)
        val list = demo.list(demo.ask("haac_bridge/entities/list"), "entities")
        val flags = list.associate { it.str("entity_id") to it.str("disabled") }
        assertEquals("true", flags.getValue(Light))
        assertEquals("false", flags.getValue("switch.demo_socket"))
    }

    @Test
    fun `activating switches the schedule on again and clears the reason`() {
        setDisabled(Light, true)
        val result = demo.result(setDisabled(Light, false)).jsonObject
        assertEquals(emptyList<String>(), strings(result, "disabled"))
        assertEquals(listOf(Morning), strings(result, "schedules_changed"))
        val schedule = morning()
        assertEquals("true", schedule.str("enabled"))
        assertEquals(emptyList<String>(), strings(schedule, "blocked_by"))
    }

    @Test
    fun `a service call for a deactivated entity fails with HAB-ENT-002`() {
        setDisabled(Light, true)
        val reply = demo.request(
            "haac_bridge/call_service",
            """{"entity_id":"$Light","service":"turn_off","service_data":{}}""",
        )
        assertEquals("HAB-ENT-002", demo.code(reply))
    }

    @Test
    fun `switching on a blocked schedule or running it fails with HAB-SCH-008`() {
        setDisabled(Light, true)
        val updated = demo.request(
            "haac_bridge/schedules/update",
            """{"schedule_id":"$Morning","updated_at":"${morning().str("updated_at")}","enabled":true}""",
        )
        assertEquals("HAB-SCH-008", demo.code(updated))
        val run = demo.request("haac_bridge/schedules/run_now", """{"schedule_id":"$Morning"}""")
        assertEquals("HAB-SCH-008", demo.code(run))
    }

    @Test
    fun `an unknown entity fails with HAB-ENT-001`() {
        assertEquals("HAB-ENT-001", demo.code(setDisabled("switch.nope", true)))
    }

    @Test
    fun `the subscriptions get disabled_changed with the full list`() {
        val id = demo.request("haac_bridge/subscribe_entities").let { demo.lastMessageId }
        setDisabled(Light, true)
        val event = demo.events(id).last()["disabled_changed"]!!.jsonObject
        assertEquals(listOf(Light), strings(event, "disabled"))
    }
}
