package com.stacknoise.haac.core.network.demo

import java.security.MessageDigest
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Times in the formats of the bridge: ISO strings and Unix seconds. */
internal object DemoTimes {
    /** [ms] as an ISO 8601 UTC time such as `2026-10-02T07:00:00Z`. */
    fun iso(ms: Long): String = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(ms))

    /** [ms] as Unix seconds with a fraction, as HA's compressed states carry them. */
    fun seconds(ms: Long): Double = ms / MS_PER_SECOND

    private const val MS_PER_SECOND = 1_000.0
}

/** Builds the entity parts of the bridge's replies and events (concept 11.3), in the format of HAAC Bridge. */
internal object DemoEntityWire {
    /** The attributes of [entity] as HA reports them: its own, plus name, device class, unit and state class. */
    fun attributes(entity: DemoEntity): JsonObject = buildJsonObject {
        put("friendly_name", entity.name)
        entity.deviceClass?.let { put("device_class", it) }
        entity.unit?.let { put("unit_of_measurement", it) }
        entity.stateClass?.let { put("state_class", it) }
        if (entity.supportedFeatures != 0) put("supported_features", entity.supportedFeatures)
        entity.attributes.forEach { (key, value) -> put(key, value) }
    }

    /** One entry of `haac_bridge/entities/list`; [area] is the name of the entity's area. */
    fun descriptor(entity: DemoEntity, area: String?): JsonObject = buildJsonObject {
        put("entity_id", entity.entityId)
        put("domain", entity.domain)
        put("name", entity.name)
        put("configured_name", JsonNull)
        put("device_class", entity.deviceClass)
        put("supported_features", entity.supportedFeatures)
        put("area", area)
        put("state", entity.state)
        put("attributes", attributes(entity))
        put("last_changed", DemoTimes.iso(entity.lastChanged))
        put("last_updated", DemoTimes.iso(entity.lastUpdated))
        put("unit_of_measurement", entity.unit)
        put("state_class", entity.stateClass)
        put("display_precision", entity.displayPrecision)
    }

    /** The full state of an `a` event: `s`, `a`, `lc` and, if it differs, `lu` (Unix seconds). */
    fun compressed(entity: DemoEntity): JsonObject = buildJsonObject {
        put("s", entity.state)
        put("a", attributes(entity))
        put("lc", DemoTimes.seconds(entity.lastChanged))
        if (entity.lastUpdated != entity.lastChanged) put("lu", DemoTimes.seconds(entity.lastUpdated))
    }

    /**
     * What a `c` event carries for [after] compared with [before]: `+` with the new state (and `lc` if the state
     * changed, else `lu`) and the changed attributes, `-` with the names of removed attributes.
     */
    fun diff(before: DemoEntity, after: DemoEntity): JsonObject {
        val old = attributes(before)
        val new = attributes(after)
        val changed = JsonObject(new.filter { (key, value) -> old[key] != value })
        val removed = old.keys.filter { it !in new }
        return buildJsonObject {
            put(
                "+",
                buildJsonObject {
                    if (before.state != after.state) put("s", after.state)
                    if (changed.isNotEmpty()) put("a", changed)
                    if (before.state != after.state || before.lastChanged != after.lastChanged) {
                        put("lc", DemoTimes.seconds(after.lastChanged))
                    } else {
                        put("lu", DemoTimes.seconds(after.lastUpdated))
                    }
                },
            )
            if (removed.isNotEmpty()) put("-", buildJsonObject { put("a", JsonArray(removed.map(::JsonPrimitive))) })
        }
    }

    /** The exposure revision: SHA-256 over the sorted entity IDs, each followed by a line break (as the bridge). */
    fun revision(entities: List<DemoEntity>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        entities.map { it.entityId }.sorted().forEach { digest.update("$it\n".toByteArray()) }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
