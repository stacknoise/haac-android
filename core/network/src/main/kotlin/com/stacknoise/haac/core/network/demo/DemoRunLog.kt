package com.stacknoise.haac.core.network.demo

import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** One entry of the demo run log (concept 19.11), in the fields of `schedules/runs`. */
internal class DemoRun(
    val seq: Long,
    val scheduleId: String,
    val name: String,
    val at: String,
    val result: String,
    val code: String?,
    val removed: Boolean,
    val notify: Boolean,
)

/**
 * The run log of the demo bridge (concept 19.11, 20.2). Demo schedules do not fire by themselves, so the log is
 * filled by *Run now*: it hears about every change of the schedules and records each `last_run` that is new since
 * the log was created, as a manual run. It lives in memory, so after an app restart it has a new `log_id` and the
 * app only sets its cursor. TODO(A5): record `notify` from the schedule once A3's demo rules carry the field; until
 * then every demo run is recorded with `notify = true`, so the entries of M-26 can be seen.
 */
internal class DemoRunLog(private val world: DemoWorld) : DemoWorldListener {
    private val logId = UUID.randomUUID().toString()
    private val started = world.clock.instant()
    private val entries = mutableListOf<DemoRun>()
    private val seen = mutableMapOf<String, String>()

    /** Answers `schedules/runs` for message [id] and returns true; false if [type] is another command. */
    fun handle(type: String, id: Int, message: JsonObject, emit: (JsonObject) -> Unit): Boolean {
        if (type != RUNS) return false
        val after = message["after_seq"]?.let { (it as? JsonPrimitive)?.longOrNull }
        val sameLog = (message["log_id"] as? JsonPrimitive)?.contentOrNull == logId
        emit(DemoReplies.result(id, reply(if (sameLog) after ?: 0 else 0)))
        return true
    }

    /** Nothing to do for entities. */
    override fun entitiesChanged(changes: List<EntityChange>) = Unit

    /** Records the schedules whose `last_run` is new since the log was created. */
    override fun schedulesChanged() {
        world.snapshot().schedules.forEach { schedule ->
            val last = schedule.lastRun ?: return@forEach
            val isNew = seen[schedule.id] != last.at && !Instant.parse(last.at).isBefore(started)
            seen[schedule.id] = last.at
            if (isNew) entries += run(schedule, last)
        }
    }

    /** The log entry for the new run [last] of [schedule]. */
    private fun run(schedule: DemoSchedule, last: DemoLastRun) = DemoRun(
        seq = entries.size + 1L,
        scheduleId = schedule.id,
        name = schedule.name,
        at = last.at,
        result = last.result,
        code = last.code,
        removed = false,
        notify = true,
    )

    /** The result of `schedules/runs`: the log id, its newest sequence number and the entries after [after]. */
    private fun reply(after: Long): JsonObject = buildJsonObject {
        put("log_id", logId)
        put("last_seq", entries.size.toLong())
        put("entries", JsonArray(entries.filter { it.seq > after }.map(::wire)))
    }

    /** [run] as the bridge sends it. */
    private fun wire(run: DemoRun): JsonObject = buildJsonObject {
        put("seq", run.seq)
        put("schedule_id", run.scheduleId)
        put("name", run.name)
        put("at", run.at)
        put("result", run.result)
        put("code", run.code?.let(::JsonPrimitive) ?: JsonNull)
        put("trigger", "manual")
        put("removed", run.removed)
        put("notify", run.notify)
    }

    /** The command of this log. */
    private companion object {
        const val RUNS = "haac_bridge/schedules/runs"
    }
}
