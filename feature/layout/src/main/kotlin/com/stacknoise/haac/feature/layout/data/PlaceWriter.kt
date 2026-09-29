package com.stacknoise.haac.feature.layout.data

import com.stacknoise.haac.core.database.DatabaseTransactions
import com.stacknoise.haac.core.database.layout.FloorEntity
import com.stacknoise.haac.core.database.layout.HomeEntity
import com.stacknoise.haac.core.database.layout.RoomEntity
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.ValidationException
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.feature.layout.domain.PlaceForm
import com.stacknoise.haac.feature.layout.domain.PlaceKind
import java.util.UUID
import javax.inject.Inject

/**
 * Creates and changes homes, levels and rooms from a [PlaceForm], each save in one transaction (concept 6.1,
 * 6.2). New places are sorted last, with gaps in the sort order for later reordering.
 */
class PlaceWriter internal constructor(
    private val daos: LayoutDaos,
    private val transactions: DatabaseTransactions,
    private val errors: ErrorFactory,
    private val newId: () -> String,
) {
    /** Uses random UUIDs as ids. */
    @Inject
    constructor(daos: LayoutDaos, transactions: DatabaseTransactions, errors: ErrorFactory) :
        this(daos, transactions, errors, { UUID.randomUUID().toString() })

    /**
     * Saves [form] for instance [serverId] and returns the place's id. Throws HAAC-LAY-002 for a blank name and
     * HAAC-LAY-001 if the place, its home or its level no longer exists.
     */
    suspend fun save(serverId: String, form: PlaceForm): String {
        val name = form.name.trim()
        if (name.isEmpty()) throw ValidationException(ErrorCode.LAY_NAME_MISSING)
        return errors.database {
            transactions.run {
                when (form.kind) {
                    PlaceKind.HOME -> saveHome(serverId, form, name)
                    PlaceKind.FLOOR -> saveFloor(form, name, requireHome(form.homeId))
                    PlaceKind.ROOM -> saveRoom(form, name, requireHome(form.homeId))
                }
            }
        }
    }

    /** A new or renamed home; checked rooms of other homes move into it without a level. */
    private suspend fun saveHome(serverId: String, form: PlaceForm, name: String): String {
        val id = form.id ?: newId()
        if (form.id == null) {
            daos.homes.insert(HomeEntity(id, serverId, name, sortOrder = next(daos.homes.maxSortOrder(serverId))))
        } else {
            found(daos.homes.rename(id, name))
        }
        if (form.roomIds.isNotEmpty()) daos.rooms.link(form.roomIds.toList(), id, null)
        addRooms(id, null, form.newRooms)
        return id
    }

    /** A new or changed level; checked rooms move onto it, unchecked rooms leave it and stay in the home. */
    private suspend fun saveFloor(form: PlaceForm, name: String, homeId: String): String {
        val id = form.id ?: newId()
        if (form.id == null) {
            val sortOrder = next(daos.floors.maxSortOrder(homeId))
            daos.floors.insert(FloorEntity(id, homeId, name, form.level, sortOrder = sortOrder))
        } else {
            requireFloorOf(id, homeId)
            daos.floors.update(id, name, form.level)
        }
        val onFloor = form.roomIds.toList()
        daos.rooms.releaseFloor(id, onFloor)
        if (onFloor.isNotEmpty()) daos.rooms.link(onFloor, homeId, id)
        addRooms(homeId, id, form.newRooms)
        return id
    }

    /** A new or changed room in home [homeId], on a level of that home or directly in it. */
    private suspend fun saveRoom(form: PlaceForm, name: String, homeId: String): String {
        form.floorId?.let { requireFloorOf(it, homeId) }
        val id = form.id ?: newId()
        if (form.id == null) {
            val sortOrder = next(daos.rooms.maxSortOrder(homeId))
            daos.rooms.insert(RoomEntity(id, homeId, form.floorId, name, sortOrder = sortOrder))
        } else {
            found(daos.rooms.update(id, name, homeId, form.floorId))
        }
        return id
    }

    /** Creates the rooms [names] in [homeId], on [floorId] or directly in the home. */
    private suspend fun addRooms(homeId: String, floorId: String?, names: List<String>) {
        var sortOrder = daos.rooms.maxSortOrder(homeId)
        names.map { it.trim() }.filter { it.isNotEmpty() }.forEach { name ->
            sortOrder = next(sortOrder)
            daos.rooms.insert(RoomEntity(newId(), homeId, floorId, name, sortOrder = sortOrder))
        }
    }

    /** [homeId] if that home exists and is not deleted, else HAAC-LAY-001. */
    private suspend fun requireHome(homeId: String?): String =
        homeId?.takeIf { daos.homes.isActive(it) > 0 } ?: missing()

    /** HAAC-LAY-001 unless floor [floorId] exists and belongs to [homeId] (concept 6.1). */
    private suspend fun requireFloorOf(floorId: String, homeId: String) {
        if (daos.floors.homeOf(floorId) != homeId) missing()
    }

    /** HAAC-LAY-001 if an update changed no row. */
    private fun found(rows: Int) {
        if (rows == 0) missing()
    }

    /** The sort order after [max], leaving a gap for reordering (concept 6.2). */
    private fun next(max: Int?): Int = (max ?: 0) + SORT_STEP

    /** Throws HAAC-LAY-001. */
    private fun missing(): Nothing = throw ValidationException(ErrorCode.LAY_PLACE_MISSING)

    /** Gap between sort orders. */
    private companion object {
        const val SORT_STEP = 1024
    }
}
