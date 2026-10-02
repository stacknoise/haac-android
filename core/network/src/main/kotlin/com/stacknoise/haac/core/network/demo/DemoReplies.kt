package com.stacknoise.haac.core.network.demo

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** The message shapes of HA's WebSocket API that the bridge answers with (concept 11.3). */
internal object DemoReplies {
    /** A success reply to message [id] carrying [payload] (JSON null for an empty result). */
    fun result(id: Int, payload: JsonElement = JsonNull): JsonObject = buildJsonObject {
        put("id", id)
        put("type", "result")
        put("success", true)
        put("result", payload)
    }

    /** An error reply to message [id] whose `code` is a HAB code (or `unknown_command`). */
    fun error(id: Int, code: String, message: String): JsonObject = buildJsonObject {
        put("id", id)
        put("type", "result")
        put("success", false)
        put(
            "error",
            buildJsonObject {
                put("code", code)
                put("message", message)
            },
        )
    }

    /** The error reply for a [failed] command. */
    fun failure(id: Int, failed: DemoOutcome.Failed): JsonObject = error(id, failed.code, failed.message)

    /** An event of the subscription [id]. */
    fun event(id: Int, payload: JsonObject): JsonObject = buildJsonObject {
        put("id", id)
        put("type", "event")
        put("event", payload)
    }

    /** HA's answer to `ping`. */
    fun pong(id: Int): JsonObject = buildJsonObject {
        put("id", id)
        put("type", "pong")
    }
}
