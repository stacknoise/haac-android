package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.core.network.http.text
import com.stacknoise.haac.feature.entities.domain.EntityState
import kotlin.math.roundToLong
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

/**
 * HA's compressed state format of `haac_bridge/subscribe_entities` (concept 11.3): `s` state, `a` attributes,
 * `lc`/`lu` last changed/updated in seconds; `lu` is left out when it equals `lc`.
 */
internal object CompressedState {
    /** A full state of an `a` event, or null if it has no state. */
    fun full(compressed: JsonObject): EntityState? {
        val state = compressed.text("s") ?: return null
        val changed = compressed.millis("lc")
        return EntityState(
            state = state,
            attributes = compressed["a"] as? JsonObject ?: JsonObject(emptyMap()),
            lastChanged = changed,
            lastUpdated = compressed.millis("lu") ?: changed,
        )
    }

    /**
     * [current] after a `c` event: `+` holds new or changed values (attributes only the changed ones), `-`
     * the names of removed attributes. A new `lc` also sets the update time.
     */
    fun apply(current: EntityState, diff: JsonObject): EntityState {
        val added = diff["+"] as? JsonObject ?: JsonObject(emptyMap())
        val removed = ((diff["-"] as? JsonObject)?.get("a") as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty().toSet()
        val attributes = current.attributes.filterKeys { it !in removed } +
            (added["a"] as? JsonObject).orEmpty()
        val changed = added.millis("lc")
        return current.copy(
            state = added.text("s") ?: current.state,
            attributes = JsonObject(attributes),
            lastChanged = changed ?: current.lastChanged,
            lastUpdated = added.millis("lu") ?: changed ?: current.lastUpdated,
        )
    }

    /** A time in seconds as milliseconds (rounded, `1790406723.1` is not exact as a double), or null. */
    private fun JsonObject.millis(key: String): Long? =
        (this[key] as? JsonPrimitive)?.doubleOrNull?.let { (it * MS_PER_SECOND).roundToLong() }

    private const val MS_PER_SECOND = 1_000
}
