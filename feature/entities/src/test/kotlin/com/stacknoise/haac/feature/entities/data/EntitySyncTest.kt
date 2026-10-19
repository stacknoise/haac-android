package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.core.database.entity.EntityStatus
import com.stacknoise.haac.core.database.entity.EntityDisabledDao
import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.connection.BridgeChannel
import com.stacknoise.haac.core.network.connection.BridgeConnection
import com.stacknoise.haac.core.network.demo.DemoWorld
import com.stacknoise.haac.core.network.demo.demoConnection
import com.stacknoise.haac.core.network.demo.demoWorld
import com.stacknoise.haac.feature.entities.disable.data.DisabledSync
import com.stacknoise.haac.feature.entities.domain.ControlRequest
import com.stacknoise.haac.feature.entities.domain.DefaultServiceCallFactory
import com.stacknoise.haac.feature.entities.domain.EntityState
import com.stacknoise.haac.feature.entities.domain.SyncResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EntitySyncTest {
    private companion object {
        const val FLUSH_MS = 5_000L
        const val ENTITIES = 200
        const val EVENTS = 50
    }

    private val json = Json { ignoreUnknownKeys = true }

    /** The bridge side: [revision] and [listed] answer the commands, [events] feed the subscription. */
    private inner class FakeChannel : BridgeChannel {
        override var isOpen = true
        override var features: Set<String> = emptySet()
        var revision = "r1"
        var listed = listOf(descriptor("switch.a", "on"), descriptor("sensor.b", "21"))
        var failure: HaacException? = null
        val requests = mutableListOf<String>()
        val events = Channel<JsonObject>(Channel.UNLIMITED)

        override suspend fun request(type: String, fields: JsonObject): JsonElement {
            requests += type
            failure?.let { throw it }
            return when (type) {
                "haac_bridge/exposure/revision" -> parse("""{"revision":"$revision","entity_count":${listed.size}}""")
                else -> parse("""{"revision":"$revision","entities":[${listed.joinToString(",")}]}""")
            }
        }

        override fun subscribe(type: String, fields: JsonObject): Flow<JsonObject> = events.consumeAsFlow()

        fun push(event: String) = events.trySend(parse(event).jsonObject)
    }

    /** `exposed_entity` and the sync columns of `server` in memory. */
    private val cache = linkedMapOf<String, ExposedEntity>()
    private var storedRevision: String? = null
    private var syncedAt: Long? = null
    private val dao = object : ExposedEntityDao {
        override suspend fun all(serverId: String) = cache.values.filter { it.serverId == serverId }

        override fun observe(serverId: String): Flow<List<ExposedEntity>> = error("not used")

        override fun observeOne(serverId: String, entityId: String): Flow<ExposedEntity?> = error("not used")

        override suspend fun upsert(rows: List<ExposedEntity>) {
            rows.forEach { cache[it.entityId] = it }
        }

        override suspend fun revision(serverId: String) = storedRevision

        override suspend fun markSynced(serverId: String, revision: String, at: Long) {
            storedRevision = revision
            syncedAt = at
        }

        override suspend fun state(serverId: String, entityId: String) = cache[entityId]?.lastState

        override suspend fun setState(serverId: String, entityId: String, state: String) {
            writes++
            cache.computeIfPresent(entityId) { _, row -> row.copy(lastState = state) }
        }
    }

    private var writes = 0
    private var now = 100L
    private val testDispatcher = StandardTestDispatcher()
    private val live = LiveStates()
    private val reportedErrors = mutableListOf<ErrorCode>()
    private val reportedChanges = mutableListOf<Pair<List<String>, List<String>>>()
    private val disabledDao = object : EntityDisabledDao {
        override suspend fun markDisabled(id: String, ids: List<String>, at: Long) {
            ids.forEach { id ->
                cache.computeIfPresent(id) { _, row -> row.copy(disabled = true, disabledAt = row.disabledAt ?: at) }
            }
        }

        override suspend fun clearDisabledExcept(id: String, keep: List<String>) {
            cache.keys.filter { it !in keep }
                .forEach { cache.computeIfPresent(it) { _, row -> row.copy(disabled = false, disabledAt = null) } }
        }
    }
    private val sync = EntitySync(
        dao,
        json,
        DefaultErrorFactory(),
        { error, _ -> reportedErrors += error.code },
        { _, added, removed -> reportedChanges += added to removed },
        live,
        DisabledSync(dao, disabledDao, json, DefaultErrorFactory()),
        testDispatcher,
        FLUSH_MS,
    ) { now }
    private val channel = FakeChannel()

    private fun parse(text: String): JsonElement = json.parseToJsonElement(text)

    private fun descriptor(id: String, state: String, name: String = id, disabled: Boolean = false) =
        """{"entity_id":"$id","domain":"${id.substringBefore('.')}","name":"$name","state":"$state",""" +
            """"attributes":{},"disabled":$disabled,"last_changed":"2026-09-26T07:12:03+00:00"}"""

    /** The live state, as the tiles see it. */
    private fun stateOf(id: String): EntityState? = live.current("s1", id)

    /** The state in the cache row, as the offline display sees it. */
    private fun cachedStateOf(id: String): EntityState? =
        cache[id]?.lastState?.let { json.decodeFromString(EntityState.serializer(), it) }

    private fun TestScope.follow() {
        backgroundScope.launch { sync.follow("s1", channel) }
        runCurrent()
    }

    @Test
    fun `first sync stores the list, the revision and the states`() = runTest(testDispatcher) {
        follow()
        assertEquals(listOf("sensor.b", "switch.a"), cache.keys.sorted())
        assertEquals("r1", storedRevision)
        assertEquals(100L, syncedAt)
        assertEquals(SyncResult(added = listOf("sensor.b", "switch.a")), sync.status.value.result)
        assertEquals(emptyList<Pair<List<String>, List<String>>>(), reportedChanges)
        assertEquals(EntityState("on", lastChanged = 1790406723000, lastUpdated = 1790406723000), stateOf("switch.a"))
        assertEquals(stateOf("switch.a"), cachedStateOf("switch.a"))
    }

    @Test
    fun `the first sync caches the deactivation of the list with its time`() = runTest(testDispatcher) {
        channel.listed = listOf(descriptor("switch.a", "on", disabled = true), descriptor("sensor.b", "21"))
        follow()
        assertEquals(true, cache["switch.a"]?.disabled)
        assertEquals(100L, cache["switch.a"]?.disabledAt)
        assertEquals(false, cache["sensor.b"]?.disabled)
    }

    @Test
    fun `a changed revision keeps the time of a deactivation that was already cached`() = runTest(testDispatcher) {
        channel.listed = listOf(descriptor("switch.a", "on", disabled = true), descriptor("sensor.b", "21"))
        follow()
        now = 500L
        channel.revision = "r2"
        channel.listed = listOf(
            descriptor("switch.a", "on", name = "Lamp", disabled = true),
            descriptor("sensor.b", "21"),
        )
        channel.push("""{"exposure_changed":{"revision":"r2"}}""")
        runCurrent()
        assertEquals("Lamp", cache["switch.a"]?.haName)
        assertEquals(100L, cache["switch.a"]?.disabledAt)
    }

    @Test
    fun `disabled_changed replaces the deactivated entities`() = runTest(testDispatcher) {
        follow()
        channel.push("""{"disabled_changed":{"disabled":["sensor.b"]}}""")
        runCurrent()
        assertEquals(true, cache["sensor.b"]?.disabled)
        assertEquals(false, cache["switch.a"]?.disabled)
        channel.push("""{"disabled_changed":{"disabled":[]}}""")
        runCurrent()
        assertEquals(false, cache["sensor.b"]?.disabled)
    }

    @Test
    fun `an unchanged revision still reads the list when the bridge reports entity_disable`() =
        runTest(testDispatcher) {
            channel.features = setOf("entity_disable")
            follow()
            channel.listed = listOf(descriptor("switch.a", "on", disabled = true), descriptor("sensor.b", "21"))
            sync.recheck()
            runCurrent()
            assertEquals(true, cache["switch.a"]?.disabled)
            assertEquals(2, channel.requests.count { it == "haac_bridge/entities/list" })
        }

    @Test
    fun `an unchanged revision skips the entity list`() = runTest(testDispatcher) {
        storedRevision = "r1"
        follow()
        assertEquals(listOf("haac_bridge/exposure/revision"), channel.requests)
        assertEquals(SyncResult(), sync.status.value.result)
    }

    @Test
    fun `live states are applied in memory without a write to the cache`() = runTest(testDispatcher) {
        follow()
        writes = 0
        channel.push("""{"a":{"switch.a":{"s":"off","a":{"device_class":"outlet"},"lc":2.0}}}""")
        channel.push("""{"c":{"switch.a":{"+":{"s":"standby","lc":3.0}},"unknown.x":{"+":{"s":"1"}}}}""")
        runCurrent()
        assertEquals("standby", stateOf("switch.a")?.state)
        assertEquals(3000L, stateOf("switch.a")?.lastChanged)
        assertEquals("outlet", stateOf("switch.a")?.attributes?.get("device_class")?.jsonPrimitive?.content)
        assertNull(live.current("s1", "unknown.x"))
        assertEquals(0, writes)
        assertEquals("on", cachedStateOf("switch.a")?.state)
    }

    @Test
    fun `changed states reach the cache in one batch after the flush interval`() = runTest(testDispatcher) {
        follow()
        writes = 0
        repeat(10) { channel.push("""{"c":{"switch.a":{"+":{"s":"s$it"}},"sensor.b":{"+":{"s":"v$it"}}}}""") }
        runCurrent()
        assertEquals(0, writes)
        advanceTimeBy(FLUSH_MS + 1)
        assertEquals(2, writes)
        assertEquals("s9", cachedStateOf("switch.a")?.state)
        assertEquals("v9", cachedStateOf("sensor.b")?.state)
        advanceTimeBy(FLUSH_MS)
        assertEquals(2, writes)
    }

    @Test
    fun `200 entities with 10 events a second cause no write in a flush interval`() = runTest(testDispatcher) {
        channel.listed = List(ENTITIES) { descriptor("sensor.e$it", "0") }
        follow()
        writes = 0
        repeat(EVENTS) { n -> channel.push("""{"c":{"sensor.e${n % ENTITIES}":{"+":{"s":"$n"}}}}""") }
        runCurrent()
        assertEquals("49", stateOf("sensor.e49")?.state)
        assertEquals(0, writes)
    }

    @Test
    fun `the states are written when the connection ends`() = runTest(testDispatcher) {
        val job = backgroundScope.launch { sync.follow("s1", channel) }
        runCurrent()
        channel.push("""{"c":{"switch.a":{"+":{"s":"off"}}}}""")
        runCurrent()
        assertEquals("on", cachedStateOf("switch.a")?.state)
        job.cancel()
        runCurrent()
        assertEquals("off", cachedStateOf("switch.a")?.state)
    }

    @Test
    fun `the cached states are the starting point of a new connection`() = runTest(testDispatcher) {
        storedRevision = "r1"
        cache["switch.a"] = ExposedEntity("s1", "switch.a", "switch", "A", lastState = """{"state":"on"}""")
        follow()
        assertEquals("on", stateOf("switch.a")?.state)
        channel.push("""{"c":{"switch.a":{"+":{"s":"off"}}}}""")
        runCurrent()
        assertEquals("off", stateOf("switch.a")?.state)
    }

    @Test
    fun `exposure_changed with a new revision withdraws and adds entities`() = runTest(testDispatcher) {
        follow()
        now = 200
        channel.revision = "r2"
        channel.listed = listOf(descriptor("switch.a", "on"), descriptor("climate.c", "heat"))
        channel.push("""{"r":["sensor.b"]}""")
        channel.push("""{"exposure_changed":{"revision":"r2"}}""")
        runCurrent()
        assertEquals(SyncResult(added = listOf("climate.c"), removed = listOf("sensor.b")), sync.status.value.result)
        assertEquals(EntityStatus.WITHDRAWN, cache["sensor.b"]?.status)
        assertEquals(200L, cache["sensor.b"]?.withdrawnAt)
        assertEquals("21", stateOf("sensor.b")?.state)
        assertEquals("r2", storedRevision)
        assertEquals(listOf(listOf("climate.c") to listOf("sensor.b")), reportedChanges)
    }

    @Test
    fun `a recheck runs the revision check on the running connection`() = runTest(testDispatcher) {
        sync.recheck() // Before the sync: answered by its own revision check.
        follow()
        channel.requests.clear()
        sync.recheck()
        runCurrent()
        assertEquals(listOf("haac_bridge/exposure/revision"), channel.requests)
    }

    @Test
    fun `errors are kept in the status unless the connection ended`() = runTest(testDispatcher) {
        channel.failure = NetworkException(ErrorCode.NET_CONNECTION_LOST)
        channel.isOpen = false
        follow()
        assertNull(sync.status.value.error)
        assertEquals(emptyList<ErrorCode>(), reportedErrors)
        channel.isOpen = true
        channel.failure = null
        channel.listed = listOf("""{"entity_id":"switch.a"}""")
        follow()
        assertEquals(ErrorCode.APP_UNEXPECTED, sync.status.value.error?.code)
        assertEquals(listOf(ErrorCode.APP_UNEXPECTED), reportedErrors)
    }

    private fun TestScope.followDemo(world: DemoWorld = demoWorld()): BridgeConnection {
        val connection = demoConnection(world)
        backgroundScope.launch { sync.follow("s1", connection) }
        runCurrent()
        return connection
    }

    @Test
    fun `the first sync of the demo bridge caches ten entities with names and states`() = runTest(testDispatcher) {
        followDemo()
        assertEquals(10, cache.size)
        assertEquals("Living room light", cache.getValue("switch.demo_living_room_light").haName)
        assertEquals("Living room", cache.getValue("switch.demo_socket").area)
        assertEquals("\u00b0C", cache.getValue("sensor.demo_temperature").unit)
        assertEquals("total_increasing", cache.getValue("sensor.demo_energy").stateClass)
        assertEquals(1, cache.getValue("climate.demo_thermostat").supportedFeatures)
        assertEquals("heat", stateOf("climate.demo_thermostat")?.state)
        assertEquals("on", stateOf("switch.demo_socket")?.state)
        assertEquals("smoke", cache.getValue("binary_sensor.demo_smoke_detector").deviceClass)
        assertEquals("off", stateOf("binary_sensor.demo_front_door")?.state)
        assertNull(sync.status.value.error)
        assertEquals(emptyList<Pair<List<String>, List<String>>>(), reportedChanges)
    }

    @Test
    fun `service calls change the state through the demo subscription`() = runTest(testDispatcher) {
        val connection = followDemo()
        val calls = DefaultServiceCallFactory()
        val socket = calls.create(cache.getValue("switch.demo_socket"), ControlRequest.SwitchTo(false))
        connection.request("haac_bridge/call_service", socket.fields())
        runCurrent()
        assertEquals("off", stateOf("switch.demo_socket")?.state)
        val thermostat = calls.create(cache.getValue("climate.demo_thermostat"), ControlRequest.SetTemperature(22.5))
        connection.request("haac_bridge/call_service", thermostat.fields())
        runCurrent()
        val state = stateOf("climate.demo_thermostat")
        assertEquals("heat", state?.state)
        assertEquals(22.5, state?.attributes?.get("temperature")?.jsonPrimitive?.doubleOrNull)
        assertEquals("heating", state?.attributes?.get("hvac_action")?.jsonPrimitive?.content)
    }

    @Test
    fun `a second connection to the same demo finds the same revision and reports nothing`() = runTest(testDispatcher) {
        val world = demoWorld()
        followDemo(world)
        val revision = storedRevision
        followDemo(world)
        assertEquals(revision, storedRevision)
        assertEquals(SyncResult(), sync.status.value.result)
        assertEquals(emptyList<Pair<List<String>, List<String>>>(), reportedChanges)
    }
}
