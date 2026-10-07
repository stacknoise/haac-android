package com.stacknoise.haac.feature.onboarding.data

import com.stacknoise.haac.core.database.DatabaseTransactions
import com.stacknoise.haac.core.database.assignment.RoomAssignment
import com.stacknoise.haac.core.database.assignment.RoomAssignmentDao
import com.stacknoise.haac.core.database.layout.FloorDao
import com.stacknoise.haac.core.database.layout.FloorEntity
import com.stacknoise.haac.core.database.layout.HomeDao
import com.stacknoise.haac.core.database.layout.HomeEntity
import com.stacknoise.haac.core.database.layout.RoomDao
import com.stacknoise.haac.core.database.layout.RoomEntity
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.network.demo.DemoWorldData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The demo layout that a new demo instance starts with (concept 20.4). */
class DemoLayoutSeederTest {
    private val homes = mutableListOf<HomeEntity>()
    private val floors = mutableListOf<FloorEntity>()
    private val rooms = mutableListOf<RoomEntity>()
    private val tiles = mutableListOf<RoomAssignment>()

    private val homeDao = mockk<HomeDao>().also { dao ->
        coEvery { dao.insert(any()) } answers { homes.add(firstArg()); Unit }
    }
    private val floorDao = mockk<FloorDao>().also { dao ->
        coEvery { dao.insert(any()) } answers { floors.add(firstArg()); Unit }
    }
    private val roomDao = mockk<RoomDao>().also { dao ->
        coEvery { dao.insert(any()) } answers { rooms.add(firstArg()); Unit }
    }
    private val assignmentDao = mockk<RoomAssignmentDao>().also { dao ->
        coEvery { dao.insert(any()) } answers { tiles.addAll(firstArg<List<RoomAssignment>>()); Unit }
    }
    private val seeder = DemoLayoutSeeder(
        homes = homeDao,
        floors = floorDao,
        rooms = roomDao,
        assignments = assignmentDao,
        transactions = object : DatabaseTransactions {
            override suspend fun <T> run(block: suspend () -> T): T = block()
        },
        errors = DefaultErrorFactory(),
    )

    @Test
    fun `creates one home with a level and three rooms for the instance`() = runTest {
        seeder.seed("demo", now = 5L)
        assertEquals(listOf("demo"), homes.map { it.serverId })
        assertEquals(listOf("Ground floor"), floors.map { it.name })
        assertEquals(listOf("Living room", "Kitchen", "Bedroom"), rooms.map { it.name })
        assertTrue(rooms.all { it.homeId == homes.single().id && it.floorId == floors.single().id })
    }

    @Test
    fun `places every demo entity once with growing sort orders`() = runTest {
        seeder.seed("demo", now = 5L)
        val entities = DemoWorldData.initial(0L, "").entities.map { it.entityId }
        assertEquals(entities.sorted(), tiles.map { it.entityId }.sorted())
        assertTrue(tiles.all { it.addedAt == 5L })
        rooms.forEach { room ->
            val orders = tiles.filter { it.roomId == room.id }.map { it.sortOrder }
            assertEquals(orders.sorted(), orders)
        }
    }
}
