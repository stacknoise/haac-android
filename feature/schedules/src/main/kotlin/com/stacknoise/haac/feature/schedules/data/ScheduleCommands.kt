package com.stacknoise.haac.feature.schedules.data

import com.stacknoise.haac.core.database.schedule.ScheduleDao
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.core.network.connection.LiveConnection
import com.stacknoise.haac.core.network.connection.requireOpen
import com.stacknoise.haac.core.network.http.decodeOrUnexpected
import com.stacknoise.haac.feature.schedules.domain.ScheduleDraft
import com.stacknoise.haac.feature.schedules.domain.ScheduleItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
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
        cache(serverId, live.requireOpen().request(UPDATE, fields), item.own)
    }

    /** Writes the schedule the bridge answered with into the cache; [own] stands in for a bridge without `own`. */
    private suspend fun cache(serverId: String, reply: JsonElement, own: Boolean) {
        val updated = json.decodeOrUnexpected(ScheduleDescriptor.serializer(), reply)
        val scope = if (own) ScheduleList.SCOPE_OWN else SCOPE_ALL
        errors.database { schedules.upsert(listOf(updated.toRow(serverId, scope, System.currentTimeMillis()))) }
    }

    /**
     * Saves [draft]: a new schedule when [original] is null, else the changes against [original]; the bridge answers
     * with the schedule, which is cached. Nothing is sent if nothing changed. An admin editing a foreign schedule
     * never sends the entities (concept 19.4).
     */
    suspend fun save(serverId: String, original: ScheduleItem?, draft: ScheduleDraft) {
        val reply = if (original == null) {
            live.requireOpen().request(CREATE, ScheduleFields.create(draft))
        } else {
            val fields = ScheduleFields.changes(ScheduleDraft.of(original), draft, withEntities = original.own)
            if (fields.isEmpty()) return
            val request = buildJsonObject {
                put("schedule_id", original.id)
                put("updated_at", original.updatedAt)
                fields.forEach { (key, value) -> put(key, value) }
            }
            live.requireOpen().request(UPDATE, request)
        }
        cache(serverId, reply, own = original?.own ?: true)
    }

    /** Deletes schedule [scheduleId] on the bridge, then from the cache, so the sync does not report it as removed. */
    suspend fun delete(serverId: String, scheduleId: String) {
        live.requireOpen().request(DELETE, buildJsonObject { put("schedule_id", scheduleId) })
        errors.database { schedules.delete(serverId, listOf(scheduleId)) }
    }

    /** Bridge commands (concept 11.2) and the scope of a list that holds foreign schedules too. */
    private companion object {
        const val CREATE = "haac_bridge/schedules/create"
        const val UPDATE = "haac_bridge/schedules/update"
        const val DELETE = "haac_bridge/schedules/delete"
        const val SCOPE_ALL = "all"
    }
}
