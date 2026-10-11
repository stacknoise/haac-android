package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.feature.entities.domain.EntityState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * The live states of the entities of every instance, in memory (concept 8.1, 9.2). [EntitySync] applies the events
 * of the subscription here, so a change reaches the tiles without a detour through the database; the database only
 * keeps the last known state for the offline display (14.1) and is written in batches.
 */
@Singleton
class LiveStates @Inject constructor() {
    private val states = MutableStateFlow<Map<String, Map<String, EntityState>>>(emptyMap())

    /** The state of [entityId] of [serverId], or null if none arrived yet. */
    fun current(serverId: String, entityId: String): EntityState? = states.value[serverId]?.get(entityId)

    /** The states of the entities [entityIds] of instance [serverId] (null if unknown); emits only when one changed. */
    fun of(serverId: String, entityIds: List<String>): Flow<Map<String, EntityState?>> = states
        .map { all -> entityIds.associateWith { all[serverId]?.get(it) } }
        .distinctUntilChanged()

    /** The state of [entityId] of [serverId]; emits only when it changed. */
    fun of(serverId: String, entityId: String): Flow<EntityState?> = states
        .map { all -> all[serverId]?.get(entityId) }
        .distinctUntilChanged()

    /** Sets [changes] for instance [serverId]. */
    fun putAll(serverId: String, changes: Map<String, EntityState>) {
        if (changes.isEmpty()) return
        states.update { all -> all + (serverId to (all[serverId].orEmpty() + changes)) }
    }

    /** Adds [known] for instance [serverId], except for entities that already have a live state. */
    fun seed(serverId: String, known: Map<String, EntityState>) {
        if (known.isEmpty()) return
        states.update { all -> all + (serverId to (known + all[serverId].orEmpty())) }
    }
}
