package com.stacknoise.haac.feature.onboarding.data

import com.stacknoise.haac.core.database.DatabaseTransactions
import com.stacknoise.haac.core.database.assignment.RoomAssignment
import com.stacknoise.haac.core.database.assignment.RoomAssignmentDao
import com.stacknoise.haac.core.database.assignment.TileSize
import com.stacknoise.haac.core.database.layout.FloorDao
import com.stacknoise.haac.core.database.layout.FloorEntity
import com.stacknoise.haac.core.database.layout.HomeDao
import com.stacknoise.haac.core.database.layout.HomeEntity
import com.stacknoise.haac.core.database.layout.RoomDao
import com.stacknoise.haac.core.database.layout.RoomEntity
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.core.network.demo.DemoWorldData
import javax.inject.Inject

/** A room of the demo layout with its tiles as pairs of entity ID and tile size. */
private class DemoRoom(val id: String, val name: String, val tiles: List<Pair<String, TileSize>>)

/** The rooms of the demo layout, in the order they are shown (concept 20.3). */
private val DemoRooms = listOf(
    DemoRoom(
        "demo-room-living",
        "Living room",
        listOf(
            DemoWorldData.LIVING_ROOM_LIGHT to TileSize.SMALL,
            DemoWorldData.SOCKET to TileSize.SMALL,
            DemoWorldData.THERMOSTAT to TileSize.LARGE,
            DemoWorldData.TEMPERATURE to TileSize.SMALL,
        ),
    ),
    DemoRoom(
        "demo-room-kitchen",
        "Kitchen",
        listOf(DemoWorldData.KITCHEN_LIGHT to TileSize.SMALL, DemoWorldData.ENERGY to TileSize.WIDE),
    ),
    DemoRoom("demo-room-bedroom", "Bedroom", listOf(DemoWorldData.BEDROOM_LAMP to TileSize.SMALL)),
)

/** Gap between sort orders, as everywhere in the layout (concept 12). */
private const val SortStep = 1024

/**
 * Fills a new demo instance with a home, a level, three rooms and their tiles, so the demo shows something at
 * once (concept 20.4). The rows go in one transaction and vanish with the instance (cascade).
 */
class DemoLayoutSeeder @Inject constructor(
    private val homes: HomeDao,
    private val floors: FloorDao,
    private val rooms: RoomDao,
    private val assignments: RoomAssignmentDao,
    private val transactions: DatabaseTransactions,
    private val errors: ErrorFactory,
) {
    /** Creates the demo layout for instance [serverId]; the entities arrive with the first sync. */
    suspend fun seed(serverId: String, now: Long = System.currentTimeMillis()) = errors.database {
        transactions.run {
            homes.insert(HomeEntity(HOME_ID, serverId, "Demo home", sortOrder = SortStep))
            floors.insert(FloorEntity(FLOOR_ID, HOME_ID, "Ground floor", 0, sortOrder = SortStep))
            DemoRooms.forEachIndexed { index, room ->
                rooms.insert(RoomEntity(room.id, HOME_ID, FLOOR_ID, room.name, sortOrder = (index + 1) * SortStep))
                assignments.insert(
                    room.tiles.mapIndexed { i, (entityId, size) ->
                        RoomAssignment(room.id, entityId, (i + 1) * SortStep, size, now)
                    },
                )
            }
        }
    }

    /** The fixed IDs of the home and the level. */
    private companion object {
        const val HOME_ID = "demo-home"
        const val FLOOR_ID = "demo-floor"
    }
}
