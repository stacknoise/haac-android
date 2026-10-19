package com.stacknoise.haac.feature.entities.disable.data

import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.database.schedule.ScheduleDao
import com.stacknoise.haac.feature.entities.disable.domain.DisablePreview
import com.stacknoise.haac.feature.entities.disable.domain.disablePreview
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Tells which schedules a deactivation switches off and an activation switches on (concept 7.5, M-20, M-21). */
class DisablePreviews @Inject constructor(
    private val schedules: ScheduleDao,
    private val entities: ExposedEntityDao,
) {
    /** The preview for [entityId] of instance [serverId], updated when the cached schedules or entities change. */
    fun of(serverId: String, entityId: String): Flow<DisablePreview> =
        combine(schedules.observe(serverId), entities.observe(serverId)) { rows, cached ->
            disablePreview(entityId, rows, cached.filter { it.disabled }.mapTo(HashSet()) { it.entityId })
        }
}
