package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.core.database.entity.EntityStatus
import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.connection.BridgeChannel
import com.stacknoise.haac.feature.entities.domain.EntityState
import com.stacknoise.haac.feature.entities.domain.SyncResult
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EntitySyncTest {
    private val json = Json { ignoreUnknownKeys = true }

    /** The bridge side: [revision] and [listed] answer the commands, [events] feed the subscription. */
    private inner class FakeChannel : BridgeChannel {
        override var isOpen = true
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
            cache.computeIfPresent(entityId) { _, row -> row.copy(lastState = state) }
        }
    }

    private var now = 100L
    private val reportedErrors = mutableListOf<ErrorCode>()
    private val reportedChanges = mutableListOf<Pair<List<String>, List<String>>>()
    private val sync = EntitySync(
        dao,
        json,
        DefaultErrorFactory(),
        { error, _ -> reportedErrors += error.code },
        { _, added, removed -> reportedChanges += added to removed },
    ) { now }
    private val channel = FakeChannel()

    private fun parse(text: String): JsonElement = json.parseToJsonElement(text)

    private fun descriptor(id: String, state: String, name: String = id) =
        """{"entity_id":"$id","domain":"${id.substringBefore('.')}","name":"$name","state":"$state",""" +
            """"attributes":{},"last_changed":"2026-09-26T07:12:03+00:00"}"""

    private fun stateOf(id: String): EntityState? =
        cache[id]?.lastState?.let { json.decodeFromString(EntityState.serializer(), it) }

    private fun TestScope.follow() {
        backgroundScope.launch { sync.follow("s1", channel) }
        runCurrent()
    }

    @Test
    fun `first sync stores the list, the revision and the states`() = runTest {
        follow()
        assertEquals(listOf("sensor.b", "switch.a"), cache.keys.sorted())
        assertEquals("r1", storedRevision)
        assertEquals(100L, syncedAt)
        assertEquals(SyncResult(added = listOf("sensor.b", "switch.a")), sync.status.value.result)
        assertEquals(emptyList<Pair<List<String>, List<String>>>(), reportedChanges)
        assertEquals(EntityState("on", lastChanged = 1790406723000, lastUpdated = 1790406723000), stateOf("switch.a"))
    }

    @Test
    fun `an unchanged revision skips the entity list`() = runTest {
        storedRevision = "r1"
        follow()
        assertEquals(listOf("haac_bridge/exposure/revision"), channel.requests)
        assertEquals(SyncResult(), sync.status.value.result)
    }

    @Test
    fun `live states update the cache`() = runTest {
        follow()
        channel.push("""{"a":{"switch.a":{"s":"off","a":{"device_class":"outlet"},"lc":2.0}}}""")
        channel.push("""{"c":{"switch.a":{"+":{"s":"on","lc":3.0}},"unknown.x":{"+":{"s":"1"}}}}""")
        runCurrent()
        assertEquals("on", stateOf("switch.a")?.state)
        assertEquals(3000L, stateOf("switch.a")?.lastChanged)
        assertEquals("outlet", stateOf("switch.a")?.attributes?.get("device_class")?.jsonPrimitive?.content)
        assertNull(cache["unknown.x"])
    }

    @Test
    fun `exposure_changed with a new revision withdraws and adds entities`() = runTest {
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
    fun `errors are kept in the status unless the connection ended`() = runTest {
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
}
