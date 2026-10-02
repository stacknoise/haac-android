package com.stacknoise.haac.feature.schedules.data

import com.stacknoise.haac.feature.schedules.domain.ScheduleDraft
import com.stacknoise.haac.feature.schedules.domain.WhenType
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Builds the fields of `schedules/create` and `schedules/update` from an edited draft (concept 11.2, 19.4). */
internal object ScheduleFields {
    private val clock = DateTimeFormatter.ofPattern("HH:mm")

    /** All fields of a new schedule, switched on. */
    fun create(draft: ScheduleDraft): JsonObject = buildJsonObject {
        put("name", draft.name.trim())
        put("enabled", true)
        put("when", trigger(draft))
        put("action", draft.action.wire)
        put("entities", entities(draft))
    }

    /**
     * Only what the user changed compared with [original]; the entities only if [withEntities] (the owner edits
     * them, an admin editing a foreign schedule does not). Empty if nothing changed.
     */
    fun changes(original: ScheduleDraft, draft: ScheduleDraft, withEntities: Boolean): JsonObject = buildJsonObject {
        if (draft.name.trim() != original.name.trim()) put("name", draft.name.trim())
        if (draft.triggerDiffers(original)) put("when", trigger(draft))
        if (draft.action != original.action) put("action", draft.action.wire)
        if (withEntities && draft.entityIds != original.entityIds) put("entities", entities(draft))
    }

    /** The `when` object: the type, the weekdays and the time or the offset. */
    private fun trigger(draft: ScheduleDraft): JsonObject = buildJsonObject {
        put("type", draft.whenType.wire)
        put("days", JsonArray(draft.days.sorted().map(::JsonPrimitive)))
        if (draft.whenType == WhenType.TIME) {
            put("time", draft.time.format(clock))
        } else {
            put("offset_min", draft.offsetMin)
        }
    }

    /** The entity ids as a JSON array. */
    private fun entities(draft: ScheduleDraft): JsonArray = JsonArray(draft.entityIds.map(::JsonPrimitive))
}
