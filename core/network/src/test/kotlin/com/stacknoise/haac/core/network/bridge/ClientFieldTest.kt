package com.stacknoise.haac.core.network.bridge

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** [ClientField]: the device name goes out only to a bridge that accepts it (concept 11.2, 21.5). */
class ClientFieldTest {
    private val fields = buildJsonObject { put("entity_id", "switch.lamp") }
    private val accepting = setOf("schedules", ClientField.FEATURE)

    @Test
    fun `the name is added when the bridge lists client_info`() {
        val result = ClientField.attach(fields, "Pixel 8", accepting)

        assertEquals(JsonPrimitive("Pixel 8"), result["client"])
        assertEquals(JsonPrimitive("switch.lamp"), result["entity_id"])
    }

    @Test
    fun `an older bridge gets the fields unchanged`() {
        assertEquals(fields, ClientField.attach(fields, "Pixel 8", setOf("schedules")))
    }

    @Test
    fun `no name or an unusable name adds nothing`() {
        assertEquals(fields, ClientField.attach(fields, null, accepting))
        assertEquals(fields, ClientField.attach(fields, "", accepting))
        assertEquals(fields, ClientField.attach(fields, " \n ", accepting))
    }

    @Test
    fun `the name is cleaned before it is sent`() {
        val result = ClientField.attach(fields, "  Pixel\u0000 8\n", accepting)

        assertEquals(JsonPrimitive("Pixel 8"), result["client"])
    }

    @Test
    fun `the given fields are not changed`() {
        ClientField.attach(fields, "Pixel 8", accepting)

        assertEquals(JsonObject(mapOf("entity_id" to JsonPrimitive("switch.lamp"))), fields)
    }
}
