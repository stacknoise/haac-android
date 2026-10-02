package com.stacknoise.haac.core.network.demo

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private const val Thermostat = "climate.demo_thermostat"
private const val Temperature20 = """{"temperature":20}"""

class DemoEntityCommandsTest {
    private val demo = DemoHarness()

    private fun service(entity: String, service: String, data: String = "{}") = demo.request(
        "haac_bridge/call_service",
        """{"entity_id":"$entity","service":"$service","service_data":$data}""",
    )

    /** The `c` entry of the entity in the last event of the subscription [id]. */
    private fun change(id: Int, entity: String): JsonObject =
        demo.events(id).last()["c"]!!.jsonObject[entity]!!.jsonObject["+"]!!.jsonObject

    @Test
    fun `info reports API 1, the fixed instance and the schedules feature`() {
        val info = demo.ask("haac_bridge/info")
        assertEquals("1", info["api_version"]!!.jsonPrimitive.content)
        assertEquals(DemoInstance.INSTANCE_ID, info["instance_id"]!!.jsonPrimitive.content)
        assertEquals(listOf("schedules"), info["features"]!!.jsonArray.map { it.jsonPrimitive.content })
    }

    @Test
    fun `the demo has the entities of concept 20-3 with names and areas`() {
        val list = demo.ask("haac_bridge/entities/list")
        val byId = demo.list(list, "entities").associateBy { it.jsonObject["entity_id"]!!.jsonPrimitive.content }
        assertEquals(
            setOf(
                "switch.demo_living_room_light", "switch.demo_kitchen_light", "switch.demo_bedroom_lamp",
                "switch.demo_socket", "sensor.demo_temperature", "sensor.demo_energy", "climate.demo_thermostat",
            ),
            byId.keys,
        )
        val temperature = byId.getValue("sensor.demo_temperature").jsonObject
        assertEquals("°C", temperature["unit_of_measurement"]!!.jsonPrimitive.content)
        assertEquals("measurement", temperature["state_class"]!!.jsonPrimitive.content)
        assertEquals("Living room", temperature["area"]!!.jsonPrimitive.content)
        assertEquals("total_increasing", byId.getValue("sensor.demo_energy").str("state_class"))
    }

    @Test
    fun `the revision is the same for the same data, across restarts and after a state change`() {
        val first = demo.ask("haac_bridge/exposure/revision")
        assertEquals("7", first["entity_count"]!!.jsonPrimitive.content)
        assertEquals(first["revision"], demo.ask("haac_bridge/entities/list")["revision"])
        service("switch.demo_socket", "turn_off")
        assertEquals(first["revision"], demo.ask("haac_bridge/exposure/revision")["revision"])
        assertEquals(first["revision"], DemoHarness().ask("haac_bridge/exposure/revision")["revision"])
    }

    @Test
    fun `areas lists the floor and the three areas with their entity counts`() {
        val areas = demo.ask("haac_bridge/areas")
        assertEquals(
            "Ground floor",
            demo.list(areas, "floors").single().jsonObject["name"]!!.jsonPrimitive.content,
        )
        val counts = demo.list(areas, "areas").associate {
            it.jsonObject["name"]!!.jsonPrimitive.content to it.jsonObject["entity_count"]!!.jsonPrimitive.content
        }
        assertEquals(mapOf("Living room" to "4", "Kitchen" to "2", "Bedroom" to "1"), counts)
        assertTrue(demo.list(areas, "areas").all { it.str("floor_id") == "ground_floor" })
    }

    @Test
    fun `subscribing answers with an empty result and then all states as an added event`() {
        val reply = demo.request("haac_bridge/subscribe_entities")
        assertEquals(JsonPrimitive(true), reply["success"])
        val added = demo.events(demo.lastMessageId).single()["a"]!!.jsonObject
        assertEquals(7, added.size)
        assertEquals("on", added.getValue("switch.demo_living_room_light").jsonObject["s"]!!.jsonPrimitive.content)
        assertNotNull(added.getValue("switch.demo_socket").jsonObject["lc"])
    }

