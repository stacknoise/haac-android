package com.stacknoise.haac.feature.entities.disable.data

import com.stacknoise.haac.core.network.connection.LiveConnection
import com.stacknoise.haac.core.network.connection.requireOpen
import com.stacknoise.haac.core.network.http.decodeOrUnexpected
import com.stacknoise.haac.feature.entities.data.SetDisabledReply
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Deactivates and activates entities on the bridge (concept 7.5, 11.2). The command needs the open connection
 * (HAAC-NET-002 otherwise) and the cache changes only after the bridge answered, so it never shows a change the
 * server did not take. Bridge errors arrive as `HaacException` (18.3).
 */
@Singleton
class DisableCommands internal constructor(
    private val live: LiveConnection,
    private val sync: DisabledSync,
    private val json: Json,
    private val clock: () -> Long,
) {
    /** Uses the wall clock for the time of a deactivation. */
    @Inject
    constructor(live: LiveConnection, sync: DisabledSync, json: Json) :
        this(live, sync, json, System::currentTimeMillis)

    /**
     * Deactivates ([disabled] true) or activates [entityId] of instance [serverId]; the cache follows the bridge's
     * full list. Returns the ids of the schedules the bridge switched off or on again.
     */
    suspend fun set(serverId: String, entityId: String, disabled: Boolean): List<String> {
        val fields = buildJsonObject {
            put("entity_id", entityId)
            put("disabled", disabled)
        }
        val reply = live.requireOpen().request(SET_DISABLED, fields)
        val answer = json.decodeOrUnexpected(SetDisabledReply.serializer(), reply)
        sync.apply(serverId, answer.disabled, clock())
        return answer.schedulesChanged
    }

    /** The command of concept 11.2. */
    private companion object {
        const val SET_DISABLED = "haac_bridge/entities/set_disabled"
    }
}
