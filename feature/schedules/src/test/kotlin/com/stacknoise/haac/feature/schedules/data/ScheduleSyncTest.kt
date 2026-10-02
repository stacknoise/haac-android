package com.stacknoise.haac.feature.schedules.data

import com.stacknoise.haac.core.common.sync.ScheduleChangeReporter
import com.stacknoise.haac.core.database.schedule.ScheduleEntity
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.connection.BridgeChannel
import com.stacknoise.haac.core.network.demo.DemoWorld
import com.stacknoise.haac.feature.schedules.domain.ScheduleDraft
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleSyncTest {
    private val json = Json { ignoreUnknownKeys = true }

    /** The bridge side: [revision] and [listed] answer the commands, [events] feed the subscription. */
    private inner class FakeChannel : BridgeChannel {
        override var isOpen = true
        override var features = setOf("schedules")
        var revision = "r1"
        var scope = "own"
        var listed = listOf(descriptor("a", "Morning light"), descriptor("b", "Evening"))
        var failure: HaacException? = null
        val requests = mutableListOf<String>()
        val events = Channel<JsonObject>(Channel.UNLIMITED)

        override suspend fun request(type: String, fields: JsonObject): JsonElement {
            requests += type
            failure?.let { throw it }
            return when (type) {
                "haac_bridge/schedules/revision" -> parse("""{"revision":"$revision","scope":"$scope"}""")
                else -> parse(
                    """{"revision":"$revision","scope":"$scope","schedules":[${listed.joinToString(",")}]}""",
                )
            }
        }

        override fun subscribe(type: String, fields: JsonObject): Flow<JsonObject> {
            requests += type
            return events.consumeAsFlow()
        }

        fun push(event: String) = events.trySend(parse(event).jsonObject)
    }

    private val cache = linkedMapOf<String, ScheduleEntity>()
    private val dao = FakeScheduleDao(cache)

    private val reportedErrors = mutableListOf<ErrorCode>()
    private val removed = mutableListOf<List<String>>()
    private val paused = mutableListOf<Pair<String, String>>()
    private val reporter = object : ScheduleChangeReporter {
        override suspend fun removed(serverId: String, names: List<String>) {
            removed += names
        }

        override suspend fun paused(serverId: String, name: String, reason: String) {
            paused += name to reason
        }
    }
    private val sync = ScheduleSync(
        dao,
        json,
        DefaultErrorFactory(),
        { error, _ -> reportedErrors += error.code },
        reporter,
    ) { 100L }
    private val channel = FakeChannel()

    private fun parse(text: String): JsonElement = json.parseToJsonElement(text)

    private fun descriptor(id: String, name: String, paused: String? = null, own: String = "") =
        """{"id":"$id","owner":"u1","name":"$name","enabled":true,"when":{"type":"time","time":"06:45",""" +
            """"days":[0,1,2,3,4]},"action":"turn_on","entities":["switch.a"],""" +
            """"created_at":"2026-10-01T05:12:00+00:00","updated_at":"2026-10-01T05:12:00.123456+00:00",""" +
            (paused?.let { """"paused":{"reason":"$it","at":"2026-10-02T06:00:00+00:00"},""" } ?: "") +
            """$own"next_run":"2026-10-03T06:45:00+02:00"}"""

    private fun TestScope.follow() {
        backgroundScope.launch { sync.follow("s1", channel) }
        runCurrent()
    }

    @Test
    fun `first sync stores the list silently`() = runTest {
        follow()
        assertEquals(listOf("a", "b"), cache.keys.toList())
        val row = cache.getValue("a")
        assertEquals(31, row.days)
        assertEquals("2026-10-01T05:12:00.123456+00:00", row.updatedAt)
        assertEquals(1791002700000, row.nextRun)
        assertEquals(true, row.own)
        assertEquals(emptyList<List<String>>(), removed)
        val commands = listOf(
            "haac_bridge/schedules/revision",
            "haac_bridge/schedules/list",
            "haac_bridge/subscribe_schedules",
        )
        assertEquals(commands, channel.requests)
    }

    @Test
    fun `a bridge without the feature is left alone`() = runTest {
        channel.features = emptySet()
        follow()
        assertEquals(emptyList<String>(), channel.requests)
        assertEquals(0, cache.size)
    }

    @Test
    fun `a changed event with the same revision skips the list`() = runTest {
        follow()
        channel.requests.clear()
        channel.push("""{"schedules_changed":{"revision":"r1"}}""")
        runCurrent()
        assertEquals(listOf("haac_bridge/schedules/revision"), channel.requests)
    }

    @Test
    fun `a removed schedule and a new pause are announced`() = runTest {
        follow()
        channel.revision = "r2"
        channel.listed = listOf(descriptor("a", "Morning light", paused = "no_entities"))
        channel.push("""{"schedules_changed":{"revision":"r2"}}""")
        runCurrent()
        assertEquals(listOf("a"), cache.keys.toList())
        assertEquals(listOf(listOf("Evening")), removed)
        assertEquals(listOf("Morning light" to "no_entities"), paused)
        assertEquals("no_entities", cache.getValue("a").pausedReason)
    }

    @Test
    fun `without own the scope decides, in the admin scope own wins`() = runTest {
        channel.scope = "all"
        channel.listed = listOf(descriptor("a", "Mine", own = """"own":true,"""), descriptor("b", "Hers"))
        follow()
        assertEquals(true, cache.getValue("a").own)
        assertEquals(false, cache.getValue("b").own)
    }

    @Test
    fun `errors keep the cache and are reported unless the connection ended`() = runTest {
        follow()
        channel.failure = NetworkException(ErrorCode.NET_CONNECTION_LOST)
        channel.revision = "r2"
        channel.push("""{"schedules_changed":{"revision":"r2"}}""")
        runCurrent()
        assertEquals(listOf("a", "b"), cache.keys.toList())
        assertEquals(listOf(ErrorCode.NET_CONNECTION_LOST), reportedErrors)
    }

    private fun TestScope.followDemo(world: DemoWorld = demoWorld()) {
        val connection = demoConnection(world)
        backgroundScope.launch { sync.follow("s1", connection) }
        runCurrent()
    }

    @Test
    fun `the first sync of the demo bridge stores Morning light silently`() = runTest {
        followDemo()
        val row = cache.values.single()
        assertEquals("Morning light", row.name)
        assertEquals("time", row.whenType)
        assertEquals("07:00", row.time)
        assertEquals(31, row.days)
        assertEquals(true, row.own)
        assertNotNull(row.nextRun)
        assertEquals(emptyList<List<String>>(), removed)
    }

    @Test
    fun `a schedule created on the demo reaches the cache and one deleted there is reported as removed`() = runTest {
        val world = demoWorld()
        followDemo(world)
        val other = demoConnection(world)
        val draft = ScheduleDraft(name = "Evening", entityIds = listOf("switch.demo_socket"))
        other.request("haac_bridge/schedules/create", ScheduleFields.create(draft))
        runCurrent()
        assertEquals(setOf("Morning light", "Evening"), cache.values.map { it.name }.toSet())
        other.request("haac_bridge/schedules/delete", buildJsonObject { put("schedule_id", "demo-morning-light") })
        runCurrent()
        assertEquals(listOf("Evening"), cache.values.map { it.name })
        assertEquals(listOf(listOf("Morning light")), removed)
    }
}
