package com.stacknoise.haac.core.network.demo

import com.stacknoise.haac.core.network.http.text
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * The command `haac_bridge/schedules/skip_run` of the demo bridge (concept 19.12); the reply goes to [emit] and
 * carries the schedule as [describe] writes it.
 */
internal class DemoSkipCommand(
    private val world: DemoWorld,
    private val schedules: DemoSchedules,
    private val emit: (JsonObject) -> Unit,
    private val describe: (DemoSchedule) -> JsonObject,
) {
    /** Skips the run of `date` or takes it up again: HAB-SCH-003 for an unknown schedule, HAB-SCH-010 (19.12). */
    fun handle(id: Int, message: JsonObject) {
        val all = world.snapshot().schedules
        val current = message.text("schedule_id")?.let { wanted -> all.firstOrNull { it.id == wanted } }
        val date = message.text("date")
        val skip = (message["skip"] as? JsonPrimitive)?.booleanOrNull
        val outcome = when {
            current == null -> DemoOutcome.Failed(DemoCodes.SCH_NOT_FOUND, "The schedule does not exist")
            date == null || skip == null -> DemoOutcome.Failed(DemoCodes.WS_INVALID_REQUEST, "Invalid request")
            else -> schedules.skipRun(current, date, skip)
        }
        when (outcome) {
            is DemoOutcome.Failed -> emit(DemoReplies.failure(id, outcome))
            is DemoOutcome.Ok -> {
                emit(DemoReplies.result(id, describe(outcome.value)))
                world.setSchedules(all.map { if (it.id == outcome.value.id) outcome.value else it })
            }
        }
    }
}
