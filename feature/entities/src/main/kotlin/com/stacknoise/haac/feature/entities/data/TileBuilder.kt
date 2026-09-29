package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.core.database.assignment.TileSize
import com.stacknoise.haac.core.database.entity.EntityStatus
import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.database.entity.displayName
import com.stacknoise.haac.feature.entities.domain.ControlRequest
import com.stacknoise.haac.feature.entities.domain.EntityControlFactory
import com.stacknoise.haac.feature.entities.domain.Tile
import com.stacknoise.haac.feature.entities.domain.TileFactory
import javax.inject.Inject
import kotlinx.serialization.json.Json

/**
 * Turns cache rows into tiles: decodes the last state, applies a request HA has not confirmed yet (optimistic
 * UI, concept 8.1), resolves the display name (7.3) and adds the control (8).
 */
class TileBuilder @Inject constructor(
    private val json: Json,
    private val factory: TileFactory,
    private val controls: EntityControlFactory,
) {
    /** The tile of [entity] with its local [alias] and [size], showing the result of [pending] if there is one. */
    fun tile(entity: ExposedEntity, alias: String?, size: TileSize, pending: ControlRequest? = null): Tile {
        val cached = json.decodeState(entity.lastState)
        val state = cached?.let { pending?.applyTo(it) ?: it }
        val tile = factory.create(entity, state, entity.displayName(alias), size)
        val active = tile.available && !tile.withdrawn
        return tile.copy(
            defaultName = entity.displayName(null),
            control = if (active && state != null) controls.create(entity, state) else null,
        )
    }

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
}
