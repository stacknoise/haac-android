package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.core.database.DatabaseTransactions
import com.stacknoise.haac.core.database.assignment.EntityAlias
import com.stacknoise.haac.core.database.assignment.EntityAliasDao
import com.stacknoise.haac.core.database.assignment.RoomAssignment
import com.stacknoise.haac.core.database.assignment.RoomAssignmentDao
import com.stacknoise.haac.core.database.assignment.TileSize
import com.stacknoise.haac.core.database.entity.EntityStatus
import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ValidationException
import com.stacknoise.haac.feature.entities.domain.DefaultEntityControlFactory
import com.stacknoise.haac.feature.entities.domain.DefaultTileFactory
import com.stacknoise.haac.feature.entities.domain.TileContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** [AssignmentWriter] and [EntityCatalog] against in-memory tables (concept 7.2 – 7.4). */
class AssignmentTest {
    private val cache = MutableStateFlow(
        listOf(
            ExposedEntity("s1", "switch.lamp", "switch", "Floor lamp plug", lastState = """{"state":"on"}"""),
            ExposedEntity("s1", "climate.radiator", "climate", "Radiator"),
            ExposedEntity("s1", "sensor.gone", "sensor", "Humidity", status = EntityStatus.WITHDRAWN),
        ),
    )
    private val rows = MutableStateFlow<List<RoomAssignment>>(emptyList())
    private val aliases = MutableStateFlow<List<EntityAlias>>(emptyList())
    private val activeRooms = mutableSetOf("living", "kitchen")

    private val entityDao = object : ExposedEntityDao {
        override suspend fun all(serverId: String) = cache.value

        override fun observe(serverId: String): Flow<List<ExposedEntity>> = cache

        override suspend fun upsert(rows: List<ExposedEntity>) = unused()

        override suspend fun revision(serverId: String) = unused()

        override suspend fun markSynced(serverId: String, revision: String, at: Long) = unused()

        override suspend fun state(serverId: String, entityId: String) = unused()

        override suspend fun setState(serverId: String, entityId: String, state: String) = unused()
    }

    private val assignmentDao = object : RoomAssignmentDao {
        override fun observe(roomId: String) =
            rows.map { list -> list.filter { it.roomId == roomId }.sortedBy { it.sortOrder } }

        override fun observeAll(serverId: String) = rows.map { list -> list.filter { it.roomId in activeRooms } }

        override suspend fun insert(rows: List<RoomAssignment>) {
            val taken = this@AssignmentTest.rows.value.map { it.roomId to it.entityId }.toSet()
            this@AssignmentTest.rows.value += rows.filter { (it.roomId to it.entityId) !in taken }
        }

        override suspend fun maxSortOrder(roomId: String) =
            rows.value.filter { it.roomId == roomId }.maxOfOrNull { it.sortOrder }

        override suspend fun roomIsActive(roomId: String) = if (roomId in activeRooms) 1 else 0

        override suspend fun remove(roomId: String, entityId: String) {
            rows.value = rows.value.filterNot { it.roomId == roomId && it.entityId == entityId }
        }

        override suspend fun removeEverywhere(serverId: String, entityIds: List<String>) {
            rows.value = rows.value.filterNot { it.entityId in entityIds }
        }
    }

    private val aliasDao = object : EntityAliasDao {
        override fun observe(serverId: String): Flow<List<EntityAlias>> = aliases

        override suspend fun set(alias: EntityAlias) {
            aliases.value = aliases.value.filterNot { it.entityId == alias.entityId } + alias
        }

        override suspend fun clear(serverId: String, entityId: String) {
            aliases.value = aliases.value.filterNot { it.entityId == entityId }
        }
    }

    private val passThrough = object : DatabaseTransactions {
        override suspend fun <T> run(block: suspend () -> T): T = block()
    }

    /** Fails a call the tested code must not make. */
    private fun unused(): Nothing = throw AssertionError("not expected in these tests")

    private val errors = DefaultErrorFactory()
    private val builder =
        TileBuilder(Json { ignoreUnknownKeys = true }, DefaultTileFactory(), DefaultEntityControlFactory())
    private val catalog = EntityCatalog(entityDao, aliasDao, assignmentDao, builder, PendingStates(), errors)
    private val writer = AssignmentWriter(assignmentDao, aliasDao, passThrough, errors) { 42 }

    @Test
    fun `withdrawn entities cannot be picked, new ones get their default size at the end`() = runTest {
        val assignable = catalog.assignable("s1").first()
        assertEquals(listOf("Floor lamp plug", "Radiator"), assignable.map { it.tile.name })

        writer.add("living", assignable.take(1))
        writer.add("living", catalog.assignable("s1").first())
        val placed = rows.value.filter { it.roomId == "living" }
        val expected = listOf("switch.lamp" to 1024, "climate.radiator" to 2048)
        assertEquals(expected, placed.map { it.entityId to it.sortOrder })
        assertEquals(listOf(TileSize.SMALL, TileSize.LARGE), placed.map { it.tileSize })
        assertEquals(setOf("living"), catalog.assignable("s1").first().first().roomIds)
    }

    @Test
    fun `a deleted room is rejected`() = runTest {
        val error = assertThrows<ValidationException> { writer.add("attic", catalog.assignable("s1").first()) }
        assertEquals(ErrorCode.LAY_PLACE_MISSING, error.code)
        assertTrue(rows.value.isEmpty())
    }

    @Test
    fun `room tiles show aliases, live states and withdrawn entities`() = runTest {
        rows.value = listOf(
            RoomAssignment("living", "sensor.gone", 2, TileSize.SMALL, 1),
            RoomAssignment("living", "switch.lamp", 1, TileSize.SMALL, 1),
            RoomAssignment("living", "switch.unknown", 3, TileSize.SMALL, 1),
        )
        writer.rename("s1", "switch.lamp", " Reading lamp ")

        val tiles = catalog.roomTiles("s1", "living").first()
        assertEquals(listOf("Reading lamp", "Humidity", "switch.unknown"), tiles.map { it.name })
        assertEquals("Floor lamp plug", tiles[0].defaultName)
        assertEquals(TileContent.Switch(true), tiles[0].content)
        assertEquals(listOf(false, true, true), tiles.map { it.withdrawn })

        writer.rename("s1", "switch.lamp", " ")
        assertEquals("Floor lamp plug", catalog.roomTiles("s1", "living").first()[0].name)
    }

    @Test
    fun `remove takes one room or every room`() = runTest {
        writer.add("living", catalog.assignable("s1").first())
        writer.add("kitchen", catalog.assignable("s1").first())
        writer.remove("living", "switch.lamp")
        assertEquals(3, rows.value.size)
        writer.removeEverywhere("s1", listOf("climate.radiator"))
        assertEquals(listOf("kitchen" to "switch.lamp"), rows.value.map { it.roomId to it.entityId })
    }
}
