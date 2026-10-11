package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.core.common.dispatch.DefaultDispatcher
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
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Sync of the active instance on every new connection (concept 9.1, 9.2, 11.4): revision check, entity list
 * and diff when the revision changed, then the live states of `haac_bridge/subscribe_entities` into [LiveStates].
 * The states reach the cache (`last_state`, for the offline display) in batches every few seconds and when the
 * connection ends, not with every event. All of it runs on the default dispatcher.
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
    private val live: LiveStates,
    private val dispatcher: CoroutineDispatcher,
    private val flushMs: Long,
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
        live: LiveStates,
        @DefaultDispatcher dispatcher: CoroutineDispatcher,
    ) : this(entities, json, errors, reporter, changes, live, dispatcher, FLUSH_MS, System::currentTimeMillis)

    private val _status = MutableStateFlow(SyncStatus())
    private val rechecks = Channel<Unit>(Channel.CONFLATED)
    private val dirty = ConcurrentHashMap.newKeySet<String>()

    /** Result and time of the last sync, or the error that stopped it. */
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /**
     * Syncs instance [serverId] over [channel] and keeps its states current until the connection ends or the
     * caller is cancelled. An ended connection is not an error here: the supervisor reconnects and the next
     * connection syncs again.
     */
    suspend fun follow(serverId: String, channel: BridgeChannel) {
        try {
            withContext(dispatcher) { run(serverId, channel) }
        } catch (e: HaacException) {
            if (channel.isOpen) {
                _status.update { it.copy(error = e) }
                reporter.report(e, serverId)
            }
        }
    }

    /** The sync and the subscription of one connection, with the batched write of the states beside it. */
    private suspend fun run(serverId: String, channel: BridgeChannel) = coroutineScope {
        seed(serverId)
        val writer = launch {
            while (true) {
                delay(flushMs)
                flush(serverId)
            }
        }
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
        } finally {
            writer.cancel()
            withContext(NonCancellable) { flush(serverId) }
        }
    }

    /** Loads the cached states once per connection, so a change event finds the state it applies to. */
    private suspend fun seed(serverId: String) {
        val cached = errors.database { entities.all(serverId) }
        val known = cached.mapNotNull { row -> json.decodeState(row.lastState)?.let { row.entityId to it } }
        live.seed(serverId, known.toMap())
    }

    /**
     * Writes the states that changed since the last write to the cache in one transaction (concept 12); the
     * offline display (14.1) shows them after a restart.
     */
    private suspend fun flush(serverId: String) {
        val ids = dirty.toList().also { dirty.removeAll(it.toSet()) }
        val states = ids.mapNotNull { id -> live.current(serverId, id)?.let { id to encode(it) } }.toMap()
        if (states.isNotEmpty()) errors.database { entities.setStates(serverId, states) }
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
        live.putAll(serverId, list.entities.associate { it.entityId to it.entityState() })
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

    /** Applies the states of an `a` (full) or `c` (changed) event; `r` is followed by `exposure_changed`. */
    private fun applyStates(serverId: String, event: JsonObject) {
        val states = buildMap {
            (event[ADDED] as? JsonObject)?.forEach { (entityId, compressed) ->
                (compressed as? JsonObject)?.let(CompressedState::full)?.let { put(entityId, it) }
            }
            (event[CHANGED] as? JsonObject)?.forEach { (entityId, diff) ->
                val current = live.current(serverId, entityId)
                if (current != null && diff is JsonObject) put(entityId, CompressedState.apply(current, diff))
            }
        }
        live.putAll(serverId, states)
        dirty += states.keys
    }

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

        /** How often changed states are written to the cache. */
        const val FLUSH_MS = 5_000L

        /** Marks a [recheck] among the subscription events. */
        val Recheck = JsonObject(emptyMap())
    }
}
