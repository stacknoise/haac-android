package com.stacknoise.haac.core.network.demo

import java.security.MessageDigest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Builds the schedule parts of the bridge's replies (concept 11.2, 19.4), in the format of HAAC Bridge. */
internal object DemoScheduleWire {
    /** The scope of the schedules the demo user sees: only the own ones, as every non-admin user. */
    const val SCOPE = "own"

    /** One entry of `schedules/list`, `create` and `update` replies; the demo user owns every schedule; [disabled]
     * are the deactivated entities that block it (concept 7.5). */
    fun descriptor(
        schedule: DemoSchedule,
        nextRun: String?,
        disabled: List<String> = emptyList(),
    ): JsonObject = buildJsonObject {
        put("id", schedule.id)
        put("owner", DemoInstance.USER_ID)
        put("owner_name", DemoInstance.DISPLAY_NAME)
        put("own", true)
        put("name", schedule.name)
        put("enabled", schedule.enabled)
        put("when", trigger(schedule))
        put("action", schedule.action)
        put("entities", JsonArray(schedule.entities.map(::JsonPrimitive)))
        put("created_at", schedule.createdAt)
        put("updated_at", schedule.updatedAt)
        put("paused", JsonNull)
        put("last_run", schedule.lastRun?.let(::lastRun) ?: JsonNull)
        put("next_run", nextRun)
        put("repeat", weekly())
        put("notify", false)
        put("off_reason", schedule.offReason)
        put("blocked_by", JsonArray(blockedBy(schedule, disabled).map(::JsonPrimitive)))
        put("expired", false)
        put("next_runs", JsonArray(listOfNotNull(nextRun).map(::JsonPrimitive)))
    }

    /** The date rule of the schedules the demo knows so far: weekly, every week (concept 19.10). */
    private fun weekly(): JsonObject = buildJsonObject {
        put("freq", "weekly")
        put("interval", 1)
    }

    /** SHA-256 over `id`, `updated_at` and `last_run.at` of the schedules and the scope, as stable as the data. */
    fun revision(schedules: List<DemoSchedule>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        schedules.sortedBy { it.id }.forEach {
            digest.update("${it.id}\t${it.updatedAt}\t${it.lastRun?.at.orEmpty()}\n".toByteArray())
        }
        digest.update(SCOPE.toByteArray())
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** The `when` object of [schedule]: type and weekdays, plus the time or the offset. */
    private fun trigger(schedule: DemoSchedule): JsonObject = buildJsonObject {
        put("type", schedule.whenType)
        put("days", JsonArray(schedule.days.map(::JsonPrimitive)))
        schedule.time?.let { put("time", it) }
        schedule.offsetMin?.let { put("offset_min", it) }
    }

    /** The `last_run` object. */
    private fun lastRun(run: DemoLastRun): JsonObject = buildJsonObject {
        put("at", run.at)
        put("result", run.result)
        put("code", run.code)
    }
}
