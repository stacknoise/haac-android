package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.core.database.DatabaseTransactions
import com.stacknoise.haac.core.database.assignment.EntityAlias
import com.stacknoise.haac.core.database.assignment.EntityAliasDao
import com.stacknoise.haac.core.database.assignment.RoomAssignment
import com.stacknoise.haac.core.database.assignment.RoomAssignmentDao
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.ValidationException
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.feature.entities.domain.CatalogEntry
import javax.inject.Inject

/**
 * Places entities in rooms, removes them and sets local names (concept 7.2, 7.3). Nothing of this is sent to HA.
 */
class AssignmentWriter internal constructor(
    private val assignments: RoomAssignmentDao,
    private val aliases: EntityAliasDao,
    private val transactions: DatabaseTransactions,
    private val errors: ErrorFactory,
    private val clock: () -> Long,
) {
    /** Uses the wall clock for `addedAt`. */
    @Inject
    constructor(
        assignments: RoomAssignmentDao,
        aliases: EntityAliasDao,
        transactions: DatabaseTransactions,
        errors: ErrorFactory,
    ) : this(assignments, aliases, transactions, errors, System::currentTimeMillis)

    /**
     * Adds [entries] at the end of room [roomId] with their default tile size; entities already in the room stay
     * where they are. Throws HAAC-LAY-001 if the room no longer exists.
     */
    suspend fun add(roomId: String, entries: List<CatalogEntry>) = errors.database {
        transactions.run {
            if (assignments.roomIsActive(roomId) == 0) throw ValidationException(ErrorCode.LAY_PLACE_MISSING)
            val now = clock()
            val start = assignments.maxSortOrder(roomId) ?: 0
            val rows = entries.filter { roomId !in it.roomIds }.mapIndexed { index, entry ->
                RoomAssignment(roomId, entry.entityId, start + (index + 1) * SORT_STEP, entry.tile.size, now)
            }
            assignments.insert(rows)
        }
    }

    /** *Remove from room*: only the local assignment goes (concept 7.2). */
    suspend fun remove(roomId: String, entityId: String) = errors.database { assignments.remove(roomId, entityId) }

    /** *Remove from all rooms* of instance [serverId] (concept 7.4). */
    suspend fun removeEverywhere(serverId: String, entityIds: List<String>) =
        errors.database { assignments.removeEverywhere(serverId, entityIds) }

    /** Sets the local name of [entityId]; a blank [alias] restores the default name (M-07 *Use default name*). */
    suspend fun rename(serverId: String, entityId: String, alias: String?) = errors.database {
        val name = alias?.trim().orEmpty()
        if (name.isEmpty()) aliases.clear(serverId, entityId) else aliases.set(EntityAlias(serverId, entityId, name))
    }

    /** Gap between sort orders, for later reordering (concept 7.2). */
    private companion object {
        const val SORT_STEP = 1024
    }
}
