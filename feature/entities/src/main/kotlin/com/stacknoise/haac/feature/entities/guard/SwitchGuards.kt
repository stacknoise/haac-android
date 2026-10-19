package com.stacknoise.haac.feature.entities.guard

import android.database.SQLException
import com.stacknoise.haac.core.database.guard.EntityGuardDao
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.database
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Reads and writes the switch guards of an instance (concept 8.5); they stay on this device and never go to HA. */
class SwitchGuards @Inject constructor(
    private val dao: EntityGuardDao,
    private val errors: ErrorFactory,
) {
    /** The guards of instance [serverId] by `entity_id`, updated on every change. */
    fun observe(serverId: String): Flow<Map<String, SwitchGuard>> = dao.observe(serverId)
        .map { rows -> rows.mapNotNull { row -> row.toSwitchGuard()?.let { row.entityId to it } }.toMap() }
        .catch { e -> throw if (e is SQLException) errors.from(e) else e }

    /** Turns the guard of [entityId] on with [guard]'s direction, or off if [guard] is null. */
    suspend fun set(serverId: String, entityId: String, guard: SwitchGuard?) = errors.database {
        if (guard == null) dao.clear(serverId, entityId) else dao.set(guard.toRow(serverId, entityId))
    }
}
