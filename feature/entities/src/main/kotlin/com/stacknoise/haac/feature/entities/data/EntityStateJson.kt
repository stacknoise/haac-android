package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.feature.entities.domain.EntityState
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * The cached state JSON of column `last_state`, or null when there is none or it cannot be read; tiles then show
 * no state instead of failing (concept 7.4, 8.1).
 */
internal fun Json.decodeState(lastState: String?): EntityState? = lastState?.let {
    try {
        decodeFromString(EntityState.serializer(), it)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
