package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.feature.entities.domain.ControlRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** An entity of one instance. */
data class EntityKey(val serverId: String, val entityId: String)

/**
 * Requests sent (or about to be sent) that HA has not confirmed yet (optimistic UI, concept 8.1). Tiles show
 * their result until [EntityController] clears them: on confirmation, on failure (rollback) or after a timeout.
 */
@Singleton
class PendingStates @Inject constructor() {
    private val requests = MutableStateFlow<Map<EntityKey, ControlRequest>>(emptyMap())

    /** The pending requests of instance [serverId] by `entity_id`. */
    fun of(serverId: String): Flow<Map<String, ControlRequest>> = requests
        .map { map -> map.filterKeys { it.serverId == serverId }.mapKeys { it.key.entityId } }
        .distinctUntilChanged()

    /** Shows [request] on the tile of [key] instead of an older pending one. */
    fun set(key: EntityKey, request: ControlRequest) = requests.update { it + (key to request) }

    /** Removes [request] of [key], unless a newer request replaced it meanwhile. */
    fun clear(key: EntityKey, request: ControlRequest) = requests.update { map ->
        if (map[key] === request) map - key else map
    }
}
