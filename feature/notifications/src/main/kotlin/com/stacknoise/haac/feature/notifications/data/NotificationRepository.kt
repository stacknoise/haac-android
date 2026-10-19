package com.stacknoise.haac.feature.notifications.data

import com.stacknoise.haac.core.database.assignment.EntityAlias
import com.stacknoise.haac.core.database.assignment.EntityAliasDao
import com.stacknoise.haac.core.database.assignment.RoomAssignmentDao
import com.stacknoise.haac.core.common.sync.RunResult
import com.stacknoise.haac.core.common.sync.ScheduleRun
import com.stacknoise.haac.core.database.DatabaseTransactions
import com.stacknoise.haac.core.database.entity.EntityStatus
import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.database.entity.byEntityId
import com.stacknoise.haac.core.database.entity.displayName
import com.stacknoise.haac.core.database.notification.NotificationDao
import com.stacknoise.haac.core.database.notification.NotificationEntity
import com.stacknoise.haac.core.database.notification.NotificationType
import com.stacknoise.haac.core.database.schedule.ScheduleDao
import com.stacknoise.haac.core.database.server.RunCursorDao
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.feature.notifications.domain.EntityLabel
import com.stacknoise.haac.feature.notifications.domain.NotificationItem
import com.stacknoise.haac.feature.notifications.domain.RunInfo
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Entries a deletion removed, kept so that *Undo* can put them back; opaque outside the data layer.
 */
class DeletedNotifications internal constructor(internal val rows: List<NotificationEntity>) {
    /** How many entries were deleted. */
    val count: Int get() = rows.size
}

/**
 * The notification list (concept 9.1, 17.4): entity changes of a sync, errors grouped by code within 10
 * minutes, entries purged after 30 days. Writes are serialised, so a repeated error never creates two entries.
 */
