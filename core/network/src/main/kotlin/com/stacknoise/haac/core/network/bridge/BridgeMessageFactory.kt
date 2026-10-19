package com.stacknoise.haac.core.network.bridge

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** A WebSocket command with its message id. */
data class BridgeCommand(val id: Int, val json: String)

/** Builds the messages of HA's WebSocket API with increasing ids (concept 11, 17.2). */
interface BridgeMessageFactory {
    /** The `auth` message; contains the access token and must never be logged. */
    fun auth(accessToken: String): String

    /** A command such as `haac_bridge/info` with the next message id and the request [fields]. */
    fun command(type: String, fields: JsonObject = NO_FIELDS): BridgeCommand

    /** `haac_bridge/entities/set_disabled`: deactivates or activates [entityId] for the caller (concept 7.5). */
    fun setDisabled(entityId: String, disabled: Boolean): BridgeCommand

    /** `haac_bridge/schedules/preview`: the next runs of a draft with the `when` and `repeat` objects (19.10). */
    fun preview(trigger: JsonObject, repeat: JsonObject): BridgeCommand

    /** `haac_bridge/schedules/runs`: the caller's run log entries after [afterSeq] of log [logId] (19.11). */
    fun runs(logId: String? = null, afterSeq: Long? = null): BridgeCommand

    /** Shared constants. */
    companion object {
        /** A command without request fields. */
        val NO_FIELDS = JsonObject(emptyMap())
    }
}

/** Message ids grow per factory; HA only requires them to increase on one connection. */
class DefaultBridgeMessageFactory @Inject constructor() : BridgeMessageFactory {
    private val ids = AtomicInteger()

    /** `{"type": "auth", "access_token": …}`. */
    override fun auth(accessToken: String): String = buildJsonObject {
        put("type", "auth")
        put("access_token", accessToken)
    }.toString()

    /** `{"id": n, "type": …, <fields>}`; [fields] cannot replace `id` or `type`. */
    override fun command(type: String, fields: JsonObject): BridgeCommand {
        val id = ids.incrementAndGet()
        val json = buildJsonObject {
            fields.forEach { (key, value) -> put(key, value) }
            put("id", id)
            put("type", type)
        }
        return BridgeCommand(id, json.toString())
    }

    /** `{"type": "haac_bridge/entities/set_disabled", "entity_id": …, "disabled": …}`. */
    override fun setDisabled(entityId: String, disabled: Boolean): BridgeCommand =
        command(
            "haac_bridge/entities/set_disabled",
            buildJsonObject {
                put("entity_id", entityId)
                put("disabled", disabled)
            },
        )

    /** `{"type": "haac_bridge/schedules/preview", "when": …, "repeat": …}`. */
    override fun preview(trigger: JsonObject, repeat: JsonObject): BridgeCommand =
        command(
            "haac_bridge/schedules/preview",
            buildJsonObject {
                put("when", trigger)
                put("repeat", repeat)
            },
        )

    /** `{"type": "haac_bridge/schedules/runs"}` with `log_id` and `after_seq` only when a cursor exists. */
    override fun runs(logId: String?, afterSeq: Long?): BridgeCommand =
        command(
            "haac_bridge/schedules/runs",
            buildJsonObject {
                logId?.let { put("log_id", it) }
                afterSeq?.let { put("after_seq", it) }
            },
        )
}
