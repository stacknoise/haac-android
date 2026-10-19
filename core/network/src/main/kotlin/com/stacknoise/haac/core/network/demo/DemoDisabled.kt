package com.stacknoise.haac.core.network.demo

import java.time.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/** The entities of [schedule] the user deactivated: its `blocked_by` (concept 7.5). */
internal fun blockedBy(schedule: DemoSchedule, disabled: List<String>): List<String> =
    schedule.entities.filter { it in disabled }

/**
 * The deactivation of entities in the demo bridge (concept 7.5, 20.3): `haac_bridge/entities/set_disabled` changes the
 * list of the demo user, switches the schedules that contain the entity off, or on again, and answers with the full
 * list and the ids of the changed schedules. The reply goes to [emit].
 */
internal class DemoDisabled(private val world: DemoWorld, private val emit: (JsonObject) -> Unit) {
    /** Answers [type] for message [id] and returns true; false if [type] is not a deactivation command. */
    fun handle(type: String, id: Int, message: JsonObject): Boolean {
        if (type != SET_DISABLED) return false
        val entityId = (message["entity_id"] as? JsonPrimitive)?.contentOrNull
        val disabled = (message["disabled"] as? JsonPrimitive)?.booleanOrNull
        val data = world.snapshot()
        when {
            entityId == null || disabled == null ->
                emit(DemoReplies.error(id, DemoCodes.WS_INVALID_REQUEST, "Invalid request"))
            data.entities.none { it.entityId == entityId } ->
                emit(DemoReplies.error(id, DemoCodes.ENT_NOT_FOUND, "$entityId does not exist"))
            else -> change(id, data, entityId, disabled)
        }
        return true
    }

    /** Applies the change, answers with the new list and the changed schedules, then tells the subscriptions. */
    private fun change(id: Int, data: DemoWorldData, entityId: String, disable: Boolean) {
        val list = data.disabled.filter { it != entityId }.let { if (disable) it + entityId else it }
        val now = world.clock.millis()
        val updated = if (disable) {
            switchOff(data.schedules, entityId, now)
        } else {
            switchOn(data.schedules, list, entityId, now)
        }
        val changed = updated.filter { it !in data.schedules }.map { it.id }
        emit(
            DemoReplies.result(
                id,
                buildJsonObject {
                    put("disabled", JsonArray(list.map(::JsonPrimitive)))
                    put("schedules_changed", JsonArray(changed.map(::JsonPrimitive)))
                },
            ),
        )
        if (changed.isNotEmpty()) world.setSchedules(updated)
        if (list != data.disabled) world.setDisabled(list)
    }

    /** [schedules] with every enabled one that contains [entityId] switched off because of it (concept 7.5). */
    private fun switchOff(schedules: List<DemoSchedule>, entityId: String, now: Long): List<DemoSchedule> =
        schedules.map {
            if (it.enabled && entityId in it.entities) it.changed(enabled = false, reason = OFF_REASON, now) else it
        }

    /** [schedules] with the ones that were off because of [entityId] switched on, if no entity of them is in [list]. */
    private fun switchOn(schedules: List<DemoSchedule>, list: List<String>, entityId: String, now: Long) =
        schedules.map {
            val again = it.offReason == OFF_REASON && entityId in it.entities && blockedBy(it, list).isEmpty()
            if (again) it.changed(enabled = true, reason = null, now) else it
        }

    /** The schedule with a new `enabled`, `off_reason` and a version token later than the old one. */
    private fun DemoSchedule.changed(enabled: Boolean, reason: String?, now: Long): DemoSchedule {
        val version = maxOf(now, Instant.parse(updatedAt).toEpochMilli() + 1)
        return copy(enabled = enabled, offReason = reason, updatedAt = DemoTimes.iso(version))
    }

    /** Command name and the reason of a schedule that is off because of a deactivated entity (concept 7.5). */
    private companion object {
        const val SET_DISABLED = "haac_bridge/entities/set_disabled"
        const val OFF_REASON = "entity_disabled"
    }
}
