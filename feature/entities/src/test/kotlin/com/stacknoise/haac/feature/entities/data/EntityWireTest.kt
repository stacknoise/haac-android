package com.stacknoise.haac.feature.entities.data

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/** The deactivation fields of the entity replies (concept 7.5, 11.2). */
class EntityWireTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun descriptor(extra: String) = json.decodeFromString(
        EntityDescriptor.serializer(),
        """{"entity_id":"switch.a","domain":"switch","state":"on"$extra}""",
    )

    @Test
    fun `an entity of a bridge without the feature is not deactivated`() {
        assertFalse(descriptor("").toRow("s1", "{}").disabled)
    }

    @Test
    fun `a deactivated entity reaches the cache row`() {
        assertEquals(true, descriptor(""","disabled":true""").toRow("s1", "{}").disabled)
    }

    @Test
    fun `the answer of set_disabled lists the deactivated entities and the schedules it changed`() {
        val reply = json.decodeFromString(
            SetDisabledReply.serializer(),
            """{"disabled":["switch.a"],"schedules_changed":["s1","s2"]}""",
        )
        assertEquals(listOf("switch.a"), reply.disabled)
        assertEquals(2, reply.schedulesChanged.size)
    }
}
