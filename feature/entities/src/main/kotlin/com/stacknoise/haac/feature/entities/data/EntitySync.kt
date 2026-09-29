package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.core.common.sync.EntityChangeReporter
import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.ErrorReporter
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.core.network.connection.BridgeChannel
import com.stacknoise.haac.core.network.http.decodeOrUnexpected
import com.stacknoise.haac.core.network.http.text
import com.stacknoise.haac.feature.entities.domain.EntityState
import com.stacknoise.haac.feature.entities.domain.ExposureDiff
import com.stacknoise.haac.feature.entities.domain.SyncResult
import com.stacknoise.haac.feature.entities.domain.SyncStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Sync of the active instance on every new connection (concept 9.1, 9.2, 11.4): revision check, entity list
 * and diff when the revision changed, then the live states of `haac_bridge/subscribe_entities` into the cache.
 * `exposure_changed` runs the revision check again. The sync never assigns entities to rooms (9.3). Changes
 * and errors go to the notification list (M-09, 17.4).
 */
@Singleton
class EntitySync internal constructor(
    private val entities: ExposedEntityDao,
    private val json: Json,
    private val errors: ErrorFactory,
    private val reporter: ErrorReporter,
    private val changes: EntityChangeReporter,
    private val clock: () -> Long,
) {
    /** Uses the wall clock for sync and withdrawal times. */
    @Inject
    constructor(
        entities: ExposedEntityDao,
        json: Json,
        errors: ErrorFactory,
        reporter: ErrorReporter,
        changes: EntityChangeReporter,
    ) : this(entities, json, errors, reporter, changes, System::currentTimeMillis)

    private val _status = MutableStateFlow(SyncStatus())
    private val rechecks = Channel<Unit>(Channel.CONFLATED)

    /** Result and time of the last sync, or the error that stopped it. */
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /**
     * Syncs instance [serverId] over [channel] and keeps its states current until the connection ends or the
     * caller is cancelled. An ended connection is not an error here: the supervisor reconnects and the next
     * connection syncs again.
     */
    suspend fun follow(serverId: String, channel: BridgeChannel) {
        try {
            var revision = syncExposure(serverId, channel)
            rechecks.tryReceive() // A request from before this sync is answered by it.
            merge(channel.subscribe(SUBSCRIBE), rechecks.receiveAsFlow().map { Recheck }).collect { event ->
                val changed = (event[EXPOSURE_CHANGED] as? JsonObject)?.text("revision")
                if (event === Recheck || (changed != null && changed != revision)) {
                    revision = syncExposure(serverId, channel)
                } else {
                    applyStates(serverId, event)
                }
            }
        } catch (e: HaacException) {
            if (channel.isOpen) {
                _status.update { it.copy(error = e) }
                reporter.report(e, serverId)
            }
        }
    }

    /**
     * Runs the revision check again on the running connection, e.g. after the bridge rejected a service call
     * (concept 14.1); without a connection the next one checks anyway.
     */
    fun recheck() {
        rechecks.trySend(Unit)
    }

    /** Steps 2–4 and 6 of concept 9.1; returns the revision now in the cache. */
    private suspend fun syncExposure(serverId: String, channel: BridgeChannel): String {
        val revision = json.decodeOrUnexpected(ExposureRevision.serializer(), channel.request(REVISION)).revision
        val now = clock()
        if (revision == errors.database { entities.revision(serverId) }) {
            errors.database { entities.markSynced(serverId, revision, now) }
            _status.value = SyncStatus(SyncResult(), now)
            return revision
        }
        val list = json.decodeOrUnexpected(EntityList.serializer(), channel.request(LIST))
        val listed = list.entities.map { it.toRow(serverId, encode(it.entityState())) }
        val cached = errors.database { entities.all(serverId) }
        val change = ExposureDiff.compute(cached, listed, now)
        errors.database { entities.applySync(serverId, change.rows, list.revision, now) }
        _status.value = SyncStatus(change.result, now)
        // On the first sync every entity is new; the list would only repeat the picker.
        if (cached.isNotEmpty()) report(serverId, change.result)
        return list.revision
    }

    /** Adds the entries for [result] to the notification list, if anything was added or withdrawn (concept 9.1). */
    private suspend fun report(serverId: String, result: SyncResult) {
        if (result.added.isNotEmpty() || result.removed.isNotEmpty()) {
            changes.report(serverId, result.added, result.removed)
        }
    }

    /** Writes the states of an `a` (full) or `c` (changed) event; `r` is followed by `exposure_changed`. */
    private suspend fun applyStates(serverId: String, event: JsonObject) {
        val states = buildMap {
            (event[ADDED] as? JsonObject)?.forEach { (entityId, compressed) ->
                (compressed as? JsonObject)?.let(CompressedState::full)?.let { put(entityId, it) }
            }
            (event[CHANGED] as? JsonObject)?.forEach { (entityId, diff) ->
                val current = cachedState(serverId, entityId)
                if (current != null && diff is JsonObject) put(entityId, CompressedState.apply(current, diff))
            }
        }
        if (states.isEmpty()) return
        val encoded = states.mapValues { encode(it.value) }
        errors.database { entities.setStates(serverId, encoded) }
    }

    /** The cached state of [entityId], or null if the entity is not cached or has no state yet. */
    private suspend fun cachedState(serverId: String, entityId: String): EntityState? =
        errors.database { entities.state(serverId, entityId) }
            ?.let { json.decodeOrUnexpected(EntityState.serializer(), it) }

    /** [state] as the JSON of column `last_state`. */
    private fun encode(state: EntityState): String = json.encodeToString(EntityState.serializer(), state)

    /** Bridge commands and event keys (concept 11.2, 11.3). */
    private companion object {
        const val REVISION = "haac_bridge/exposure/revision"
        const val LIST = "haac_bridge/entities/list"
        const val SUBSCRIBE = "haac_bridge/subscribe_entities"
        const val ADDED = "a"
        const val CHANGED = "c"
        const val EXPOSURE_CHANGED = "exposure_changed"

        /** Marks a [recheck] among the subscription events. */
        val Recheck = JsonObject(emptyMap())
    }
}
