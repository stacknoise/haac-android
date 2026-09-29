package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.core.database.assignment.TileSize
import com.stacknoise.haac.core.database.entity.EntityStatus
import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.database.entity.displayName
import com.stacknoise.haac.feature.entities.domain.ControlRequest
import com.stacknoise.haac.feature.entities.domain.EntityControlFactory
import com.stacknoise.haac.feature.entities.domain.EntityDetail
import com.stacknoise.haac.feature.entities.domain.EntityState
import com.stacknoise.haac.feature.entities.domain.Tile
import com.stacknoise.haac.feature.entities.domain.TileFactory
import com.stacknoise.haac.feature.entities.domain.attributes
import com.stacknoise.haac.feature.entities.domain.readings
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

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
    fun tile(entity: ExposedEntity, alias: String?, size: TileSize, pending: ControlRequest? = null): Tile =
        tile(entity, alias, size, state(entity, pending))

    /**
     * The detail screen of [entity] (concept 8.1, 15.4): its tile, names, times, readings, attributes and every
     * control of its domain; no controls while it is unavailable or withdrawn.
     */
    fun detail(entity: ExposedEntity, alias: String?, pending: ControlRequest? = null): EntityDetail {
        val state = state(entity, pending)
        val tile = tile(entity, alias, factory.defaultSize(entity.domain), state)
        val reported = state?.attributes ?: JsonObject(emptyMap())
        return EntityDetail(
            tile = tile,
            state = state?.state,
            haName = entity.haName,
            configuredName = entity.configuredName,
            lastChanged = state?.lastChanged,
            lastUpdated = state?.lastUpdated,
            readings = readings(reported),
            attributes = attributes(reported),
            controls = if (tile.active && state != null) controls.detail(entity, state) else emptyList(),
        )
    }

    /** The tile of [entity] in [state] with its tile control. */
    private fun tile(entity: ExposedEntity, alias: String?, size: TileSize, state: EntityState?): Tile {
        val tile = factory.create(entity, state, entity.displayName(alias), size)
        return tile.copy(
            defaultName = entity.displayName(null),
            control = if (tile.active && state != null) controls.create(entity, state) else null,
        )
    }

    /** The cached state of [entity] with the result of [pending] applied. */
    private fun state(entity: ExposedEntity, pending: ControlRequest?): EntityState? =
        json.decodeState(entity.lastState)?.let { pending?.applyTo(it) ?: it }

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
