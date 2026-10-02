package com.stacknoise.haac.feature.schedules.data

import com.stacknoise.haac.core.database.schedule.ScheduleDao
import com.stacknoise.haac.core.database.schedule.ScheduleEntity
import kotlinx.coroutines.flow.Flow

/** The `schedule` table in memory, by schedule id; the observing queries are not needed by the data tests. */
internal class FakeScheduleDao(val cache: MutableMap<String, ScheduleEntity> = linkedMapOf()) : ScheduleDao {
    override suspend fun all(serverId: String) = cache.values.toList()

    override fun observe(serverId: String): Flow<List<ScheduleEntity>> = error("not used")

    override fun observe(serverId: String, scheduleId: String): Flow<ScheduleEntity?> = error("not used")

    override suspend fun upsert(rows: List<ScheduleEntity>) {
        rows.forEach { cache[it.scheduleId] = it }
    }

    override suspend fun delete(serverId: String, scheduleIds: List<String>) {
        scheduleIds.forEach(cache::remove)
    }
}
