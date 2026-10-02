package com.stacknoise.haac.feature.schedules.data

import com.stacknoise.haac.core.database.schedule.ScheduleDao
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.core.network.connection.LiveConnection
import com.stacknoise.haac.core.network.connection.requireOpen
import com.stacknoise.haac.core.network.http.decodeOrUnexpected
import com.stacknoise.haac.feature.schedules.domain.ScheduleItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Changes schedules on the bridge (concept 11.2, 19.4). Every command needs the open connection (HAAC-NET-002
 * otherwise) and changes the cache only after the bridge answered, so the cache never shows an edit the server did
 * not take. Bridge errors arrive as `HaacException` (18.3).
 */
@Singleton
class ScheduleCommands @Inject constructor(
    private val live: LiveConnection,
    private val schedules: ScheduleDao,
    private val json: Json,
    private val errors: ErrorFactory,
) {
    /** Turns [item] on or off (`enabled`) and caches the schedule the bridge answers with. */
    suspend fun setEnabled(serverId: String, item: ScheduleItem, enabled: Boolean) {
        val fields = buildJsonObject {
            put("schedule_id", item.id)
            put("updated_at", item.updatedAt)
            put("enabled", enabled)
        }
        val reply = live.requireOpen().request(UPDATE, fields)
        val updated = json.decodeOrUnexpected(ScheduleDescriptor.serializer(), reply)
        val scope = if (item.own) ScheduleList.SCOPE_OWN else SCOPE_ALL
        errors.database { schedules.upsert(listOf(updated.toRow(serverId, scope, System.currentTimeMillis()))) }
    }

    /** Deletes schedule [scheduleId] on the bridge, then from the cache, so the sync does not report it as removed. */
    suspend fun delete(serverId: String, scheduleId: String) {
        live.requireOpen().request(DELETE, buildJsonObject { put("schedule_id", scheduleId) })
        errors.database { schedules.delete(serverId, listOf(scheduleId)) }
    }

    /** Bridge commands (concept 11.2) and the scope of a list that holds foreign schedules too. */
    private companion object {
        const val UPDATE = "haac_bridge/schedules/update"
        const val DELETE = "haac_bridge/schedules/delete"
        const val SCOPE_ALL = "all"
    }
}
