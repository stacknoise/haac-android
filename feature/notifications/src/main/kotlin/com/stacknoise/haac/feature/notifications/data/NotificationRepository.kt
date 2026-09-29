package com.stacknoise.haac.feature.notifications.data

import com.stacknoise.haac.core.database.assignment.RoomAssignmentDao
import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.database.entity.displayName
import com.stacknoise.haac.core.database.notification.NotificationDao
import com.stacknoise.haac.core.database.notification.NotificationEntity
import com.stacknoise.haac.core.database.notification.NotificationType
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.feature.notifications.domain.EntityLabel
import com.stacknoise.haac.feature.notifications.domain.NotificationItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * The notification list (concept 9.1, 17.4): entity changes of a sync, errors grouped by code within 10
 * minutes, entries purged after 30 days. Writes are serialised, so a repeated error never creates two entries.
 */
@Singleton
class NotificationRepository internal constructor(
    private val notifications: NotificationDao,
    private val entities: ExposedEntityDao,
    private val assignments: RoomAssignmentDao,
    private val errors: ErrorFactory,
    private val clock: () -> Long,
) {
    /** Uses the wall clock. */
    @Inject
    constructor(
        notifications: NotificationDao,
        entities: ExposedEntityDao,
        assignments: RoomAssignmentDao,
        errors: ErrorFactory,
    ) : this(notifications, entities, assignments, errors, System::currentTimeMillis)

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

    /** Entries of instance [serverId] and global ones, newest first, with the names of their entities. */
    fun items(serverId: String?): Flow<List<NotificationItem>> = notifications.observe(serverId).map { rows ->
        val names = serverId?.let { id ->
            errors.database { entities.all(id) }.associate { it.entityId to it.displayName(null) }
        }.orEmpty()
        rows.map { it.toItem(names) }
    }

    /** Number of unread entries of [serverId] and global ones; drives the unread dot of the bell. */
    fun unreadCount(serverId: String?): Flow<Int> = notifications.unreadCount(serverId)

    /** Marks entry [id] read. */
    suspend fun markRead(id: Long) = errors.database { notifications.markRead(id, clock()) }

    /** *Mark all read*. */
    suspend fun markAllRead(serverId: String?) = errors.database { notifications.markAllRead(serverId, clock()) }

    /** *Dismiss* or *Keep*: the entry stays without actions. */
    suspend fun resolve(id: Long) = errors.database { notifications.resolve(id, clock()) }

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
    private fun NotificationEntity.toItem(names: Map<String, String>) = NotificationItem(
        id = id,
        type = type,
        createdAt = createdAt,
        unread = readAt == null,
        resolved = resolvedAt != null,
        count = count,
        entities = decode(entityIds).map { EntityLabel(it, names[it] ?: it) },
        error = errorCode?.let { ErrorCode.of(it) ?: ErrorCode.APP_UNEXPECTED },
        bridgeCode = bridgeCode,
        serverId = serverId,
    )

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
