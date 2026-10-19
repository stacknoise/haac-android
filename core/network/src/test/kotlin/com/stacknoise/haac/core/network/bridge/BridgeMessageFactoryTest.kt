package com.stacknoise.haac.core.network.bridge

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** The typed commands of the extension (concept 7.5, 19.10, 19.11) and their message ids. */
class BridgeMessageFactoryTest {
    private val factory = DefaultBridgeMessageFactory()

    private fun parse(command: BridgeCommand): JsonObject = Json.parseToJsonElement(command.json).jsonObject

    @Test
    fun `set_disabled names the entity and the direction`() {
        val message = parse(factory.setDisabled("switch.a", true))
        assertEquals("haac_bridge/entities/set_disabled", message.getValue("type").jsonPrimitive.content)
        assertEquals("switch.a", message.getValue("entity_id").jsonPrimitive.content)
        assertEquals(true, message.getValue("disabled").jsonPrimitive.boolean)
    }

    @Test
    fun `preview carries the trigger and the repeat rule`() {
        val trigger = buildJsonObject { put("type", "time") }
        val repeat = buildJsonObject { put("freq", "daily") }
        val message = parse(factory.preview(trigger, repeat))
        assertEquals("haac_bridge/schedules/preview", message.getValue("type").jsonPrimitive.content)
        assertEquals(trigger, message.getValue("when"))
        assertEquals(repeat, message.getValue("repeat"))
    }

    @Test
    fun `runs sends the cursor only when there is one`() {
        val first = parse(factory.runs())
        assertEquals(setOf("id", "type"), first.keys)
        val next = parse(factory.runs("log-1", 41))
        assertEquals("log-1", next.getValue("log_id").jsonPrimitive.content)
        assertEquals(41, next.getValue("after_seq").jsonPrimitive.int)
        assertEquals("haac_bridge/schedules/runs", next.getValue("type").jsonPrimitive.content)
    }

    @Test
    fun `message ids keep increasing across the typed commands`() {
        val ids = listOf(factory.runs().id, factory.setDisabled("switch.a", false).id, factory.command("ping").id)
        assertEquals(listOf(1, 2, 3), ids)
    }
}
