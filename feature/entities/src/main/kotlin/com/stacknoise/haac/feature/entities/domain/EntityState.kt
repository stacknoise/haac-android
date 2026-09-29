package com.stacknoise.haac.feature.entities.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * The last known state of an entity as the cache stores it (column `last_state`, concept 8.1, 12); times in
 * milliseconds since the epoch.
 */
@Serializable
data class EntityState(
    val state: String,
    val attributes: JsonObject = JsonObject(emptyMap()),
    val lastChanged: Long? = null,
    val lastUpdated: Long? = null,
)
