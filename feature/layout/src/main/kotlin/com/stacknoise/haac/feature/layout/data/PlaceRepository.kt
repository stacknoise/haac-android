package com.stacknoise.haac.feature.layout.data

import android.database.SQLException
import com.stacknoise.haac.core.database.layout.FloorDao
import com.stacknoise.haac.core.database.layout.FloorEntity
import com.stacknoise.haac.core.database.layout.HomeDao
import com.stacknoise.haac.core.database.layout.HomeEntity
import com.stacknoise.haac.core.database.layout.RoomDao
import com.stacknoise.haac.core.database.layout.RoomEntity
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.feature.layout.domain.Floor
import com.stacknoise.haac.feature.layout.domain.Home
import com.stacknoise.haac.feature.layout.domain.Places
import com.stacknoise.haac.feature.layout.domain.Room
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine

/** Reads the homes, floors and rooms of an instance (concept 6). */
class PlaceRepository @Inject constructor(
    private val homes: HomeDao,
    private val floors: FloorDao,
    private val rooms: RoomDao,
    private val errors: ErrorFactory,
) {
    /** The places of instance [serverId], updated on every change; database errors arrive as HAAC-DB-001. */
    fun places(serverId: String): Flow<Places> =
        combine(homes.observe(serverId), floors.observe(serverId), rooms.observe(serverId), ::toPlaces)
            .catch { e -> throw if (e is SQLException) errors.from(e) else e }

    /** The domain view of the rows; a room whose floor is being deleted shows directly in its home. */
    private fun toPlaces(homes: List<HomeEntity>, floors: List<FloorEntity>, rooms: List<RoomEntity>): Places {
        val floorIds = floors.map { it.id }.toSet()
        return Places(
            homes = homes.map { Home(it.id, it.name) },
            floors = floors.map { Floor(it.id, it.homeId, it.name, it.level) },
            rooms = rooms.map { Room(it.id, it.homeId, it.floorId?.takeIf(floorIds::contains), it.name) },
        )
    }
}
