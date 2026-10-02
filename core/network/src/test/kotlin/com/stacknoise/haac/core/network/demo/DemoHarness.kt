package com.stacknoise.haac.core.network.demo

import com.stacknoise.haac.core.network.http.text
import com.stacknoise.haac.core.network.websocket.messageId
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The text of the string or number [key]; fails if it is missing. */
internal fun JsonObject.str(key: String): String = this[key]!!.jsonPrimitive.content

/** The object [key]; fails if it is missing. */
internal fun JsonObject.obj(key: String): JsonObject = this[key]!!.jsonObject

/** The text of the string or number [key] of an array element. */
internal fun JsonElement.str(key: String): String = jsonObject.str(key)

/** The turn-off call of the socket as the request fields of `call_service`. */
internal const val TurnOffSocket =
    """{"entity_id":"switch.demo_socket","service":"turn_off","service_data":{}}"""

/**
 * A [DemoBridge] without a socket: [request] sends one command and returns its reply, events pile up in [events].
 * The clock stands still on Friday 2026-10-02 12:00 in Berlin (10:00 UTC), so times can be asserted exactly.
 */
class DemoHarness(val store: MemoryDemoWorldStore = MemoryDemoWorldStore()) {
    /** The fixed clock of the harness. */
    val clock: Clock = Clock.fixed(Instant.parse("2026-10-02T10:00:00Z"), ZoneId.of("Europe/Berlin"))

    /** The world over [store]; a second harness over the same store reads what the first one saved. */
    val world = DemoWorld(store, Json { ignoreUnknownKeys = true }, clock)

    /** Everything the bridge sent so far. */
    val out = mutableListOf<JsonObject>()
    private val bridge = DemoBridge(world) { out += it }
    private var lastId = 0

    /** Events (`type` event) of subscription [id], in order. */
    fun events(id: Int): List<JsonObject> =
        out.filter { it.messageId == id && it.text("type") == "event" }.map { it["event"]!!.jsonObject }

    /** Sends [type] with the request [fields] (a JSON object as text) and returns the reply; [lastId] is its id. */
    fun request(type: String, fields: String = "{}"): JsonObject {
        val id = ++lastId
        val message = Json.parseToJsonElement(fields).jsonObject + mapOf(
            "id" to Json.parseToJsonElement("$id"),
            "type" to Json.parseToJsonElement("\"$type\""),
        )
        bridge.handle(JsonObject(message))
        return out.first { it.messageId == id && it.text("type") in setOf("result", "pong") }
    }

    /** The id of the last request. */
    val lastMessageId: Int get() = lastId

    /** The `result` of [reply], failing if it is an error. */
    fun result(reply: JsonObject): JsonElement {
        check(reply["success"]?.jsonPrimitive?.content == "true") { "error reply: $reply" }
        return reply["result"]!!
    }

    /** The HAB code of the error reply [reply]. */
    fun code(reply: JsonObject): String? = reply["error"]?.jsonObject?.text("code")

    /** The result object of [type] with [fields]. */
    fun ask(type: String, fields: String = "{}"): JsonObject = result(request(type, fields)).jsonObject

    /** The items of the array [key] of [obj]. */
    fun list(obj: JsonObject, key: String): JsonArray = obj[key]!!.jsonArray
}
