package com.stacknoise.haac.feature.layout.data

import com.stacknoise.haac.core.database.DatabaseTransactions
import com.stacknoise.haac.core.database.layout.FloorEntity
import com.stacknoise.haac.core.database.layout.HomeEntity
import com.stacknoise.haac.core.database.layout.RoomEntity
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.ValidationException
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.feature.layout.domain.ImportPlan
import com.stacknoise.haac.feature.layout.domain.ImportResult
import java.util.UUID
import javax.inject.Inject

/** Where an import writes to: the existing home [homeId], or a new home called [newHomeName] when [homeId] is null. */
data class ImportTarget(val homeId: String?, val newHomeName: String = "")

/**
 * Writes an [ImportPlan] as ordinary levels and rooms of a home in one transaction (concept 6.3). Later changes in
 * Home Assistant are not followed.
 */
class AreaImporter internal constructor(
    private val daos: LayoutDaos,
    private val transactions: DatabaseTransactions,
    private val errors: ErrorFactory,
    private val newId: () -> String,
) {
    /** Uses random UUIDs as ids. */
    @Inject
    constructor(daos: LayoutDaos, transactions: DatabaseTransactions, errors: ErrorFactory) :
        this(daos, transactions, errors, { UUID.randomUUID().toString() })

    /** Creates what [plan] lists in [target] for instance [serverId]; HAAC-LAY-001 if the home is gone. */
    suspend fun import(serverId: String, target: ImportTarget, plan: ImportPlan): ImportResult = errors.database {
        transactions.run {
            val homeId = target.homeId?.also { requireHome(it) } ?: createHome(serverId, target.newHomeName)
            var floorOrder = daos.floors.maxSortOrder(homeId) ?: 0
            var roomOrder = daos.rooms.maxSortOrder(homeId) ?: 0
            var levels = 0
            plan.floors.forEach { planned ->
                val floorId = planned.existingId ?: newId().also { id ->
                    floorOrder += SORT_STEP
                    levels++
                    daos.floors.insert(FloorEntity(id, homeId, planned.name, planned.level, sortOrder = floorOrder))
                }
                planned.rooms.forEach { name ->
                    roomOrder += SORT_STEP
                    daos.rooms.insert(RoomEntity(newId(), homeId, floorId, name, sortOrder = roomOrder))
                }
            }
            plan.looseRooms.forEach { name ->
                roomOrder += SORT_STEP
                daos.rooms.insert(RoomEntity(newId(), homeId, null, name, sortOrder = roomOrder))
            }
            ImportResult(levels, plan.newRooms)
        }
    }

    /** A new home called [name], sorted last. */
    private suspend fun createHome(serverId: String, name: String): String {
        if (name.isBlank()) throw ValidationException(ErrorCode.LAY_NAME_MISSING)
        val id = newId()
        val order = (daos.homes.maxSortOrder(serverId) ?: 0) + SORT_STEP
        daos.homes.insert(HomeEntity(id, serverId, name.trim(), sortOrder = order))
        return id
    }

    /** HAAC-LAY-001 unless home [homeId] exists. */
    private suspend fun requireHome(homeId: String) {
        if (daos.homes.isActive(homeId) == 0) throw ValidationException(ErrorCode.LAY_PLACE_MISSING)
    }

    /** Gap between sort orders. */
    private companion object {
        const val SORT_STEP = 1024
    }
}
