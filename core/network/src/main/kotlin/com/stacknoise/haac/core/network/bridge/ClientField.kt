package com.stacknoise.haac.core.network.bridge

import com.stacknoise.haac.core.database.settings.DeviceName
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** The optional `client` field (device name) of `call_service` and `schedules/run_now` (concept 11.2, 21.5). */
object ClientField {
    /** Feature flag of `haac_bridge/info`: the bridge accepts the field; older bridges reject unknown fields. */
    const val FEATURE = "client_info"

    /** The request field that carries the device name. */
    const val FIELD = "client"

    /**
     * [fields] plus `client` with the cleaned [name], only if the bridge lists [FEATURE] among its [features] and
     * the name is not empty; otherwise [fields] unchanged.
     */
    fun attach(fields: JsonObject, name: String?, features: Set<String>): JsonObject {
        val client = name?.let(DeviceName::clean)
        if (FEATURE !in features || client == null) return fields
        return JsonObject(fields + (FIELD to JsonPrimitive(client)))
    }
}
