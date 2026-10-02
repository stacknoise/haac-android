package com.stacknoise.haac.core.network.demo

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/** The schedule fields after validation (concept 19.2); [time] is set for fixed times, [offsetMin] for sun events. */
internal class Plan(
    val name: String,
    val enabled: Boolean,
    val whenType: String,
    val time: String?,
    val days: List<Int>,
    val offsetMin: Int?,
    val action: String,
    val entities: List<String>,
)

/** The validation rules of the bridge for schedules (concept 19.2), applied to the fields of create and update. */
internal object DemoScheduleRules {
    /** Limit of schedules per user. */
    const val MAX_SCHEDULES = 50
    private const val MAX_NAME = 60
    private const val MAX_ENTITIES = 20
    private const val MAX_OFFSET = 180
    private val WEEKDAYS = 0..6
    private val TIME = Regex("([01][0-9]|2[0-3]):[0-5][0-9]")
    private val TYPES = setOf("time", "sunrise", "sunset")
    private val ACTIONS = setOf("turn_on", "turn_off", "toggle")

    /**
     * [fields] on top of [base] (nothing for a new schedule), checked against the rules of concept 19.2; HAB-SCH-001
     * with the first problem if they do not hold. [entities] are the exposed entities.
     */
    fun plan(fields: JsonObject, base: DemoSchedule?, entities: List<DemoEntity>): DemoOutcome<Plan> {
        val plan = merge(fields, base)
        val problem = nameProblem(plan) ?: triggerProblem(plan) ?: actionProblem(plan) ?: entityProblem(plan, entities)
        return if (problem == null) DemoOutcome.Ok(plan) else DemoOutcome.Failed(DemoCodes.SCH_INVALID, problem)
    }

    /** The given fields, else the value of [base]; a trigger of the other kind drops the fields of the old one. */
    private fun merge(fields: JsonObject, base: DemoSchedule?): Plan {
        val trigger = fields["when"] as? JsonObject
        val type = trigger?.text("type") ?: base?.whenType.orEmpty()
        val fixedTime = type == "time"
        return Plan(
            name = fields.text("name")?.trim() ?: base?.name.orEmpty(),
            enabled = (fields["enabled"] as? JsonPrimitive)?.booleanOrNull ?: base?.enabled ?: true,
            whenType = type,
            time = if (fixedTime) trigger?.text("time") ?: base?.time else null,
            days = trigger?.texts("days")?.map { it.toIntOrNull() ?: -1 } ?: base?.days.orEmpty(),
            offsetMin = if (fixedTime) null else trigger?.number("offset_min") ?: base?.offsetMin ?: 0,
            action = fields.text("action") ?: base?.action.orEmpty(),
            entities = fields.texts("entities") ?: base?.entities.orEmpty(),
        )
    }

    /** The name needs 1 to 60 characters. */
    private fun nameProblem(plan: Plan): String? =
        if (plan.name.length in 1..MAX_NAME) null else "The name needs 1 to $MAX_NAME characters"

    /** Trigger type, time of day or sun offset, and the weekdays. */
    private fun triggerProblem(plan: Plan): String? = when {
        plan.whenType !in TYPES -> "Unknown trigger ${plan.whenType}"
        plan.whenType == "time" && plan.time?.let(TIME::matches) != true -> "The time needs the form HH:MM"
        plan.whenType != "time" && plan.offsetMin !in -MAX_OFFSET..MAX_OFFSET -> "The offset is at most $MAX_OFFSET"
        plan.days.isEmpty() || plan.days.any { it !in WEEKDAYS } || plan.days.distinct() != plan.days ->
            "The weekdays need different numbers from 0 to 6"
        else -> null
    }

    /** The action is `turn_on`, `turn_off` or `toggle`. */
    private fun actionProblem(plan: Plan): String? =
        if (plan.action in ACTIONS) null else "Unknown action ${plan.action}"

    /** 1 to 20 different entities, all of them exposed switches. */
    private fun entityProblem(plan: Plan, entities: List<DemoEntity>): String? = when {
        plan.entities.size !in 1..MAX_ENTITIES || plan.entities.distinct() != plan.entities ->
            "A schedule needs 1 to $MAX_ENTITIES different entities"
        plan.entities.any { id -> entities.none { it.entityId == id && it.domain == "switch" } } ->
            "Only exposed switches can be scheduled"
        else -> null
    }

    /** The string [key], or null. */
    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    /** The strings of the array [key] (anything else in it reads as an empty string), or null without the array. */
    private fun JsonObject.texts(key: String): List<String>? =
        (this[key] as? JsonArray)?.map { (it as? JsonPrimitive)?.contentOrNull.orEmpty() }

    /** The integer [key], or null. */
    private fun JsonObject.number(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
}