    @Test
    fun `a service call changes the state and sends the change on the subscription`() {
        demo.request("haac_bridge/subscribe_entities")
        val subscription = demo.lastMessageId
        val reply = service("switch.demo_living_room_light", "turn_off")
        assertEquals(JsonPrimitive(true), reply["success"])
        val diff = change(subscription, "switch.demo_living_room_light")
        assertEquals("off", diff["s"]!!.jsonPrimitive.content)
        assertNotNull(diff["lc"])
        val after = demo.ask("haac_bridge/entities/list")
        val light = demo.list(after, "entities").first { it.str("entity_id") == "switch.demo_living_room_light" }
        assertEquals("off", light.str("state"))
    }

    @Test
    fun `toggle flips a switch and a call that changes nothing sends no event`() {
        demo.request("haac_bridge/subscribe_entities")
        val subscription = demo.lastMessageId
        service("switch.demo_kitchen_light", "toggle")
        assertEquals("on", change(subscription, "switch.demo_kitchen_light")["s"]!!.jsonPrimitive.content)
        val events = demo.events(subscription).size
        service("switch.demo_kitchen_light", "turn_on")
        assertEquals(events, demo.events(subscription).size)
    }

    @Test
    fun `a service the entity does not support is HAB-SVC-002`() {
        assertEquals("HAB-SVC-002", demo.code(service("switch.demo_socket", "set_temperature", Temperature20)))
        assertEquals("HAB-SVC-002", demo.code(service("sensor.demo_energy", "turn_on")))
        assertEquals("HAB-SVC-002", demo.code(service("climate.demo_thermostat", "turn_on")))
        assertEquals("HAB-SVC-002", demo.code(service(Thermostat, "set_humidity", """{"humidity":40}""")))
    }

    @Test
    fun `an unknown entity is HAB-ENT-001 and target keys in service data are HAB-WS-001`() {
        assertEquals("HAB-ENT-001", demo.code(service("switch.nobody", "turn_on")))
        val withTarget = service("switch.demo_socket", "turn_on", """{"entity_id":"switch.demo_kitchen_light"}""")
        assertEquals("HAB-WS-001", demo.code(withTarget))
    }

    @Test
    fun `the thermostat takes a target inside its limits and sends it as an attribute change`() {
        demo.request("haac_bridge/subscribe_entities")
        val subscription = demo.lastMessageId
        assertEquals(JsonPrimitive(true), service(Thermostat, "set_temperature", """{"temperature":22.5}""")["success"])
        val attributes = change(subscription, "climate.demo_thermostat")["a"]!!.jsonObject
        assertEquals(22.5, attributes["temperature"]!!.jsonPrimitive.doubleOrNull)
        assertNull(change(subscription, "climate.demo_thermostat")["s"])
    }

    @Test
    fun `a target outside the limits and an unknown mode are rejected as HAB-SVC-003`() {
        assertEquals("HAB-SVC-003", demo.code(service(Thermostat, "set_temperature", """{"temperature":35}""")))
        assertEquals("HAB-SVC-003", demo.code(service(Thermostat, "set_temperature", """{"temperature":5}""")))
        assertEquals("HAB-SVC-003", demo.code(service(Thermostat, "set_hvac_mode", """{"hvac_mode":"cool"}""")))
    }

    @Test
    fun `the mode off stops heating and the thermostat heats again in mode heat`() {
        demo.request("haac_bridge/subscribe_entities")
        val subscription = demo.lastMessageId
        service("climate.demo_thermostat", "set_hvac_mode", """{"hvac_mode":"off"}""")
        val off = change(subscription, "climate.demo_thermostat")
        assertEquals("off", off["s"]!!.jsonPrimitive.content)
        assertEquals("off", off["a"]!!.jsonObject["hvac_action"]!!.jsonPrimitive.content)
        service("climate.demo_thermostat", "set_hvac_mode", """{"hvac_mode":"heat"}""")
        assertEquals("heating", change(subscription, Thermostat).obj("a").str("hvac_action"))
    }

    @Test
    fun `unsubscribing stops the events`() {
        demo.request("haac_bridge/subscribe_entities")
        val subscription = demo.lastMessageId
        demo.request("unsubscribe_events", """{"subscription":$subscription}""")
        service("switch.demo_socket", "turn_off")
        assertEquals(1, demo.events(subscription).size)
    }

    @Test
    fun `ping is answered with pong and an unknown command with unknown_command`() {
        assertEquals("pong", demo.request("ping")["type"]!!.jsonPrimitive.content)
        assertEquals("unknown_command", demo.code(demo.request("haac_bridge/nope")))
        assertFalse(demo.out.any { it["id"] == null })
    }
}
