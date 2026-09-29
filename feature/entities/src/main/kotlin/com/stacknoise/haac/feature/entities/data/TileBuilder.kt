package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.core.database.assignment.TileSize
import com.stacknoise.haac.core.database.entity.EntityStatus
import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.database.entity.displayName
import com.stacknoise.haac.feature.entities.domain.EntityState
import com.stacknoise.haac.feature.entities.domain.Tile
import com.stacknoise.haac.feature.entities.domain.TileFactory
import javax.inject.Inject
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Turns cache rows into tiles: decodes the last state and resolves the display name (concept 7.3). */
class TileBuilder @Inject constructor(private val json: Json, private val factory: TileFactory) {
    /** The tile of [entity] with its local [alias] and [size]. */
    fun tile(entity: ExposedEntity, alias: String?, size: TileSize): Tile =
        factory.create(entity, state(entity.lastState), entity.displayName(alias), size)
            .copy(defaultName = entity.displayName(null))

    /**
     * The tile of an assigned entity the cache does not know (e.g. removed before the first sync after an
     * update): inactive, named by its alias or `entity_id` (concept 7.4).
     */
    fun missing(serverId: String, entityId: String, alias: String?, size: TileSize): Tile {
        val placeholder = ExposedEntity(
            serverId = serverId,
            entityId = entityId,
            domain = entityId.substringBefore('.'),
            haName = entityId,
            status = EntityStatus.WITHDRAWN,
        )
        return tile(placeholder, alias, size)
    }

    /** Default size of a new tile of [domain]. */
    fun defaultSize(domain: String): TileSize = factory.defaultSize(domain)

    /** The cached state JSON, or null when there is none or it cannot be read (the tile then shows no state). */
    private fun state(lastState: String?): EntityState? = lastState?.let {
        try {
            json.decodeFromString(EntityState.serializer(), it)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
