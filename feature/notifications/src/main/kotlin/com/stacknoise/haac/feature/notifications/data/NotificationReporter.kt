package com.stacknoise.haac.feature.notifications.data

import com.stacknoise.haac.core.common.dispatch.IoDispatcher
import com.stacknoise.haac.core.common.sync.EntityChangeReporter
import com.stacknoise.haac.core.common.sync.ScheduleChangeReporter
import com.stacknoise.haac.core.common.sync.ScheduleRun
import com.stacknoise.haac.core.error.ErrorReporter
import com.stacknoise.haac.core.error.HaacException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Writes errors (concept 17.4) and sync changes (9.1, 19.7, 19.11) into the notification list. */
@Singleton
class NotificationReporter internal constructor(
    private val repository: NotificationRepository,
    private val scope: CoroutineScope,
) : ErrorReporter, EntityChangeReporter, ScheduleChangeReporter {
    /** Error entries are written in the background, so reporting never blocks the caller. */
    @Inject
    constructor(repository: NotificationRepository, @IoDispatcher io: CoroutineDispatcher) :
        this(repository, CoroutineScope(SupervisorJob() + io))

    /** Adds or counts an error entry. */
    override fun report(error: HaacException, serverId: String?) {
        scope.launch {
            try {
                repository.addError(error, serverId)
            } catch (_: HaacException) {
                // The list itself cannot be written (e.g. the instance was just removed): nowhere left to report.
            }
        }
    }

    /** Adds the entries of one sync. */
    override suspend fun report(serverId: String, added: List<String>, removed: List<String>) =
        repository.addEntityChanges(serverId, added, removed)

    /** Adds one grouped entry for the removed schedules. */
    override suspend fun removed(serverId: String, names: List<String>) =
        repository.addScheduleRemoved(serverId, names)

    /** Adds the entry of a paused schedule. */
    override suspend fun paused(serverId: String, name: String, reason: String) =
        repository.addSchedulePaused(serverId, name, reason)

    /** Adds the run entries of a sync and moves the run log cursor in the same transaction. */
    override suspend fun ran(serverId: String, logId: String, lastSeq: Long, runs: List<ScheduleRun>) =
        repository.addScheduleRuns(serverId, logId, lastSeq, runs)
}
