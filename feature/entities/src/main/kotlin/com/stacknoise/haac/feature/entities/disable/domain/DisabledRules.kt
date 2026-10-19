package com.stacknoise.haac.feature.entities.disable.domain

import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.database.schedule.ScheduleEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** `off_reason` of a schedule the bridge switched off because the user deactivated one of its entities (7.5). */
const val OffReasonEntityDisabled = "entity_disabled"

/** Name of the feature flag in `haac_bridge/info` (concept 11.2). */
const val EntityDisableFeature = "entity_disable"

/**
 * The deactivated entities in the payload of `disabled_changed` or in the reply of `set_disabled`: a plain list of
 * `entity_id`s or an object with the list under `disabled` (concept 7.5, 11.2); null for anything else.
 */
fun disabledList(payload: JsonElement?): List<String>? {
    val array = when (payload) {
        is JsonArray -> payload
        is JsonObject -> payload["disabled"] as? JsonArray
        else -> null
    }
    return array?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
}

/**
 * The entities that stay deactivated after the bridge's entity list: those it marks in [listedDisabled], plus every
 * cached deactivated entity that is not in the list any more (withdrawn, 7.4), which stays deactivated (7.5).
 */
fun disabledAfterList(listedDisabled: List<String>, listedIds: Set<String>, cached: List<ExposedEntity>): List<String> =
    (listedDisabled + cached.filter { it.disabled && it.entityId !in listedIds }.map { it.entityId }).distinct()

/** [rows] from the bridge's list with the time of a deactivation that was already cached taken over from [cached]. */
fun withDisabledTimes(rows: List<ExposedEntity>, cached: List<ExposedEntity>): List<ExposedEntity> {
    val times = cached.filter { it.disabled }.mapNotNull { row -> row.disabledAt?.let { row.entityId to it } }.toMap()
    return rows.map { row -> if (row.disabled) times[row.entityId]?.let { row.copy(disabledAt = it) } ?: row else row }
}

/**
 * What the dialog (M-20) and the sheet (M-21) tell about the schedules: the names of the user's own schedules that
 * *Deactivate* switches off ([off]) and the ones that *Activate* switches on again ([on], concept 7.5).
 */
data class DisablePreview(val off: List<String> = emptyList(), val on: List<String> = emptyList())

/**
 * The preview for [entityId] from the cached [schedules] and the entities [disabled] now: an enabled own schedule
 * with the entity goes off; an own schedule that is off because of a deactivation goes on again if no other of its
 * entities is still deactivated. Schedules of other users (admins) are never touched.
 */
fun disablePreview(entityId: String, schedules: List<ScheduleEntity>, disabled: Set<String>): DisablePreview {
    val own = schedules.filter { it.own }.map { it to entityIdsOf(it.entityIds) }.filter { entityId in it.second }
    val off = own.filter { it.first.enabled }.map { it.first.name }
    val others = disabled - entityId
    val on = own.filter { (row, ids) -> row.offReason == OffReasonEntityDisabled && ids.none { it in others } }
        .map { it.first.name }
    return DisablePreview(off, on)
}

/** The entity ids of column `entity_ids`; a damaged value names none. */
private fun entityIdsOf(json: String): List<String> = try {
    Json.decodeFromString(ListSerializer(String.serializer()), json)
} catch (_: IllegalArgumentException) {
    emptyList()
}
