package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.entity.EntityStatus
import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.error.HaacException

/**
 * What one sync found (concept 9.1 step 4): entities exposed for the first time, entities no longer exposed
 * (now withdrawn, 7.4) and withdrawn entities exposed again. Changed metadata is updated silently.
 */
data class SyncResult(
    val added: List<String> = emptyList(),
    val removed: List<String> = emptyList(),
    val restored: List<String> = emptyList(),
)

/** Result and time of the last sync, or the error that stopped it (concept 9.3, Diagnostics). */
data class SyncStatus(val result: SyncResult? = null, val syncedAt: Long? = null, val error: HaacException? = null)

/** The rows to write after a sync and what changed. */
data class ExposureChange(val rows: List<ExposedEntity>, val result: SyncResult)

/** Compares the cache with the entity list of the bridge (concept 9.1 step 4, 7.4). */
object ExposureDiff {
    /**
     * [listed] are the bridge's entities as active rows; a cached active entity missing there becomes
     * withdrawn at [now] and keeps its last state. Withdrawn entities that stay missing are not touched.
     */
    fun compute(cached: List<ExposedEntity>, listed: List<ExposedEntity>, now: Long): ExposureChange {
        val before = cached.associateBy { it.entityId }
        val listedIds = listed.mapTo(HashSet()) { it.entityId }
        val withdrawn = cached
            .filter { it.status == EntityStatus.ACTIVE && it.entityId !in listedIds }
            .map { it.copy(status = EntityStatus.WITHDRAWN, withdrawnAt = now) }
        val result = SyncResult(
            added = listed.filter { before[it.entityId] == null }.map { it.entityId }.sorted(),
            removed = withdrawn.map { it.entityId }.sorted(),
            restored = listed.filter { before[it.entityId]?.status == EntityStatus.WITHDRAWN }
                .map { it.entityId }.sorted(),
        )
        return ExposureChange(listed + withdrawn, result)
    }
}
