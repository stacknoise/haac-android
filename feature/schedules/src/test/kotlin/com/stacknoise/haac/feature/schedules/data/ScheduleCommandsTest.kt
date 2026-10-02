package com.stacknoise.haac.feature.schedules.data

import com.stacknoise.haac.core.database.schedule.ScheduleEntity
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.network.connection.BridgeChannel
import com.stacknoise.haac.core.network.connection.BridgeConnection
import com.stacknoise.haac.core.network.connection.LiveConnection
import com.stacknoise.haac.core.network.demo.DemoWorld
import com.stacknoise.haac.feature.schedules.domain.ScheduleAction
import com.stacknoise.haac.feature.schedules.domain.ScheduleDraft
import com.stacknoise.haac.feature.schedules.domain.toItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ScheduleCommandsTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val sent = mutableListOf<Pair<String, JsonObject>>()
    private val cache = linkedMapOf<String, ScheduleEntity>()

    private var reply = """
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
    fun `an admin changing a foreign schedule never sends the entities and keeps it foreign`() = runTest {
        reply = reply.replace(""""own":true,""", "")
        val foreign = cached().copy(own = false, ownerName = "Lena")
        val draft = ScheduleDraft.of(foreign).copy(entityIds = listOf("switch.z"), action = ScheduleAction.TURN_OFF)
        commands.save("s1", foreign, draft)
        assertEquals(setOf("schedule_id", "updated_at", "action"), sent.single().second.keys)
        assertEquals(false, cache.getValue("a").own)
    }

    @Test
    fun `switching a foreign schedule off keeps it foreign in the cache`() = runTest {
        reply = reply.replace(""""own":true,""", "")
        commands.setEnabled("s1", cached().copy(own = false), enabled = false)
        assertEquals(false, cache.getValue("a").own)
        assertEquals("false", sent.single().second.getValue("enabled").jsonPrimitive.content)
    }

    @Test
    fun `a deleted schedule leaves the cache`() = runTest {
        commands.save("s1", null, draft)
        commands.delete("s1", "a")
        assertEquals("haac_bridge/schedules/delete", sent.last().first)
        assertEquals("a", sent.last().second.getValue("schedule_id").jsonPrimitive.content)
        assertEquals(emptyMap<String, ScheduleEntity>(), cache)
    }

    private fun TestScope.useDemo(world: DemoWorld = demoWorld()): BridgeConnection =
        demoConnection(world).also { connection.value = it }

    private val evening = ScheduleDraft(
        name = "Evening",
        action = ScheduleAction.TURN_OFF,
        entityIds = listOf("switch.demo_socket"),
    )

    @Test
    fun `against the demo bridge a schedule is created, edited, switched off and deleted`() = runTest {
        useDemo()
        commands.save("s1", null, evening)
        val created = cache.values.single()
        assertEquals("Evening", created.name)
        assertEquals(true, created.own)
        assertEquals("turn_off", created.action)
        commands.save("s1", created.toItem(), ScheduleDraft.of(created.toItem()).copy(name = "Late"))
        assertEquals("Late", cache.getValue(created.scheduleId).name)
        commands.setEnabled("s1", cache.getValue(created.scheduleId).toItem(), false)
        assertEquals(false, cache.getValue(created.scheduleId).enabled)
        assertNull(cache.getValue(created.scheduleId).nextRun)
        commands.delete("s1", created.scheduleId)
        assertEquals(emptyMap<String, ScheduleEntity>(), cache)
    }

    @Test
    fun `an edit of an old version fails with the conflict of the demo bridge`() = runTest {
        useDemo()
        commands.save("s1", null, evening)
        val old = cache.values.single().toItem()
        commands.save("s1", old, ScheduleDraft.of(old).copy(name = "One"))
        val failure = runCatching { commands.save("s1", old, ScheduleDraft.of(old).copy(name = "Two")) }
            .exceptionOrNull() as HaacException
        assertEquals(ErrorCode.SCH_CONFLICT, failure.code)
        assertEquals("HAB-SCH-004", failure.bridgeCode)
        assertEquals("One", cache.values.single().name)
    }

    @Test
    fun `an invalid schedule is rejected by the demo bridge as HAAC-SCH-003`() = runTest {
        useDemo()
        val failure = runCatching { commands.save("s1", null, evening.copy(entityIds = listOf("sensor.demo_energy"))) }
            .exceptionOrNull() as HaacException
        assertEquals(ErrorCode.SCH_INVALID, failure.code)
        assertEquals(emptyMap<String, ScheduleEntity>(), cache)
    }
}
