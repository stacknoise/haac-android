package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.assignment.TileSize
import com.stacknoise.haac.core.database.entity.EntityStatus
import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.feature.entities.domain.EntityDomains.CLIMATE
import com.stacknoise.haac.feature.entities.domain.EntityDomains.OFF
import com.stacknoise.haac.feature.entities.domain.EntityDomains.ON
import com.stacknoise.haac.feature.entities.domain.EntityDomains.SENSOR
import com.stacknoise.haac.feature.entities.domain.EntityDomains.SWITCH
import com.stacknoise.haac.feature.entities.domain.EntityDomains.UNAVAILABLE
import java.math.RoundingMode
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Builds tiles per entity domain (concept 7.2, 17.2): default size, icon and content. The only place that
 * branches on the domain to build a tile.
 */
interface TileFactory {
    /** Default size of a new tile of [domain]: 2×2 for climate, else 1×1. */
    fun defaultSize(domain: String): TileSize

    /** The tile of [entity] with its last [state], display [name] and [size]. */
    fun create(entity: ExposedEntity, state: EntityState?, name: String, size: TileSize): Tile
}

/** [TileFactory] for switch, sensor and climate (concept 1.2, 8). */
class DefaultTileFactory @Inject constructor() : TileFactory {
    /** Climate tiles hold the dial, everything else fits 1×1. */
    override fun defaultSize(domain: String): TileSize = if (domain == CLIMATE) TileSize.LARGE else TileSize.SMALL

    /** Picks icon and content by domain; `unavailable` and `unknown` states mark the tile unavailable. */
    override fun create(entity: ExposedEntity, state: EntityState?, name: String, size: TileSize): Tile {
        val raw = state?.state
        val content = when (entity.domain) {
            SWITCH -> TileContent.Switch(if (raw == ON || raw == OFF) raw == ON else null)
            SENSOR -> sensor(entity, raw.orEmpty())
            CLIMATE -> climate(state?.attributes)
            else -> TileContent.Other(raw.orEmpty())
        }
        return Tile(
            entityId = entity.entityId,
            name = name,
            icon = icon(entity.domain, entity.deviceClass),
            size = size,
            content = content,
            available = raw != null && raw !in UNAVAILABLE,
            withdrawn = entity.status == EntityStatus.WITHDRAWN,
        )
    }

    /** Icon by domain and device class (concept 8.2, 8.3). */
    private fun icon(domain: String, deviceClass: String?): TileIcon = when (domain) {
        SWITCH -> if (deviceClass == "outlet") TileIcon.OUTLET else TileIcon.SWITCH
        CLIMATE -> TileIcon.CLIMATE
        else -> SENSOR_ICONS[deviceClass] ?: TileIcon.SENSOR
    }

    /** A timestamp sensor as a time, any other sensor as its rounded value with unit (concept 8.3). */
    private fun sensor(entity: ExposedEntity, raw: String): TileContent {
        val at = if (entity.deviceClass == EntityDomains.TIMESTAMP) isoEpochMillis(raw) else null
        return at?.let(TileContent::Timestamp)
            ?: TileContent.Sensor(sensorValue(raw, entity.displayPrecision), entity.unit)
    }


    /** A numeric state rounded to [precision] digits (`suggested_display_precision`), other states unchanged. */
    private fun sensorValue(raw: String, precision: Int?): String {
        val number = raw.toBigDecimalOrNull() ?: return raw
        return if (precision == null) raw else number.setScale(precision, RoundingMode.HALF_UP).toPlainString()
    }

    /** Target and current temperature with one decimal, heating from `hvac_action` (concept 8.4). */
    private fun climate(attributes: JsonObject?): TileContent.Climate = TileContent.Climate(
        target = attributes?.number(EntityDomains.TEMPERATURE)?.let(::degrees),
        current = attributes?.number("current_temperature")?.let(::degrees),
        heating = (attributes?.get("hvac_action") as? JsonPrimitive)?.content == "heating",
    )



    /** Sensor icons by device class. */
    private companion object {
        val SENSOR_ICONS = mapOf(
            "temperature" to TileIcon.TEMPERATURE,
            "humidity" to TileIcon.HUMIDITY,
            "power" to TileIcon.POWER,
            "energy" to TileIcon.ENERGY,
            "battery" to TileIcon.BATTERY,
        )
    }
}
