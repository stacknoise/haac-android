package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.network.connection.BridgeChannel
import com.stacknoise.haac.core.network.connection.LiveConnection
import com.stacknoise.haac.feature.entities.domain.DefaultHistoryChartFactory
import com.stacknoise.haac.feature.entities.domain.HistoryChart
import com.stacknoise.haac.feature.entities.domain.HistoryPreset
import com.stacknoise.haac.feature.entities.domain.HistoryWindow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** [EntityHistory]: the request it sends and the chart it returns (concept 8.1, 11.2). */
class EntityHistoryTest {
    private val rows = listOf(
        ExposedEntity("s1", "switch.lamp", "switch", "Lamp"),
        ExposedEntity("s1", "sensor.e", "sensor", "Energy", unit = "kWh", stateClass = "total"),
    )

    /** Only `all` is read; writes never happen here. */
    private val cache = object : ExposedEntityDao {
        override suspend fun all(serverId: String): List<ExposedEntity> = rows

        override fun observe(serverId: String): Flow<List<ExposedEntity>> = flowOf(rows)

        override suspend fun revision(serverId: String): String? = error("not read")

        override suspend fun state(serverId: String, entityId: String): String? = error("not read")

        override suspend fun upsert(rows: List<ExposedEntity>) = error("not written")

        override suspend fun markSynced(serverId: String, revision: String, at: Long) = error("not written")

        override suspend fun setState(serverId: String, entityId: String, state: String) = error("not written")
    }

    /** Answers every command with [answer] and keeps the last request. */
    private val bridge = object : BridgeChannel {
        override val isOpen = true
        var answer = "{}"
        var sent: Pair<String, JsonObject>? = null

        override suspend fun request(type: String, fields: JsonObject): JsonElement {
            sent = type to fields
            return Json.parseToJsonElement(answer)
        }

        override fun subscribe(type: String, fields: JsonObject): Flow<JsonObject> = error("not used")
    }

    private val live = object : LiveConnection {
        override val connection = MutableStateFlow<BridgeChannel?>(bridge)
    }

    private val history = EntityHistory(live, cache, DefaultHistoryChartFactory(), DefaultErrorFactory())
    private val window = HistoryWindow(start = 1_790_467_200_000, end = 1_790_553_600_000, HistoryPreset.DAY)

    @Test
    fun `a switch asks for a minimal history`() = runTest {
        bridge.answer = """{"switch.lamp":[{"s":"on","lu":1790467200.0}]}"""
        val chart = history.load("s1", "switch.lamp", window)
        val (type, fields) = bridge.sent!!
        assertEquals("haac_bridge/history", type)
        assertEquals(
            """{"entity_ids":["switch.lamp"],"end":"2026-09-28T00:00:00Z","start":"2026-09-27T00:00:00Z",""" +
                """"minimal_response":true}""",
            fields.toString(),
        )
        assertEquals(1, (chart as HistoryChart.Timeline).segments.size)
    }

    @Test
    fun `a counter asks for sums from one period earlier`() = runTest {
        history.load("s1", "sensor.e", window)
        val (type, fields) = bridge.sent!!
        assertEquals("haac_bridge/statistics", type)
        assertEquals(
            """{"entity_ids":["sensor.e"],"end":"2026-09-28T00:00:00Z","start":"2026-09-26T23:00:00Z",""" +
                """"period":"hour","types":["sum"]}""",
            fields.toString(),
        )
    }

    @Test
    fun `without a connection loading fails with HAAC-NET-002`() = runTest {
        live.connection.value = null
        val error = runCatching { history.load("s1", "switch.lamp", window) }.exceptionOrNull() as? HaacException
        assertEquals(ErrorCode.NET_CONNECTION_LOST, error?.code)
    }
}
