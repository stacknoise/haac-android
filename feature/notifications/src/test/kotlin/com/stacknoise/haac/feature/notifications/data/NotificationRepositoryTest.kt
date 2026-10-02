package com.stacknoise.haac.feature.notifications.data

import com.stacknoise.haac.core.database.assignment.EntityAlias
import com.stacknoise.haac.core.database.assignment.EntityAliasDao
import com.stacknoise.haac.core.database.assignment.RoomAssignment
import com.stacknoise.haac.core.database.assignment.RoomAssignmentDao
import com.stacknoise.haac.core.database.assignment.TileSize
import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.database.notification.NotificationDao
import com.stacknoise.haac.core.database.notification.NotificationEntity
import com.stacknoise.haac.core.database.notification.NotificationType
import com.stacknoise.haac.core.error.BridgeException
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.feature.notifications.domain.EntityLabel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NotificationRepositoryTest {
    /** The `notification` table in memory with the same filters as the DAO queries. */
    private val table = MutableStateFlow<List<NotificationEntity>>(emptyList())
    private var nextId = 1L

    private fun visible(serverId: String?) =
        table.value.filter { it.serverId == null || it.serverId == serverId }.sortedByDescending { it.createdAt }

    private fun change(id: Long, update: (NotificationEntity) -> NotificationEntity) {
        table.value = table.value.map { if (it.id == id) update(it) else it }
    }

    private val dao = object : NotificationDao {
        override suspend fun insert(entry: NotificationEntity): Long {
            val id = nextId++
            table.value += entry.copy(id = id)
            return id
        }

        override fun observe(serverId: String?): Flow<List<NotificationEntity>> = table.map { visible(serverId) }

        override fun unreadCount(serverId: String?): Flow<Int> =
            table.map { visible(serverId).count { it.readAt == null } }

        override suspend fun recentError(serverId: String?, code: String, since: Long) = table.value
            .filter { it.type == NotificationType.ERROR && it.errorCode == code && it.serverId == serverId }
            .filter { it.createdAt >= since }
            .maxByOrNull { it.createdAt }

        override suspend fun countAgain(id: Long) = change(id) { it.copy(count = it.count + 1, readAt = null) }

        override suspend fun markRead(id: Long, at: Long) = change(id) { it.copy(readAt = it.readAt ?: at) }

        override suspend fun markAllRead(serverId: String?, at: Long) {
            visible(serverId).forEach { entry -> change(entry.id) { it.copy(readAt = it.readAt ?: at) } }
        }

        override suspend fun resolve(id: Long, at: Long) =
            change(id) { it.copy(resolvedAt = at, readAt = it.readAt ?: at) }

        override suspend fun get(id: Long): NotificationEntity? = table.value.firstOrNull { it.id == id }

        override suspend fun shown(serverId: String?): List<NotificationEntity> = visible(serverId)

        override suspend fun restore(entries: List<NotificationEntity>) {
            table.value = (table.value.filter { old -> entries.none { it.id == old.id } } + entries).sortedBy { it.id }
        }

        override suspend fun delete(id: Long) {
            table.value = table.value.filter { it.id != id }
        }

        override suspend fun deleteAll(serverId: String?) {
            val shown = visible(serverId).map { it.id }.toSet()
            table.value = table.value.filter { it.id !in shown }
        }

        override suspend fun purge(before: Long) {
            table.value = table.value.filter { it.createdAt >= before }
        }
    }

    private val entities = object : ExposedEntityDao {
        override suspend fun all(serverId: String) = listOf(
            ExposedEntity(serverId, "switch.hall", "switch", "Hall switch"),
            ExposedEntity(serverId, "sensor.h", "sensor", "Humidity sensor", configuredName = "Humidity"),
        )

        override fun observe(serverId: String): Flow<List<ExposedEntity>> = error("not used")

        override suspend fun upsert(rows: List<ExposedEntity>) = error("not used")

        override suspend fun revision(serverId: String) = error("not used")

        override suspend fun markSynced(serverId: String, revision: String, at: Long) = error("not used")

        override suspend fun state(serverId: String, entityId: String) = error("not used")

        override suspend fun setState(serverId: String, entityId: String, state: String) = error("not used")
    }

    private val removed = mutableListOf<Pair<String, List<String>>>()
    private val assignments = object : RoomAssignmentDao {
        override fun observe(roomId: String): Flow<List<RoomAssignment>> = error("not used")

        override fun observeAll(serverId: String): Flow<List<RoomAssignment>> = error("not used")

        override suspend fun insert(rows: List<RoomAssignment>) = error("not used")

        override suspend fun maxSortOrder(roomId: String) = error("not used")

        override suspend fun roomIsActive(roomId: String) = error("not used")

        override suspend fun remove(roomId: String, entityId: String) = error("not used")

        override suspend fun arrange(roomId: String, entityId: String, sortOrder: Int, tileSize: TileSize) =
            error("no layout here")

        override suspend fun removeEverywhere(serverId: String, entityIds: List<String>) {
            removed += serverId to entityIds
        }
    }

    private val localNames = MutableStateFlow<List<EntityAlias>>(emptyList())
    private val aliases = object : EntityAliasDao {
        override fun observe(serverId: String): Flow<List<EntityAlias>> = localNames

        override suspend fun set(alias: EntityAlias) = error("not used")

        override suspend fun clear(serverId: String, entityId: String) = error("not used")
    }

    private var now = 1_000_000L
    private val repository = NotificationRepository(dao, entities, aliases, assignments, DefaultErrorFactory()) { now }

    @Test
    fun `a local entity name replaces the HA name in the entries`() = runTest {
        localNames.value = listOf(EntityAlias("s1", "switch.hall", "Hall lamp"))
        repository.addEntityChanges("s1", added = listOf("switch.hall"), removed = listOf("sensor.h"))

        val items = repository.items("s1").first()
        assertEquals(
            listOf(EntityLabel("switch.hall", "Hall lamp")),
            items.single { it.type == NotificationType.ADDED }.entities,
        )
        assertEquals(
            listOf(EntityLabel("sensor.h", "Humidity")),
            items.single { it.type == NotificationType.REMOVED }.entities,
        )
    }

    @Test
    fun `a sync adds one entry per kind with the entity names`() = runTest {
        repository.addEntityChanges("s1", added = listOf("switch.hall", "switch.unknown"), removed = listOf("sensor.h"))
        repository.addEntityChanges("s1", added = emptyList(), removed = emptyList())

        val items = repository.items("s1").first()
        assertEquals(2, items.size)
        val added = items.single { it.type == NotificationType.ADDED }
        assertEquals(2, added.count)
        val expected = listOf(
            EntityLabel("switch.hall", "Hall switch"),
            EntityLabel("switch.unknown", "switch.unknown"),
        )
        assertEquals(expected, added.entities)
        val removed = items.single { it.type == NotificationType.REMOVED }
        assertEquals(listOf(EntityLabel("sensor.h", "Humidity")), removed.entities)
    }

    @Test
    fun `removed schedules make one grouped entry with their names, paused ones one each with the reason`() = runTest {
        repository.addScheduleRemoved("s1", listOf("Morning light", "Evening"))
        repository.addScheduleRemoved("s1", emptyList())
        repository.addSchedulePaused("s1", "Garage", "no_entities")

        val items = repository.items("s1").first()
        assertEquals(2, items.size)
        val gone = items.single { it.type == NotificationType.SCHEDULE_REMOVED }
        assertEquals(2, gone.count)
        assertEquals(listOf("Morning light", "Evening"), gone.entities.map { it.name })
        assertEquals(ErrorCode.SCH_REMOVED, gone.error)
        val paused = items.single { it.type == NotificationType.SCHEDULE_PAUSED }
        assertEquals(listOf("Garage"), paused.entities.map { it.name })
        assertEquals("no_entities", paused.detail)
        assertEquals(ErrorCode.SCH_PAUSED, paused.error)
    }

    @Test
    fun `the same error within 10 minutes is counted, later or elsewhere it is new`() = runTest {
        val lost = NetworkException(ErrorCode.NET_UNREACHABLE)
        repository.addError(lost, "s1")
        repository.markAllRead("s1")
        now += 9 * 60_000L
        repository.addError(lost, "s1")
        repository.addError(lost, null)
        now += 2 * 60_000L
        repository.addError(lost, "s1")

        val errors = table.value.sortedBy { it.createdAt }
        assertEquals(listOf(2, 1, 1), errors.map { it.count })
        assertEquals(null, errors.first().readAt)
        assertEquals(3, repository.unreadCount("s1").first())
    }

    @Test
    fun `bridge codes are kept and unknown codes show as unexpected`() = runTest {
        repository.addError(BridgeException(ErrorCode.BRG_ACTION_FAILED, bridgeCode = "HAB-X-9"), "s1")
        val future = NotificationEntity(
            id = 99,
            serverId = null,
            type = NotificationType.ERROR,
            errorCode = "HAAC-NEW-001",
            createdAt = now,
        )
        table.value += future

        val items = repository.items("s1").first()
        assertEquals("HAB-X-9", items.single { it.id == 1L }.bridgeCode)
        assertEquals(ErrorCode.APP_UNEXPECTED, items.single { it.id == 99L }.error)
    }

    @Test
    fun `entries older than 30 days are purged on the next write`() = runTest {
        repository.addError(NetworkException(ErrorCode.NET_UNREACHABLE), "s1")
        now += 31L * 24 * 60 * 60_000L
        repository.addEntityChanges("s1", listOf("switch.hall"), emptyList())
        assertEquals(listOf(NotificationType.ADDED), table.value.map { it.type })
    }

    @Test
    fun `resolved entries stay, read and without actions`() = runTest {
        repository.addEntityChanges("s1", listOf("switch.hall"), emptyList())
        repository.resolve(1)
        val item = repository.items("s1").first().single()
        assertTrue(item.resolved)
        assertFalse(item.unread)
    }

    @Test
    fun `remove tile takes the entities out of every room and resolves the entry`() = runTest {
        repository.addEntityChanges("s1", added = emptyList(), removed = listOf("sensor.h"))
        repository.removeTiles(repository.items("s1").first().single())

        assertEquals(listOf("s1" to listOf("sensor.h")), removed)
        assertTrue(repository.items("s1").first().single().resolved)
    }

    @Test
    fun `one entry can be deleted`() = runTest {
        repository.addEntityChanges("s1", added = listOf("switch.hall"), removed = listOf("sensor.h"))
        val first = repository.items("s1").first().first()

        repository.delete(first.id)

        assertEquals(1, repository.items("s1").first().size)
        assertTrue(repository.items("s1").first().none { it.id == first.id })
    }

    @Test
    fun `a deleted entry comes back with its state`() = runTest {
        repository.addEntityChanges("s1", added = listOf("switch.hall"), removed = emptyList())
        val entry = repository.items("s1").first().single()
        repository.markRead(entry.id)
        val before = table.value

        val deleted = repository.delete(entry.id)
        assertTrue(repository.items("s1").first().isEmpty())
        repository.restore(deleted)

        assertEquals(before, table.value)
    }

    @Test
    fun `delete all can be undone`() = runTest {
        repository.addEntityChanges("s1", listOf("switch.hall"), listOf("sensor.h"))
        repository.addError(NetworkException(ErrorCode.NET_UNREACHABLE), null)
        val before = table.value

        val deleted = repository.deleteAll("s1")
        assertEquals(3, deleted.count)
        repository.restore(deleted)

        assertEquals(before, table.value)
    }

    @Test
    fun `delete all removes the instance's and the global entries but not another instance's`() = runTest {
        repository.addEntityChanges("s1", listOf("switch.hall"), emptyList())
        repository.addError(NetworkException(ErrorCode.NET_UNREACHABLE), null)
        repository.addEntityChanges("s2", listOf("switch.hall"), emptyList())

        repository.deleteAll("s1")

        assertTrue(repository.items("s1").first().isEmpty())
        assertEquals(listOf("s2"), table.value.map { it.serverId })
    }
}
