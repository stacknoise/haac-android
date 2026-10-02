package com.stacknoise.haac.feature.schedules.data

import com.stacknoise.haac.core.database.schedule.ScheduleEntity
import com.stacknoise.haac.feature.schedules.domain.WeekDays
import com.stacknoise.haac.feature.schedules.domain.isoEpochMillis
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Answer of `haac_bridge/schedules/revision` (concept 11.2, 19.4). */
@Serializable
internal class ScheduleRevision(val revision: String)

/** Answer of `haac_bridge/schedules/list` (concept 19.4): the schedules the caller can see. */
@Serializable
internal class ScheduleList(
    val revision: String,
    val scope: String = SCOPE_OWN,
    val schedules: List<ScheduleDescriptor> = emptyList(),
) {
    /** Scope of a list that holds only the caller's own schedules. */
    companion object {
        const val SCOPE_OWN = "own"
    }
}

/** The trigger of a schedule (concept 19.2); [time] is set for fixed times, [offsetMin] for sun events. */
@Serializable
internal class WhenDescriptor(
    val type: String,
    val time: String? = null,
    val days: List<Int> = emptyList(),
    @SerialName("offset_min") val offsetMin: Int? = null,
)

/** A pause the system set (concept 19.6). */
@Serializable
internal class PausedDescriptor(val reason: String, val at: String)

/** The outcome of the last run (concept 19.2). */
@Serializable
internal class LastRunDescriptor(val at: String, val result: String, val code: String? = null)

/** One schedule of `haac_bridge/schedules/list` (concept 19.2, 19.4); [own] is missing in bridges before it existed. */
@Serializable
internal class ScheduleDescriptor(
    val id: String,
    val owner: String,
    @SerialName("owner_name") val ownerName: String? = null,
    val own: Boolean? = null,
    val name: String,
    val enabled: Boolean,
    @SerialName("when") val trigger: WhenDescriptor,
    val action: String,
    val entities: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val paused: PausedDescriptor? = null,
    @SerialName("last_run") val lastRun: LastRunDescriptor? = null,
    @SerialName("next_run") val nextRun: String? = null,
) {
    /** The cache row of instance [serverId]; without [own] the schedule is the caller's if the [scope] is `own`. */
    fun toRow(serverId: String, scope: String, syncedAt: Long): ScheduleEntity = ScheduleEntity(
        serverId = serverId,
        scheduleId = id,
        owner = owner,
        ownerName = ownerName,
        own = own ?: (scope == ScheduleList.SCOPE_OWN),
        name = name,
        enabled = enabled,
        whenType = trigger.type,
        time = trigger.time,
        days = WeekDays.toMask(trigger.days),
        offsetMin = trigger.offsetMin,
        action = action,
        entityIds = Json.encodeToString(ENTITY_IDS, entities),
        createdAt = isoEpochMillis(createdAt) ?: syncedAt,
        updatedAt = updatedAt,
        pausedReason = paused?.reason,
        pausedAt = paused?.let { isoEpochMillis(it.at) },
        lastRunAt = lastRun?.let { isoEpochMillis(it.at) },
        lastRunResult = lastRun?.result,
        lastRunCode = lastRun?.code,
        nextRun = isoEpochMillis(nextRun),
        syncedAt = syncedAt,
    )

    private companion object {
        val ENTITY_IDS = ListSerializer(String.serializer())
    }
}
