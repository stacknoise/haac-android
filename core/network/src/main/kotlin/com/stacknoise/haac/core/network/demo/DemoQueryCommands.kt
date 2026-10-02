package com.stacknoise.haac.core.network.demo

import com.stacknoise.haac.core.network.bridge.BridgeInfo
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/** The entities and the period (UTC) of a `history` or `statistics` request. */
private data class SeriesRequest(val ids: List<String>, val start: Instant, val end: Instant)

/**
 * The read-only commands of the demo bridge (concept 11.2): `info`, `exposure/revision`, `entities/list`, `areas`,
 * `history` and `statistics`; every reply goes to [emit].
 */
internal class DemoQueryCommands(
    private val world: DemoWorld,
    private val history: DemoHistory,
    private val emit: (JsonObject) -> Unit,
) {
    /** Answers [type] for message [id] and returns true; false if [type] is not a query. */
    fun handle(type: String, id: Int, message: JsonObject): Boolean {
        when (type) {
            "haac_bridge/info" -> emit(DemoReplies.result(id, info()))
            "haac_bridge/exposure/revision" -> emit(DemoReplies.result(id, revision()))
            "haac_bridge/entities/list" -> emit(DemoReplies.result(id, entities()))
            "haac_bridge/areas" -> emit(DemoReplies.result(id, areas()))
            "haac_bridge/history" -> series(id, message, statistics = false)
            "haac_bridge/statistics" -> series(id, message, statistics = true)
            else -> return false
        }
        return true
    }

    /** The result of `info`: the bridge info with its optional fields (`urls`, `features`) always written. */
    private fun info(): JsonElement = json.encodeToJsonElement(BridgeInfo.serializer(), DemoInstance.info)

    /** The result of `exposure/revision`: the revision of the entity set and the number of entities. */
    private fun revision(): JsonObject {
        val entities = world.snapshot().entities
        return buildJsonObject {
            put("revision", DemoEntityWire.revision(entities))
            put("entity_count", entities.size)
        }
    }

    /** The result of `entities/list`: the revision and one descriptor per entity, with the name of its area. */
    private fun entities(): JsonObject {
        val data = world.snapshot()
        val areas = data.areas.associate { it.id to it.name }
        return buildJsonObject {
            put("revision", DemoEntityWire.revision(data.entities))
            put("entities", JsonArray(data.entities.map { DemoEntityWire.descriptor(it, areas[it.areaId]) }))
        }
    }

    /** The result of `areas`: the areas that hold an entity, with their count, and the floors of those areas. */
    private fun areas(): JsonObject {
        val data = world.snapshot()
        val counts = data.entities.groupingBy { it.areaId }.eachCount()
        val used = data.areas.filter { (counts[it.id] ?: 0) > 0 }
        val floors = data.floors.filter { floor -> used.any { it.floorId == floor.id } }
        return buildJsonObject {
            put(
                "floors",
                JsonArray(
                    floors.map {
                        buildJsonObject {
                            put("floor_id", it.id)
                            put("name", it.name)
                            put("level", it.level)
                        }
                    },
                ),
            )
            put(
                "areas",
                JsonArray(
                    used.map {
                        buildJsonObject {
                            put("area_id", it.id)
                            put("name", it.name)
                            put("floor_id", it.floorId)
                            put("entity_count", counts[it.id] ?: 0)
                        }
                    },
                ),
            )
        }
    }

    /** `history` or `statistics`: HAB-WS-001 for bad fields; entities that do not exist are left out. */
    private fun series(id: Int, message: JsonObject, statistics: Boolean) {
        val request = seriesRequest(message)
        if (request == null) {
            emit(DemoReplies.error(id, DemoCodes.WS_INVALID_REQUEST, "Invalid history request"))
            return
        }
        val (ids, start, end) = request
        val listed = world.snapshot().entities.filter { it.entityId in ids }
        val entities = if (start.isAfter(world.clock.instant())) emptyList() else listed
        val rows = entities.associate { entity ->
            entity.entityId to if (statistics) {
                history.statistics(entity, start.toEpochMilli(), end.toEpochMilli(), period(message), types(message))
            } else {
                history.history(entity, start.toEpochMilli(), end.toEpochMilli(), minimal(message))
            }
        }
        emit(DemoReplies.result(id, JsonObject(rows)))
    }

    /** The entity IDs and the period of a history request; null if a field is missing or the period is reversed. */
    private fun seriesRequest(message: JsonObject): SeriesRequest? {
        val ids = (message["entity_ids"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        val start = time(message["start"])
        val end = if ("end" in message) time(message["end"]) else world.clock.instant()
        if (ids == null || start == null || end == null) return null
        return if (end.isBefore(start)) null else SeriesRequest(ids, start, end)
    }

    /** The ISO time [element], or null if it is not one. */
    private fun time(element: JsonElement?): Instant? = try {
        (element as? JsonPrimitive)?.contentOrNull?.let(Instant::parse)
    } catch (_: DateTimeParseException) {
        null
    }

    /** The statistics `period`; hour if none is given. */
    private fun period(message: JsonObject): String = (message["period"] as? JsonPrimitive)?.contentOrNull ?: "hour"

    /** The statistics `types`; all four if none are given. */
    private fun types(message: JsonObject): List<String> =
        (message["types"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            ?: listOf("mean", "min", "max", "sum")

    /** True if the request asks for a minimal history response. */
    private fun minimal(message: JsonObject): Boolean =
        (message["minimal_response"] as? JsonPrimitive)?.booleanOrNull == true

    /** The JSON of the bridge info with its optional fields (`urls`, `features`) always written. */
    private companion object {
        val json = Json { encodeDefaults = true }
    }
}
