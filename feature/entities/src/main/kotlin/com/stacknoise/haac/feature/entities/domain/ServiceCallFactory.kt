package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ValidationException
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * A request of `haac_bridge/call_service` (concept 11.2): [service] of the entity's own domain without the
 * domain prefix, and [data] without target keys; the bridge sets the target itself (11.4).
 */
data class ServiceCall(val entityId: String, val service: String, val data: JsonObject = JsonObject(emptyMap())) {
    /** The request fields `entity_id`, `service` and `service_data`. */
    fun fields(): JsonObject = buildJsonObject {
        put("entity_id", entityId)
        put("service", service)
        put("service_data", data)
    }
}

/** Builds service calls, checked against the entity's domain and `supported_features` (concept 8, 17.2). */
interface ServiceCallFactory {
    /** The call that carries out [request] on [entity]; HAAC-ENT-002 if the entity does not support it. */
    fun create(entity: ExposedEntity, request: ControlRequest): ServiceCall
}

/** [ServiceCallFactory] for the controls of v1 (concept 8.2, 8.4). */
class DefaultServiceCallFactory @Inject constructor() : ServiceCallFactory {
    /**
     * `switch.turn_on` / `turn_off` for the state the user wants (not `toggle`, so a stale state never inverts
     * the intent) and `climate.set_temperature` with flag 1.
     */
    override fun create(entity: ExposedEntity, request: ControlRequest): ServiceCall = when (request) {
        is ControlRequest.SwitchTo -> {
            ensure(entity.domain == EntityDomains.SWITCH)
            ServiceCall(entity.entityId, if (request.on) "turn_on" else "turn_off")
        }
        is ControlRequest.SetTemperature -> {
            ensure(entity.domain == EntityDomains.CLIMATE)
            ensure(entity.supportedFeatures and EntityDomains.CLIMATE_TARGET_TEMPERATURE != 0)
            ServiceCall(
                entity.entityId,
                "set_temperature",
                buildJsonObject { put(EntityDomains.TEMPERATURE, request.target) },
            )
        }
    }

    /** Throws HAAC-ENT-002 unless [supported]. */
    private fun ensure(supported: Boolean) {
        if (!supported) throw ValidationException(ErrorCode.ENT_ACTION_NOT_SUPPORTED)
    }
}
