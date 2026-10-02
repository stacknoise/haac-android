package com.stacknoise.haac.core.network.demo

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/** The schedule commands (`haac_bridge/schedules/...`) of the demo bridge (concept 19.4); replies go to [emit]. */
internal class DemoScheduleCommands(
    private val world: DemoWorld,
    private val schedules: DemoSchedules,
    private val emit: (JsonObject) -> Unit,
) {
    /** Answers [type] for message [id] and returns true; false if [type] is not a schedule command. */
    fun handle(type: String, id: Int, message: JsonObject): Boolean {
        when (type) {
            "haac_bridge/schedules/revision" -> emit(DemoReplies.result(id, revision()))
            "haac_bridge/schedules/list" -> emit(DemoReplies.result(id, list()))
            "haac_bridge/schedules/create" -> create(id, message)
            "haac_bridge/schedules/update" -> update(id, message)
            "haac_bridge/schedules/delete" -> delete(id, message)
            "haac_bridge/schedules/run_now" -> runNow(id, message)
            else -> return false
        }
        return true
    }

    /** The revision and scope of the demo user's schedules. */
    fun revision(): JsonObject = buildJsonObject {
        put("revision", DemoScheduleWire.revision(world.snapshot().schedules))
        put("scope", DemoScheduleWire.SCOPE)
    }

    /** The result of `schedules/list`: revision, scope and the schedules with their next run. */
    private fun list(): JsonObject {
        val all = world.snapshot().schedules
        return buildJsonObject {
            put("revision", DemoScheduleWire.revision(all))
            put("scope", DemoScheduleWire.SCOPE)
            put("schedules", JsonArray(all.map(::descriptor)))
        }
    }

    /** Creates a schedule owned by the demo user, or answers HAB-SCH-001 or HAB-SCH-005. */
    private fun create(id: Int, message: JsonObject) {
        val snapshot = world.snapshot()
        when (val created = schedules.create(message, snapshot.schedules, snapshot.entities)) {
            is DemoOutcome.Failed -> emit(DemoReplies.failure(id, created))
            is DemoOutcome.Ok -> {
                emit(DemoReplies.result(id, descriptor(created.value)))
                world.setSchedules(snapshot.schedules + created.value)
            }
        }
    }

    /** Changes a schedule: HAB-SCH-003 for an unknown one, HAB-SCH-004 for an old `updated_at`. */
    private fun update(id: Int, message: JsonObject) {
        val snapshot = world.snapshot()
        val current = find(message)
        val outcome = if (current == null) {
            notFound()
        } else {
            schedules.update(message, current, message.text("updated_at"), snapshot.entities)
        }
        when (outcome) {
            is DemoOutcome.Failed -> emit(DemoReplies.failure(id, outcome))
            is DemoOutcome.Ok -> {
                emit(DemoReplies.result(id, descriptor(outcome.value)))
                world.setSchedules(snapshot.schedules.map { if (it.id == outcome.value.id) outcome.value else it })
            }
        }
    }

    /** Deletes a schedule; an unknown one is not an error. */
    private fun delete(id: Int, message: JsonObject) {
        val current = find(message)
        emit(DemoReplies.result(id))
        if (current != null) world.setSchedules(world.snapshot().schedules.filter { it.id != current.id })
    }

    /** Switches the entities of a schedule once and sets its `last_run`; the plan does not change. */
    private fun runNow(id: Int, message: JsonObject) {
        val snapshot = world.snapshot()
        val current = find(message)
        if (current == null) {
            emit(DemoReplies.failure(id, notFound()))
            return
        }
        val (run, switched) = schedules.run(current, snapshot.entities)
        emit(DemoReplies.result(id))
        world.setEntities(switched.filter { it !in snapshot.entities })
        world.setSchedules(snapshot.schedules.map { if (it.id == run.id) run else it })
    }

    /** The schedule `schedule_id` of [message], or null. */
    private fun find(message: JsonObject): DemoSchedule? =
        message.text("schedule_id")?.let { wanted -> world.snapshot().schedules.firstOrNull { it.id == wanted } }

    /** HAB-SCH-003. */
    private fun notFound() = DemoOutcome.Failed(DemoCodes.SCH_NOT_FOUND, "The schedule does not exist")

    /** [schedule] as the bridge sends it, with its next run. */
    private fun descriptor(schedule: DemoSchedule): JsonObject =
        DemoScheduleWire.descriptor(schedule, schedules.nextRun(schedule))

    /** The string [key], or null. */
    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
}
