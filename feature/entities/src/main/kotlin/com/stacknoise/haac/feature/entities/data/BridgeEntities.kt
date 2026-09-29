package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.feature.entities.domain.EntityState
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Answer of `haac_bridge/exposure/revision` (concept 11.2). */
@Serializable
internal class ExposureRevision(val revision: String)

/** Answer of `haac_bridge/entities/list` (concept 11.3). */
@Serializable
internal class EntityList(val revision: String, val entities: List<EntityDescriptor> = emptyList())

/** One entity of `haac_bridge/entities/list` (concept 7.1, 11.3); sensor fields are null for other domains. */
@Serializable
internal class EntityDescriptor(
    @SerialName("entity_id") val entityId: String,
    val domain: String,
    val name: String? = null,
    @SerialName("configured_name") val configuredName: String? = null,
    @SerialName("device_class") val deviceClass: String? = null,
    @SerialName("supported_features") val supportedFeatures: Int = 0,
    val area: String? = null,
    val state: String,
    val attributes: JsonObject = JsonObject(emptyMap()),
    @SerialName("last_changed") val lastChanged: String? = null,
    @SerialName("last_updated") val lastUpdated: String? = null,
    @SerialName("unit_of_measurement") val unit: String? = null,
    @SerialName("state_class") val stateClass: String? = null,
    @SerialName("display_precision") val displayPrecision: Int? = null,
) {
    /** The state part of the descriptor. */
    fun entityState(): EntityState =
        EntityState(state, attributes, epochMillis(lastChanged), epochMillis(lastUpdated) ?: epochMillis(lastChanged))

    /** The cache row of instance [serverId] with [lastState] as its JSON state; always active. */
    fun toRow(serverId: String, lastState: String): ExposedEntity = ExposedEntity(
        serverId = serverId,
        entityId = entityId,
        domain = domain,
        haName = name ?: entityId,
        configuredName = configuredName,
        deviceClass = deviceClass,
        unit = unit,
        stateClass = stateClass,
        displayPrecision = displayPrecision,
        area = area,
        supportedFeatures = supportedFeatures,
        lastState = lastState,
    )

    /** Companion with the time parser. */
    private companion object {
        /** HA's ISO time such as `2026-09-26T07:12:03.123456+00:00` in milliseconds, or null. */
        fun epochMillis(iso: String?): Long? = try {
            iso?.let { OffsetDateTime.parse(it).toInstant().toEpochMilli() }
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
