package com.stacknoise.haac.feature.entities.disable.data

import com.stacknoise.haac.core.database.entity.EntityDisabledDao
import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.feature.entities.disable.domain.disabledAfterList
import com.stacknoise.haac.feature.entities.disable.domain.disabledList
import javax.inject.Inject
import kotlinx.serialization.json.JsonElement

/**
 * Keeps the cached deactivations (`exposed_entity.disabled`, concept 7.5, 12) equal to the bridge's: from the entity
 * list of a sync, from the event `disabled_changed` and from the reply of `set_disabled`. A deactivation the bridge
 * no longer lists because the entity was withdrawn stays (7.4). Nothing here changes the cache offline.
 */
class DisabledSync @Inject constructor(
    private val entities: ExposedEntityDao,
    private val disabled: EntityDisabledDao,
    private val errors: ErrorFactory,
) {
    /**
     * Makes [listedDisabled], the entities the bridge's entity list marks, the deactivated ones of instance
     * [serverId] as of [at]; [listedIds] are all entities of the list.
     */
    suspend fun fromList(serverId: String, listedDisabled: List<String>, listedIds: Set<String>, at: Long) {
        val cached = errors.database { entities.all(serverId) }
        apply(serverId, disabledAfterList(listedDisabled, listedIds, cached), at)
    }

    /** Makes the list in [payload] (event `disabled_changed`) the deactivated entities; other payloads are ignored. */
    suspend fun fromPayload(serverId: String, payload: JsonElement?, at: Long) {
        disabledList(payload)?.let { apply(serverId, it, at) }
    }

    /** Makes [ids] the deactivated entities of instance [serverId], the bridge's full list (concept 11.2). */
    suspend fun apply(serverId: String, ids: List<String>, at: Long) {
        errors.database { disabled.applyDisabled(serverId, ids, at) }
    }
}
