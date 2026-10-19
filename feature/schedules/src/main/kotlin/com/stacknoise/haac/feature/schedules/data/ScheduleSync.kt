package com.stacknoise.haac.feature.schedules.data

import com.stacknoise.haac.core.common.sync.RunResult
import com.stacknoise.haac.core.common.sync.ScheduleChangeReporter
import com.stacknoise.haac.core.common.sync.ScheduleRun
import com.stacknoise.haac.core.database.schedule.ScheduleDao
import com.stacknoise.haac.core.database.server.RunCursorDao
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.ErrorReporter
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.core.network.connection.BridgeChannel
import com.stacknoise.haac.core.network.http.decodeOrUnexpected
import com.stacknoise.haac.feature.schedules.domain.ScheduleDiff
import com.stacknoise.haac.feature.schedules.domain.isoEpochMillis
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Sync of the schedules of the active instance (concept 19.7, analogous to the entity sync of 9.1): on every
 * established connection and on every `schedules_changed` event, if the bridge reports the feature `schedules`.
 * It checks the revision, reads the list when the revision changed, diffs by id and tells the notification list
 * about removed and newly paused schedules of the user. With the feature `schedule_runs` it also reads the run
 * log after the stored cursor (concept 19.11): the runs of schedules with `notify` become entries, and a schedule
 * whose last run deleted it is not reported as removed. It acts only on a successful answer; on errors, without
 * a connection or with a bridge without the feature the cache stays as it is.
 */
@Suppress("LongParameterList") // the DAOs, the bridge reporters and the clock, all injected
@Singleton
class ScheduleSync internal constructor(
    private val schedules: ScheduleDao,
    private val cursors: RunCursorDao,
    private val json: Json,
    private val errors: ErrorFactory,
    private val reporter: ErrorReporter,
    private val changes: ScheduleChangeReporter,
    private val clock: () -> Long,
) {
    /** Uses the wall clock for the sync time. */
    @Inject
    constructor(
        schedules: ScheduleDao,
        cursors: RunCursorDao,
        json: Json,
        errors: ErrorFactory,
        reporter: ErrorReporter,
        changes: ScheduleChangeReporter,
    ) : this(schedules, cursors, json, errors, reporter, changes, System::currentTimeMillis)

    /** The revision of the last written list per instance; it is not kept across app starts, the first sync lists. */
    private val revisions = ConcurrentHashMap<String, String>()

    private val rechecks = Channel<Unit>(Channel.CONFLATED)

    /**
     * Runs the revision check again on the running connection, e.g. after the bridge rejected an edit as changed
     * somewhere else (HAAC-SCH-004); without a connection the next one syncs anyway.
     */
    fun recheck() {
        rechecks.trySend(Unit)
    }

    /**
     * Syncs instance [serverId] over [channel] and follows `schedules_changed` until the connection ends or the
     * caller is cancelled. Returns at once if the bridge has no schedules. An ended connection is not an error
     * here: the supervisor reconnects and the next connection syncs again.
     */
    suspend fun follow(serverId: String, channel: BridgeChannel) {
        if (FEATURE !in channel.features) return
        try {
            sync(serverId, channel)
            rechecks.tryReceive() // A request from before this sync is answered by it.
            val events = channel.subscribe(SUBSCRIBE).filter { CHANGED in it }
            merge(events.map { }, rechecks.receiveAsFlow()).collect { sync(serverId, channel) }
        } catch (e: HaacException) {
            if (channel.isOpen) reporter.report(e, serverId)
        }
    }

    /** Steps 1 to 5 of concept 19.7, then the run log of 19.11; the list is applied only if both answered. */
    private suspend fun sync(serverId: String, channel: BridgeChannel) {
        val revision = json.decodeOrUnexpected(ScheduleRevision.serializer(), channel.request(REVISION)).revision
        if (revisions[serverId] == revision) return
        val list = json.decodeOrUnexpected(ScheduleList.serializer(), channel.request(LIST))
        val now = clock()
        val listed = list.schedules.map { it.toRow(serverId, list.scope, now) }
        val cached = errors.database { schedules.all(serverId) }
        val change = ScheduleDiff.compute(cached, listed)
        val runs = if (RUNS_FEATURE in channel.features) readRuns(serverId, channel) else null
        errors.database { schedules.applySync(serverId, change.rows, change.removedIds) }
        revisions[serverId] = list.revision
        val removed = change.removedNames(runs?.ranOut.orEmpty())
        if (removed.isNotEmpty()) changes.removed(serverId, removed)
        change.paused.forEach { changes.paused(serverId, it.name, it.reason) }
        runs?.let { changes.ran(serverId, it.logId, it.lastSeq, it.announced) }
    }

    /**
     * Reads the run log after the stored cursor (concept 19.11). The first sync of an instance and a changed
     * `log_id` announce nothing and only set the cursor; otherwise the entries with `notify` are announced.
     */
    private suspend fun readRuns(serverId: String, channel: BridgeChannel): RunLog {
        val cursor = errors.database { cursors.runCursor(serverId) }
        val knownLog = cursor?.logId
        val knownSeq = cursor?.seq
        val fields = buildJsonObject {
            if (knownLog != null && knownSeq != null) {
                put("log_id", knownLog)
                put("after_seq", knownSeq)
            }
        }
        val runs = json.decodeOrUnexpected(ScheduleRuns.serializer(), channel.request(RUNS, fields))
        val continues = knownLog == runs.logId && knownSeq != null
        val announced = if (continues) runs.entries.filter { it.notify }.map(::toRun) else emptyList()
        return RunLog(
            logId = runs.logId,
            lastSeq = runs.lastSeq,
            announced = announced,
            ranOut = runs.entries.filter { it.removed }.map { it.scheduleId }.toSet(),
        )
    }

    /** The run to announce for [entry]; an unreadable time falls back to the sync time, an unknown result to failed. */
    private fun toRun(entry: RunEntry) = ScheduleRun(
        scheduleId = entry.scheduleId,
        name = entry.name,
        at = isoEpochMillis(entry.at) ?: clock(),
        result = RESULTS[entry.result] ?: RunResult.FAILED,
        removed = entry.removed,
    )

    /**
     * What one read of the run log brings: the cursor ([logId], [lastSeq]), the runs to [announced] and the ids of
     * the schedules whose last run deleted them ([ranOut]), which the removal check must not report.
     */
    private class RunLog(
        val logId: String,
        val lastSeq: Long,
        val announced: List<ScheduleRun>,
        val ranOut: Set<String>,
    )

    /** Bridge commands, the feature flag and the event key (concept 11.2, 19.4). */
    private companion object {
        const val FEATURE = "schedules"
        const val REVISION = "haac_bridge/schedules/revision"
        const val LIST = "haac_bridge/schedules/list"
        const val SUBSCRIBE = "haac_bridge/subscribe_schedules"
        const val CHANGED = "schedules_changed"
        const val RUNS_FEATURE = "schedule_runs"
        const val RUNS = "haac_bridge/schedules/runs"
        val RESULTS = mapOf(
            "ok" to RunResult.OK,
            "partial" to RunResult.PARTIAL,
            "failed" to RunResult.FAILED,
            "missed" to RunResult.MISSED,
        )
    }
}
