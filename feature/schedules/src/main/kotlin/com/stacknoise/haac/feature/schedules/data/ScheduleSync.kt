package com.stacknoise.haac.feature.schedules.data

import com.stacknoise.haac.core.common.sync.ScheduleChangeReporter
import com.stacknoise.haac.core.database.schedule.ScheduleDao
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.ErrorReporter
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.core.network.connection.BridgeChannel
import com.stacknoise.haac.core.network.http.decodeOrUnexpected
import com.stacknoise.haac.feature.schedules.domain.ScheduleDiff
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/**
 * Sync of the schedules of the active instance (concept 19.7, analogous to the entity sync of 9.1): on every
 * established connection and on every `schedules_changed` event, if the bridge reports the feature `schedules`.
 * It checks the revision, reads the list when the revision changed, diffs by id and tells the notification list
 * about removed and newly paused schedules of the user. It acts only on a successful answer; on errors, without
 * a connection or with a bridge without the feature the cache stays as it is.
 */
@Singleton
class ScheduleSync internal constructor(
    private val schedules: ScheduleDao,
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
        json: Json,
        errors: ErrorFactory,
        reporter: ErrorReporter,
        changes: ScheduleChangeReporter,
    ) : this(schedules, json, errors, reporter, changes, System::currentTimeMillis)

    /** The revision of the last written list per instance; it is not kept across app starts, the first sync lists. */
    private val revisions = ConcurrentHashMap<String, String>()

    /**
     * Syncs instance [serverId] over [channel] and follows `schedules_changed` until the connection ends or the
     * caller is cancelled. Returns at once if the bridge has no schedules. An ended connection is not an error
     * here: the supervisor reconnects and the next connection syncs again.
     */
    suspend fun follow(serverId: String, channel: BridgeChannel) {
        if (FEATURE !in channel.features) return
        try {
            sync(serverId, channel)
            channel.subscribe(SUBSCRIBE).collect { event ->
                if (CHANGED in event) sync(serverId, channel)
            }
        } catch (e: HaacException) {
            if (channel.isOpen) reporter.report(e, serverId)
        }
    }

    /** Steps 1 to 5 of concept 19.7. */
    private suspend fun sync(serverId: String, channel: BridgeChannel) {
        val revision = json.decodeOrUnexpected(ScheduleRevision.serializer(), channel.request(REVISION)).revision
        if (revisions[serverId] == revision) return
        val list = json.decodeOrUnexpected(ScheduleList.serializer(), channel.request(LIST))
        val now = clock()
        val listed = list.schedules.map { it.toRow(serverId, list.scope, now) }
        val cached = errors.database { schedules.all(serverId) }
        val change = ScheduleDiff.compute(cached, listed)
        errors.database { schedules.applySync(serverId, change.rows, change.removedIds) }
        revisions[serverId] = list.revision
        if (change.removedNames.isNotEmpty()) changes.removed(serverId, change.removedNames)
        change.paused.forEach { changes.paused(serverId, it.name, it.reason) }
    }

    /** Bridge commands, the feature flag and the event key (concept 11.2, 19.4). */
    private companion object {
        const val FEATURE = "schedules"
        const val REVISION = "haac_bridge/schedules/revision"
        const val LIST = "haac_bridge/schedules/list"
        const val SUBSCRIBE = "haac_bridge/subscribe_schedules"
        const val CHANGED = "schedules_changed"
    }
}
