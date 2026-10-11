package com.stacknoise.haac.feature.entities.data

import android.database.SQLException
import com.stacknoise.haac.core.common.dispatch.DefaultDispatcher
import com.stacknoise.haac.core.database.assignment.EntityAliasDao
import com.stacknoise.haac.core.database.assignment.RoomAssignmentDao
import com.stacknoise.haac.core.database.entity.EntityStatus
import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.feature.entities.domain.CatalogEntry
import com.stacknoise.haac.feature.entities.domain.EntityDetail
import com.stacknoise.haac.feature.entities.domain.Tile
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn

/**
 * Reads the entities of an instance as tiles: per room for the grid, all of them for the picker (concept 7). Live
 * states come from [LiveStates]; the cache rows supply names and, offline, the last state. Tiles are built on the
 * default dispatcher.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EntityCatalog @Inject constructor(
    private val entities: ExposedEntityDao,
    private val aliases: EntityAliasDao,
    private val assignments: RoomAssignmentDao,
    private val builder: TileBuilder,
    private val pending: PendingStates,
    private val live: LiveStates,
    private val errors: ErrorFactory,
    @param:DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) {
    /**
     * The tiles of room [roomId] of instance [serverId] in their order, with live states and the result of
     * requests HA has not confirmed yet (M-05, M-08, concept 8.1).
     */
    fun roomTiles(serverId: String, roomId: String): Flow<List<Tile>> = assignments.observe(roomId)
        .flatMapLatest { rows ->
            // Only the states of this room's entities are watched, so other entities' events build no tiles here.
            val states = live.of(serverId, rows.map { it.entityId })
            combine(entities.observe(serverId), aliases.observe(serverId), pending.of(serverId), states) {
                    cached, aliased, sent, liveStates ->
                val byId = cached.associateBy { it.entityId }
                val alias = aliased.associate { it.entityId to it.alias }
                rows.map { row ->
                    byId[row.entityId]?.let {
                        val size = row.tileSize
                        builder.tile(it, alias[row.entityId], size, sent[row.entityId], liveStates[row.entityId])
                    } ?: builder.missing(serverId, row.entityId, alias[row.entityId], row.tileSize)
                }
            }
        }.flowOn(dispatcher).databaseErrors()

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

    /**
     * The detail of [entityId] of instance [serverId] with live state and pending requests (concept 8.1, 15.4);
     * null while the cache does not know it.
     */
    fun detail(serverId: String, entityId: String): Flow<EntityDetail?> {
        val row = entities.observeOne(serverId, entityId)
        return combine(row, aliases.observe(serverId), pending.of(serverId), live.of(serverId, entityId)) {
                entity, names, sent, liveState ->
            entity?.let {
                val alias = names.firstOrNull { name -> name.entityId == entityId }?.alias
                builder.detail(it, alias, sent[entityId], liveState)
            }
        }.flowOn(dispatcher).databaseErrors()
    }

    /** Database errors of a flow arrive as HAAC-DB-001. */
    private fun <T> Flow<T>.databaseErrors(): Flow<T> =
        catch { e -> throw if (e is SQLException) errors.from(e) else e }
}
