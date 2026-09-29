package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ValidationException
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
    /** Looks up domain, flag, service and data of [request] and checks them against [entity]. */
    override fun create(entity: ExposedEntity, request: ControlRequest): ServiceCall {
        val spec = spec(request)
        val flagged = spec.flag == null || hasFlag(entity.supportedFeatures, spec.flag)
        val supported = entity.domain == spec.domain && flagged
        if (!supported) throw ValidationException(ErrorCode.ENT_ACTION_NOT_SUPPORTED)
        return ServiceCall(entity.entityId, spec.service, JsonObject(spec.data))
    }

    /**
     * What [request] needs: `switch.turn_on` / `turn_off` for the state the user wants (not `toggle`, so a stale
     * state never inverts the intent), the climate services with their flags (8.4; HVAC mode needs none).
     */
    private fun spec(request: ControlRequest): CallSpec = when (request) {
        is ControlRequest.SwitchTo ->
            CallSpec(EntityDomains.SWITCH, null, if (request.on) "turn_on" else "turn_off")
        is ControlRequest.PowerTo -> if (request.on) {
            CallSpec(EntityDomains.CLIMATE, EntityDomains.CLIMATE_TURN_ON, "turn_on")
        } else {
            CallSpec(EntityDomains.CLIMATE, EntityDomains.CLIMATE_TURN_OFF, "turn_off")
        }
        is ControlRequest.SetHvacMode -> climate(null, "set_hvac_mode", "hvac_mode" to JsonPrimitive(request.mode))
        is ControlRequest.SetTemperature -> climate(
            EntityDomains.CLIMATE_TARGET_TEMPERATURE,
            SET_TEMPERATURE,
            number(EntityDomains.TEMPERATURE, request.target),
        )
        is ControlRequest.SetTemperatureRange -> climate(
            EntityDomains.CLIMATE_TARGET_RANGE,
            SET_TEMPERATURE,
            number(EntityDomains.TARGET_LOW, request.low),
            number(EntityDomains.TARGET_HIGH, request.high),
        )
        is ControlRequest.SetHumidity -> climate(
            EntityDomains.CLIMATE_TARGET_HUMIDITY,
            "set_humidity",
            number(EntityDomains.HUMIDITY, request.target),
        )
        is ControlRequest.SetMode ->
            climate(request.kind.flag, request.kind.service, request.kind.attribute to JsonPrimitive(request.mode))
    }

    /** A climate call with [flag], [service] and [data]. */
    private fun climate(flag: Int?, service: String, vararg data: Pair<String, JsonPrimitive>): CallSpec =
        CallSpec(EntityDomains.CLIMATE, flag, service, data.toMap())

    /** A number field of the service data. */
    private fun number(key: String, value: Double): Pair<String, JsonPrimitive> = key to JsonPrimitive(value)

    /** Domain, required flag (null for none), service and data of one request. */
    private class CallSpec(
        val domain: String,
        val flag: Int?,
        val service: String,
        val data: Map<String, JsonPrimitive> = emptyMap(),
    )

    /** Service shared by target temperature and range. */
    private companion object {
        const val SET_TEMPERATURE = "set_temperature"
    }
}
