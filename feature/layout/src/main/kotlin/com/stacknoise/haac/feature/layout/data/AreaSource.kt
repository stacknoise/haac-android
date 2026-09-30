package com.stacknoise.haac.feature.layout.data

import com.stacknoise.haac.core.error.UnexpectedException
import com.stacknoise.haac.core.network.connection.LiveConnection
import com.stacknoise.haac.core.network.connection.requireOpen
import com.stacknoise.haac.feature.layout.domain.HaArea
import com.stacknoise.haac.feature.layout.domain.HaAreas
import com.stacknoise.haac.feature.layout.domain.HaFloor
import javax.inject.Inject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Where the floors and areas of Home Assistant come from (concept 6.3). */
fun interface AreaSource {
    /** The floors and areas that hold entities exposed to the user. */
    suspend fun load(): HaAreas
}

/** Reads `haac_bridge/areas` over the live connection; HAAC-NET-002 without a connection (concept 11.2). */
class BridgeAreaSource @Inject constructor(private val live: LiveConnection) : AreaSource {
    /** Sends the command and parses the answer. */
    override suspend fun load(): HaAreas = parseAreas(live.requireOpen().request(COMMAND))

    /** Command name. */
    private companion object {
        const val COMMAND = "haac_bridge/areas"
    }
}

/** Parses the answer of `haac_bridge/areas`; a malformed answer is an unexpected error. */
internal fun parseAreas(result: JsonElement): HaAreas {
    val body = result as? JsonObject ?: throw UnexpectedException()
    val floors = body.list("floors").map { item ->
        val floor = item.jsonObject
        HaFloor(floor.text("floor_id"), floor.text("name"), (floor["level"] as? JsonPrimitive)?.intOrNull)
    }
    val areas = body.list("areas").map { item ->
        val area = item.jsonObject
        val count = (area["entity_count"] as? JsonPrimitive)?.intOrNull ?: 0
        HaArea(area.text("area_id"), area.text("name"), (area["floor_id"] as? JsonPrimitive)?.contentOrNull, count)
    }
    return HaAreas(floors, areas)
}

/** The array [key], or none. */
private fun JsonObject.list(key: String): JsonArray = this[key] as? JsonArray ?: JsonArray(emptyList())

/** The string [key]; a missing one is an unexpected error. */
private fun JsonObject.text(key: String): String =
    this[key]?.jsonPrimitive?.contentOrNull ?: throw UnexpectedException()
