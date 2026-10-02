package com.stacknoise.haac.feature.schedules.data

import com.stacknoise.haac.core.database.assignment.RoomAssignmentDao
import com.stacknoise.haac.core.database.entity.EntityStatus
import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.database.entity.displayName
import com.stacknoise.haac.core.database.layout.PlaceRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** A room of the user, for the filter chips of the picker and the room line of a tile (concept 19.7). */
data class RoomChip(val id: String, val name: String)

/** An entity a schedule can switch, with the rooms the user placed it in (concept 19.2, M-14). */
data class Candidate(val entityId: String, val name: String, val rooms: List<RoomChip>, val active: Boolean)

/** The entities and rooms the picker shows. */
data class Candidates(val entities: List<Candidate> = emptyList(), val rooms: List<RoomChip> = emptyList()) {
    /** The display name of [entityId], or the id itself if the entity is not cached. */
    fun nameOf(entityId: String): String = entities.firstOrNull { it.entityId == entityId }?.name ?: entityId

    /** The entities placed in room [roomId], or all of them if it is null. */
    fun inRoom(roomId: String?): List<Candidate> =
        if (roomId == null) entities else entities.filter { candidate -> candidate.rooms.any { it.id == roomId } }
}

/**
 * The entities of the active instance a schedule may switch: the domain `switch` (concept 19.1), in the order of
 * their names. Entities the bridge withdrew stay listed, so an existing schedule can still name them, but are not
 * [Candidate.active].
 */
@Singleton
class ScheduleCandidates @Inject constructor(
    private val entities: ExposedEntityDao,
    private val places: PlaceRepository,
    private val assignments: RoomAssignmentDao,
) {
    /** The switches of instance [serverId] with their rooms, updated on every change. */
    fun of(serverId: String): Flow<Candidates> = combine(
        entities.observe(serverId),
        places.places(serverId),
        assignments.observeAll(serverId),
    ) { rows, layout, placed ->
        val rooms = layout.rooms.map { RoomChip(it.id, it.name) }
        val byId = rooms.associateBy { it.id }
        val roomsOf = placed.groupBy({ it.entityId }, { byId[it.roomId] }).mapValues { it.value.filterNotNull() }
        val list = rows.filter { it.domain == SWITCH_DOMAIN }.map {
            val active = it.status == EntityStatus.ACTIVE
            Candidate(it.entityId, it.displayName(null), roomsOf[it.entityId].orEmpty(), active)
        }
        Candidates(list.sortedBy { it.name.lowercase() }, rooms)
    }

    private companion object {
        const val SWITCH_DOMAIN = "switch"
    }
}
