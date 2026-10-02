package com.stacknoise.haac.feature.schedules.data

import com.stacknoise.haac.core.database.assignment.EntityAliasDao
import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.database.entity.byEntityId
import com.stacknoise.haac.core.database.entity.displayName
import com.stacknoise.haac.core.database.schedule.ScheduleDao
import com.stacknoise.haac.feature.schedules.domain.ScheduleItem
import com.stacknoise.haac.feature.schedules.domain.toItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** A schedule with the display names of its entities, in the order of the schedule (concept 7.3, 19.7). */
data class ScheduleView(val item: ScheduleItem, val entityNames: List<String>)

/** Reads the cached schedules of an instance for the screens (concept 19.7); the cache is filled by [ScheduleSync]. */
@Singleton
class ScheduleRepository @Inject constructor(
    private val schedules: ScheduleDao,
    private val entities: ExposedEntityDao,
    private val aliases: EntityAliasDao,
) {
    /** The schedules of instance [serverId], sorted by name and updated on every change. */
    fun views(serverId: String): Flow<List<ScheduleView>> =
        combine(schedules.observe(serverId), names(serverId)) { rows, names ->
            rows.map { row -> row.toItem().let { ScheduleView(it, it.entityIds.map { id -> names[id] ?: id }) } }
        }

    /** One schedule of [serverId]; null once it is gone. */
    fun view(serverId: String, scheduleId: String): Flow<ScheduleView?> =
        combine(schedules.observe(serverId, scheduleId), names(serverId)) { row, names ->
            row?.toItem()?.let { ScheduleView(it, it.entityIds.map { id -> names[id] ?: id }) }
        }

    /** The display names of the entities of [serverId] by `entity_id`, with the local names (concept 7.3). */
    private fun names(serverId: String): Flow<Map<String, String>> =
        combine(entities.observe(serverId), aliases.observe(serverId)) { rows, names ->
            val local = names.byEntityId()
            rows.associate { it.entityId to it.displayName(local[it.entityId]) }
        }
}
