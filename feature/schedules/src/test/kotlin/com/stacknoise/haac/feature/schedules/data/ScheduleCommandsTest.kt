package com.stacknoise.haac.feature.schedules.data

import com.stacknoise.haac.core.database.schedule.ScheduleEntity
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.network.connection.BridgeChannel
import com.stacknoise.haac.core.network.connection.LiveConnection
import com.stacknoise.haac.feature.schedules.domain.ScheduleAction
import com.stacknoise.haac.feature.schedules.domain.ScheduleDraft
import com.stacknoise.haac.feature.schedules.domain.toItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ScheduleCommandsTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val sent = mutableListOf<Pair<String, JsonObject>>()
    private val cache = linkedMapOf<String, ScheduleEntity>()

    private val reply = """
        {"id":"a","owner":"u1","owner_name":"Anton","own":true,"name":"Morning light","enabled":true,
         "when":{"type":"time","time":"06:45","days":[0,1,2,3,4]},"action":"turn_on","entities":["switch.a"],
         "created_at":"2026-10-01T05:12:00+00:00","updated_at":"2026-10-02T05:12:00+00:00","next_run":null}
    """.trimIndent()

    private val channel = object : BridgeChannel {
        override val isOpen = true

        override suspend fun request(type: String, fields: JsonObject): JsonElement {
            sent += type to fields
            return json.parseToJsonElement(reply)
        }

        override fun subscribe(type: String, fields: JsonObject): Flow<JsonObject> = error("not used")
    }

    private val connection = MutableStateFlow<BridgeChannel?>(channel)
    private val live = object : LiveConnection {
        override val connection: StateFlow<BridgeChannel?> = this@ScheduleCommandsTest.connection
    }

    private val dao = FakeScheduleDao(cache)

    private val commands = ScheduleCommands(live, dao, json, DefaultErrorFactory())

    private val draft = ScheduleDraft(name = "Morning light", entityIds = listOf("switch.a"))

    private fun cached() = ScheduleDescriptor.serializer().let { json.decodeFromString(it, reply) }
        .toRow("s1", "own", 1).toItem()

    @Test
    fun `a new schedule is created and cached`() = runTest {
        commands.save("s1", null, draft)
        assertEquals("haac_bridge/schedules/create", sent.single().first)
        assertEquals("Morning light", sent.single().second.getValue("name").jsonPrimitive.content)
        assertEquals(listOf("a"), cache.keys.toList())
    }

    @Test
    fun `an edit names the schedule and its version and sends only the changes`() = runTest {
        val original = cached()
        commands.save("s1", original, ScheduleDraft.of(original).copy(action = ScheduleAction.TOGGLE))
        val (type, fields) = sent.single()
        assertEquals("haac_bridge/schedules/update", type)
        assertEquals(setOf("schedule_id", "updated_at", "action"), fields.keys)
        assertEquals("2026-10-02T05:12:00+00:00", fields.getValue("updated_at").jsonPrimitive.content)
    }

    @Test
    fun `nothing is sent when nothing changed`() = runTest {
        val original = cached()
        commands.save("s1", original, ScheduleDraft.of(original))
        assertEquals(emptyList<Pair<String, JsonObject>>(), sent)
    }

    @Test
    fun `without a connection the command fails and the cache stays`() = runTest {
        connection.value = null
        val failure = runCatching { commands.save("s1", null, draft) }.exceptionOrNull()
        assertEquals(ErrorCode.NET_CONNECTION_LOST, (failure as HaacException).code)
        assertEquals(emptyMap<String, ScheduleEntity>(), cache)
    }

    @Test
    fun `a deleted schedule leaves the cache`() = runTest {
        commands.save("s1", null, draft)
        commands.delete("s1", "a")
        assertEquals("haac_bridge/schedules/delete", sent.last().first)
        assertEquals("a", sent.last().second.getValue("schedule_id").jsonPrimitive.content)
        assertEquals(emptyMap<String, ScheduleEntity>(), cache)
    }
}
