package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.UnexpectedException
import com.stacknoise.haac.core.network.connection.LiveConnection
import com.stacknoise.haac.feature.entities.domain.HistoryChart
import com.stacknoise.haac.feature.entities.domain.HistoryChartFactory
import com.stacknoise.haac.feature.entities.domain.HistoryQuery
import com.stacknoise.haac.feature.entities.domain.HistoryWindow
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Reads the history of one entity over the live connection (concept 8.1, 11.2): `haac_bridge/history` or
 * `haac_bridge/statistics`, as [HistoryChartFactory] decides, and turns the answer into a chart.
 */
class EntityHistory @Inject constructor(
    private val live: LiveConnection,
    private val entities: ExposedEntityDao,
    private val charts: HistoryChartFactory,
    private val errors: ErrorFactory,
) {
    /**
     * The chart of [entityId] of instance [serverId] in [window]; HAAC-NET-002 without a connection,
     * HAAC-BRG-006 when the server has no recorder (HAB-HIST-001).
     */
    suspend fun load(serverId: String, entityId: String, window: HistoryWindow): HistoryChart {
        val entity = entities.require(serverId, entityId, errors)
        val channel = live.requireOpen()
        val query = charts.query(entity, window)
        val result = channel.request(command(query), fields(entityId, window, query)) as? JsonObject
            ?: throw UnexpectedException()
        return charts.create(entity, window, query, result)
    }

    /** The bridge command of [query]. */
    private fun command(query: HistoryQuery): String = when (query) {
        is HistoryQuery.States -> HISTORY
        is HistoryQuery.Statistics -> STATISTICS
    }

    /** `entity_ids`, `start` and `end` in ISO 8601 (UTC) and the fields of [query]. */
    private fun fields(entityId: String, window: HistoryWindow, query: HistoryQuery): JsonObject = buildJsonObject {
        putJsonArray("entity_ids") { add(entityId) }
        put("end", Instant.ofEpochMilli(window.end).toString())
        when (query) {
            is HistoryQuery.States -> {
                put("start", Instant.ofEpochMilli(window.start).toString())
                put("minimal_response", !query.attributes)
            }
            is HistoryQuery.Statistics -> {
                put("start", Instant.ofEpochMilli(window.start - query.lead * query.period.millis).toString())
                put("period", query.period.key)
                putJsonArray("types") { query.types.forEach { add(it) } }
            }
        }
    }

    /** Bridge commands (concept 11.2). */
    private companion object {
        const val HISTORY = "haac_bridge/history"
        const val STATISTICS = "haac_bridge/statistics"
    }
}
