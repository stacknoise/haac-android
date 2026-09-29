package com.stacknoise.haac.feature.entities.data

import android.database.SQLException
import com.stacknoise.haac.core.database.assignment.EntityAliasDao
import com.stacknoise.haac.core.database.assignment.RoomAssignmentDao
import com.stacknoise.haac.core.database.entity.EntityStatus
import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.feature.entities.domain.CatalogEntry
import com.stacknoise.haac.feature.entities.domain.Tile
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine

/** Reads the entities of an instance as tiles: per room for the grid, all of them for the picker (concept 7). */
class EntityCatalog @Inject constructor(
    private val entities: ExposedEntityDao,
    private val aliases: EntityAliasDao,
    private val assignments: RoomAssignmentDao,
    private val builder: TileBuilder,
    private val pending: PendingStates,
    private val errors: ErrorFactory,
) {
    /**
     * The tiles of room [roomId] of instance [serverId] in their order, with live states and the result of
     * requests HA has not confirmed yet (M-05, M-08, concept 8.1).
     */
    fun roomTiles(serverId: String, roomId: String): Flow<List<Tile>> {
        val rooms = assignments.observe(roomId)
        val cache = entities.observe(serverId)
        return combine(rooms, cache, aliases.observe(serverId), pending.of(serverId)) { rows, cached, aliased, sent ->
            val byId = cached.associateBy { it.entityId }
            val alias = aliased.associate { it.entityId to it.alias }
            rows.map { row ->
                byId[row.entityId]?.let { builder.tile(it, alias[row.entityId], row.tileSize, sent[row.entityId]) }
                    ?: builder.missing(serverId, row.entityId, alias[row.entityId], row.tileSize)
            }
        }.databaseErrors()
    }

    /**
     * Every entity of [serverId] that can be assigned, i.e. still shared (concept 7.4), with the rooms it is in;
     * sorted by name (M-04).
     */
    fun assignable(serverId: String): Flow<List<CatalogEntry>> {
        val placed = assignments.observeAll(serverId)
        return combine(entities.observe(serverId), aliases.observe(serverId), placed) { cached, names, rows ->
            val alias = names.associate { it.entityId to it.alias }
            val rooms = rows.groupBy({ it.entityId }, { it.roomId })
            cached.filter { it.status == EntityStatus.ACTIVE }
                .map { entity ->
                    val tile = builder.tile(entity, alias[entity.entityId], builder.defaultSize(entity.domain))
                    CatalogEntry(entity.domain, tile, rooms[entity.entityId].orEmpty().toSet())
                }
                .sortedBy { it.tile.name.lowercase() }
        }.databaseErrors()
    }

    /** Database errors of a flow arrive as HAAC-DB-001. */
    private fun <T> Flow<T>.databaseErrors(): Flow<T> =
        catch { e -> throw if (e is SQLException) errors.from(e) else e }
}
