package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.feature.entities.domain.EntityState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CompressedStateTest {
    private fun obj(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    @Test
    fun `full state without lu uses lc for both times`() {
        val compressed = obj("""{"s":"on","a":{"device_class":"outlet"},"c":"01J","lc":1790406723.1}""")
        val state = CompressedState.full(compressed)
        assertEquals(EntityState("on", obj("""{"device_class":"outlet"}"""), 1790406723100, 1790406723100), state)
        assertNull(CompressedState.full(obj("""{"a":{}}""")))
    }

    @Test
    fun `a change merges attributes and removes the listed ones`() {
        val current = EntityState("heat", obj("""{"temperature":21,"preset_mode":"eco","hvac_action":"idle"}"""), 1, 1)
        val next = CompressedState.apply(
            current,
            obj("""{"+":{"a":{"temperature":22},"lu":1790410001.5},"-":{"a":["preset_mode"]}}"""),
        )
        assertEquals("heat", next.state)
        assertEquals(obj("""{"temperature":22,"hvac_action":"idle"}"""), next.attributes)
        assertEquals(1L, next.lastChanged)
        assertEquals(1790410001500, next.lastUpdated)
    }

    @Test
    fun `a new state with lc sets both times`() {
        val current = EntityState("on", lastChanged = 1, lastUpdated = 1)
        val next = CompressedState.apply(current, obj("""{"+":{"s":"off","lc":2.0}}"""))
        assertEquals(EntityState("off", JsonObject(emptyMap()), 2000, 2000), next)
    }
}
