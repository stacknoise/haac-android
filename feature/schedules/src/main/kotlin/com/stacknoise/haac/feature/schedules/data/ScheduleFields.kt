package com.stacknoise.haac.feature.schedules.data

import com.stacknoise.haac.feature.schedules.domain.RepeatRule
import com.stacknoise.haac.feature.schedules.domain.ScheduleDraft
import com.stacknoise.haac.feature.schedules.domain.WhenType
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Builds the fields of `schedules/create`, `schedules/update` and `schedules/preview` from an edited draft (concept
 * 11.2, 19.4, 19.10). `repeat` and `notify` are sent only if the bridge reports their [ScheduleFeatures], which
 * default to none (the fields of a bridge before them).
 */
internal object ScheduleFields {
    private val clock = DateTimeFormatter.ofPattern("HH:mm")

    /** All fields of a new schedule, switched on. */
    fun create(draft: ScheduleDraft, features: ScheduleFeatures = ScheduleFeatures()): JsonObject = buildJsonObject {
        put("name", draft.name.trim())
        put("enabled", true)
        put("when", trigger(draft))
        if (features.repeat) put("repeat", repeat(draft.repeat))
        if (features.runs && draft.notify) put("notify", true)
        put("action", draft.action.wire)
        put("entities", entities(draft))
    }

    /** The fields of `schedules/preview`: the trigger and the date rule of [draft]. */
    fun preview(draft: ScheduleDraft): JsonObject = buildJsonObject {
        put("when", trigger(draft))
        put("repeat", repeat(draft.repeat))
    }

    /**
     * Only what the user changed compared with [original]; the entities only if [withEntities] (the owner edits
     * them, an admin editing a foreign schedule does not). Empty if nothing changed.
     */
    fun changes(
        original: ScheduleDraft,
        draft: ScheduleDraft,
        withEntities: Boolean,
        features: ScheduleFeatures = ScheduleFeatures(),
    ): JsonObject = buildJsonObject {
        if (draft.name.trim() != original.name.trim()) put("name", draft.name.trim())
        if (draft.triggerDiffers(original)) put("when", trigger(draft))
        if (features.repeat && draft.repeat != original.repeat) put("repeat", repeat(draft.repeat))
        if (features.runs && draft.notify != original.notify) put("notify", draft.notify)
        if (draft.action != original.action) put("action", draft.action.wire)
        if (withEntities && draft.entityIds != original.entityIds) put("entities", entities(draft))
    }

    /** The `when` object: the type, the weekdays of a weekly rule and the time or the offset. */
    private fun trigger(draft: ScheduleDraft): JsonObject = buildJsonObject {
        put("type", draft.whenType.wire)
        if (draft.weekly) put("days", JsonArray(draft.days.sorted().map(::JsonPrimitive)))
        if (draft.whenType == WhenType.TIME) {
            put("time", draft.time.format(clock))
        } else {
            put("offset_min", draft.offsetMin)
        }
    }

    /** The `repeat` object: the frequency, the interval of a repeating rule and the dates that are set. */
    private fun repeat(rule: RepeatRule): JsonObject = buildJsonObject {
        put("freq", rule.freq.wire)
        if (!rule.once) put("interval", rule.interval)
        rule.startDate?.let { put("start_date", it.toString()) }
        rule.endDate?.takeIf { !rule.once }?.let { put("end_date", it.toString()) }
    }

    /** The entity ids as a JSON array. */
    private fun entities(draft: ScheduleDraft): JsonArray = JsonArray(draft.entityIds.map(::JsonPrimitive))
}