@Suppress("TooManyFunctions", "LongParameterList") // one function per list operation; DAOs are injected
@Singleton
class NotificationRepository internal constructor(
    private val notifications: NotificationDao,
    private val entities: ExposedEntityDao,
    private val aliases: EntityAliasDao,
    private val assignments: RoomAssignmentDao,
    private val scheduleRows: ScheduleDao,
    private val cursors: RunCursorDao,
    private val transactions: DatabaseTransactions,
    private val errors: ErrorFactory,
    private val clock: () -> Long,
) {
    /** Uses the wall clock. */
    @Inject
    constructor(
        notifications: NotificationDao,
        entities: ExposedEntityDao,
        aliases: EntityAliasDao,
        assignments: RoomAssignmentDao,
        scheduleRows: ScheduleDao,
        cursors: RunCursorDao,
        transactions: DatabaseTransactions,
        errors: ErrorFactory,
    ) : this(
        notifications, entities, aliases, assignments, scheduleRows, cursors, transactions, errors,
        System::currentTimeMillis,
    )

    private val writes = Mutex()

    /** One entry for the [added] and one for the [removed] entities of a sync; an empty list adds none. */
    suspend fun addEntityChanges(serverId: String, added: List<String>, removed: List<String>) = write { now ->
        mapOf(NotificationType.ADDED to added, NotificationType.REMOVED to removed)
            .filterValues { it.isNotEmpty() }
            .forEach { (type, ids) ->
                val entry = NotificationEntity(
                    serverId = serverId,
                    type = type,
                    count = ids.size,
                    entityIds = encode(ids),
                    createdAt = now,
                )
                notifications.insert(entry)
            }
    }

    /** One entry for the schedules [names] that no longer exist on the server (HAAC-SCH-001, concept 19.7). */
    suspend fun addScheduleRemoved(serverId: String, names: List<String>) = write { now ->
        if (names.isEmpty()) return@write
        val entry = NotificationEntity(
            serverId = serverId,
            type = NotificationType.SCHEDULE_REMOVED,
            errorCode = ErrorCode.SCH_REMOVED.code,
            count = names.size,
            entityIds = encode(names),
            createdAt = now,
        )
        notifications.insert(entry)
    }

    /** An entry for schedule [name], which the server paused for [reason] (HAAC-SCH-002, concept 19.7). */
    suspend fun addSchedulePaused(serverId: String, name: String, reason: String) = write { now ->
        val entry = NotificationEntity(
            serverId = serverId,
            type = NotificationType.SCHEDULE_PAUSED,
            errorCode = ErrorCode.SCH_PAUSED.code,
            entityIds = encode(listOf(name)),
            detail = reason,
            createdAt = now,
        )
        notifications.insert(entry)
    }

    /**
     * One entry per schedule and result for the [runs] of instance [serverId], and the cursor [logId] and
     * [lastSeq], written in one transaction (concept 19.11). An empty list only moves the cursor.
     */
    suspend fun addScheduleRuns(serverId: String, logId: String, lastSeq: Long, runs: List<ScheduleRun>) = write {
        transactions.run {
            val groups = runs.groupBy { it.scheduleId to it.result }.values
            groups.forEach { notifications.insert(runEntry(serverId, it)) }
            cursors.setRunCursor(serverId, logId, lastSeq)
        }
    }

    /** The entry for [group], the runs of one schedule with one result: the newest run gives name and time. */
    private fun runEntry(serverId: String, group: List<ScheduleRun>): NotificationEntity {
        val last = group.maxBy { it.at }
        val detail = RunDetail(last.scheduleId, last.result.name, group.any { it.removed })
        return NotificationEntity(
            serverId = serverId,
            type = NotificationType.SCHEDULE_RUN,
            errorCode = ErrorCode.SCH_RUN_INCOMPLETE.code.takeIf { last.result.incomplete() },
            count = group.size,
            entityIds = encode(listOf(last.name)),
            createdAt = last.at,
            detail = Json.encodeToString(RunDetail.serializer(), detail),
        )
    }

    /** An error entry, or one more occurrence of the same code of [serverId] within 10 minutes (concept 17.4). */
    suspend fun addError(error: HaacException, serverId: String?) = write { now ->
        val recent = notifications.recentError(serverId, error.code.code, now - GROUP_MS)
        if (recent != null) {
            notifications.countAgain(recent.id)
        } else {
            val entry = NotificationEntity(
                serverId = serverId,
                type = NotificationType.ERROR,
                errorCode = error.code.code,
                bridgeCode = error.bridgeCode,
                createdAt = now,
            )
            notifications.insert(entry)
        }
    }

    /**
     * Entries of instance [serverId] and global ones, newest first, with the names of their entities: the local
     * name if the user gave one, else the name from the bridge or HA (concept 7.3).
     */
    fun items(serverId: String?): Flow<List<NotificationItem>> =
        combine(
            notifications.observe(serverId),
            localNames(serverId),
            placedIds(serverId),
            scheduleIds(serverId),
        ) { rows, local, placed, existing ->
            val names = entityNames(serverId, local)
            val shared = sharedIds(serverId)
            rows.map { it.toItem(names, shared - placed, existing) }
        }

    /** The ids of the cached schedules of [serverId], which a run entry can open; none for the global entries. */
    private fun scheduleIds(serverId: String?): Flow<Set<String>> =
        serverId?.let { id -> scheduleRows.observe(id).map { rows -> rows.map { it.scheduleId }.toSet() } }
            ?: flowOf(emptySet())

    /** The local names of instance [serverId]; none for the global entries. */
    private fun localNames(serverId: String?): Flow<List<EntityAlias>> =
        serverId?.let(aliases::observe) ?: flowOf(emptyList())

    /** The ids of the entities of [serverId] that are in at least one room; none for the global entries. */
    private fun placedIds(serverId: String?): Flow<Set<String>> =
        serverId?.let { id -> assignments.observeAll(id).map { rows -> rows.map { it.entityId }.toSet() } }
            ?: flowOf(emptySet())

    /** The ids of the entities of [serverId] that are still shared, so they can be assigned (concept 7.4). */
    private suspend fun sharedIds(serverId: String?): Set<String> {
        val id = serverId ?: return emptySet()
        return errors.database { entities.all(id) }.filter { it.status == EntityStatus.ACTIVE }
            .map { it.entityId }.toSet()
    }

    /** The display names of the entities of [serverId] by `entity_id`, with the [local] names (concept 7.3). */
    private suspend fun entityNames(serverId: String?, local: List<EntityAlias>): Map<String, String> {
        val id = serverId ?: return emptyMap()
        val byId = local.byEntityId()
        return errors.database { entities.all(id) }.associate { it.entityId to it.displayName(byId[it.entityId]) }
    }

    /** Number of unread entries of [serverId] and global ones; drives the unread dot of the bell. */
    fun unreadCount(serverId: String?): Flow<Int> = notifications.unreadCount(serverId)

    /** Marks entry [id] read. */
    suspend fun markRead(id: Long) = errors.database { notifications.markRead(id, clock()) }

    /** *Mark all read*. */
    suspend fun markAllRead(serverId: String?) = errors.database { notifications.markAllRead(serverId, clock()) }

    /** *Dismiss* or *Keep*: the entry stays without actions. */
    suspend fun resolve(id: Long) = errors.database { notifications.resolve(id, clock()) }

    /** Deletes entry [id] from the list; the result puts it back with [restore]. */
    suspend fun delete(id: Long): DeletedNotifications = errors.database {
        val rows = listOfNotNull(notifications.get(id))
        notifications.delete(id)
        DeletedNotifications(rows)
    }

    /** *Delete all*: deletes the entries of [serverId] and the global ones; the result puts them back. */
    suspend fun deleteAll(serverId: String?): DeletedNotifications = errors.database {
        val rows = notifications.shown(serverId)
        notifications.deleteAll(serverId)
        DeletedNotifications(rows)
    }

    /** Undo of a deletion: the entries return with their ids, read state and time. */
    suspend fun restore(deleted: DeletedNotifications) = errors.database { notifications.restore(deleted.rows) }

    /** *Remove tile*: removes the entities of [item] from every room of its instance, then resolves it (7.4). */
    suspend fun removeTiles(item: NotificationItem) = errors.database {
        val serverId = item.serverId ?: return@database
        assignments.removeEverywhere(serverId, item.entities.map { it.entityId })
        notifications.resolve(item.id, clock())
    }

    /** Purges old entries and runs [block] with the current time, one write at a time. */
    private suspend fun write(block: suspend (Long) -> Unit) = writes.withLock {
        val now = clock()
        errors.database {
            notifications.purge(now - RETENTION_MS)
            block(now)
        }
    }

    /** The screen's view of a row; unknown error codes (newer app data) show as HAAC-APP-000. */
    private fun NotificationEntity.toItem(names: Map<String, String>, free: Set<String>, schedules: Set<String>) =
        NotificationItem(
        id = id,
        type = type,
        createdAt = createdAt,
        unread = readAt == null,
        resolved = resolvedAt != null,
        count = count,
        entities = decode(entityIds).map { EntityLabel(it, names[it] ?: it) },
        error = errorCode?.let { ErrorCode.of(it) ?: ErrorCode.APP_UNEXPECTED },
        detail = detail,
        bridgeCode = bridgeCode,
        serverId = serverId,
        unassigned = if (type == NotificationType.ADDED) decode(entityIds).filter { it in free } else emptyList(),
        run = if (type == NotificationType.SCHEDULE_RUN) runInfo(detail, schedules) else null,
    )

    /** The run behind a *schedule run* entry; a damaged or unknown [detail] reads as a failed run of no schedule. */
    private fun runInfo(detail: String?, schedules: Set<String>): RunInfo {
        val parsed = detail?.let {
            try {
                Json.decodeFromString(RunDetail.serializer(), it)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
        val result = RunResult.entries.firstOrNull { it.name == parsed?.result } ?: RunResult.FAILED
        val id = parsed?.scheduleId
        return RunInfo(id, result, parsed?.removed ?: false, id != null && id in schedules)
    }

    /** True for the results that show HAAC-SCH-007 (concept 19.11). */
    private fun RunResult.incomplete() = this == RunResult.PARTIAL || this == RunResult.FAILED

    /** The JSON in column `detail` of a *schedule run* entry (concept 19.11). */
    @Serializable
    private class RunDetail(val scheduleId: String, val result: String, val removed: Boolean)

    /** Time limits (concept 9.1, 17.4) and the JSON of column `entity_ids`. */
    private companion object {
        const val GROUP_MS = 10 * 60_000L
        const val RETENTION_MS = 30 * 24 * 60 * 60_000L
        val IDS = ListSerializer(String.serializer())

        /** Entity ids as the JSON of column `entity_ids`. */
        fun encode(ids: List<String>): String = Json.encodeToString(IDS, ids)

        /** The ids of column `entity_ids`; a damaged value shows no entities. */
        fun decode(json: String): List<String> = try {
            Json.decodeFromString(IDS, json)
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }
}
