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
    suspend fun setEnabled(serverId: String, item: ScheduleItem, enabled: Boolean) =
        setFlag(serverId, item, "enabled", enabled)

    /** Turns the run notifications of [item] on or off (`notify`, concept 19.11); only its owner may. */
    suspend fun setNotify(serverId: String, item: ScheduleItem, notify: Boolean) =
        setFlag(serverId, item, "notify", notify)

    /** Updates the boolean [field] of [item] and caches the schedule the bridge answers with. */
    private suspend fun setFlag(serverId: String, item: ScheduleItem, field: String, value: Boolean) {
        val fields = buildJsonObject {
            put("schedule_id", item.id)
            put("updated_at", item.updatedAt)
            put(field, value)
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
        val channel = live.requireOpen()
        val features = ScheduleFeatures.of(channel.features)
        val reply = if (original == null) {
            channel.request(CREATE, ScheduleFields.create(draft, features))
        } else {
            val fields = ScheduleFields.changes(ScheduleDraft.of(original), draft, original.own, features)
            if (fields.isEmpty()) return
            val request = buildJsonObject {
                put("schedule_id", original.id)
                put("updated_at", original.updatedAt)
                fields.forEach { (key, value) -> put(key, value) }
            }
            channel.request(UPDATE, request)
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
