package com.stacknoise.haac.feature.schedules.data

import com.stacknoise.haac.core.database.schedule.ScheduleDao
import com.stacknoise.haac.core.network.connection.LiveConnection
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Whether the Schedules tab is shown (concept 19.4, 19.7): the bridge reports the feature, or schedules are cached. */
@Singleton
class ScheduleAvailability @Inject constructor(
    private val live: LiveConnection,
    private val schedules: ScheduleDao,
) {
    /**
     * True for instance [serverId] while the open connection reports the feature `schedules`, and, so the tab does
     * not vanish while offline, while schedules of that instance are cached.
     */
    fun shown(serverId: String): Flow<Boolean> =
        combine(live.connection.map { it?.features.orEmpty() }, schedules.observe(serverId)) { features, cached ->
            tabShown(features, cached.isNotEmpty())
        }
}

/** The tab is shown if the bridge reports `schedules` or the instance still has cached schedules. */
fun tabShown(features: Set<String>, hasCached: Boolean): Boolean = "schedules" in features || hasCached
