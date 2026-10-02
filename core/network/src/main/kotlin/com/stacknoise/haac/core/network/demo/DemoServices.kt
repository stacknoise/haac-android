package com.stacknoise.haac.core.network.demo

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

/** The HAB error codes the demo bridge answers with (concept 18.3). */
internal object DemoCodes {
    const val SVC_NOT_AVAILABLE = "HAB-SVC-002"
    const val SVC_FAILED = "HAB-SVC-003"
    const val ENT_NOT_FOUND = "HAB-ENT-001"
    const val SCH_INVALID = "HAB-SCH-001"
    const val SCH_NOT_FOUND = "HAB-SCH-003"
    const val SCH_CONFLICT = "HAB-SCH-004"
    const val SCH_LIMIT = "HAB-SCH-005"
    const val WS_INVALID_REQUEST = "HAB-WS-001"
}

/** The result of a demo command: a value, or the HAB code and message of the error reply. */
internal sealed interface DemoOutcome<out T> {
    /** The command worked. */
    data class Ok<T>(val value: T) : DemoOutcome<T>

    /** The command fails with [code]; the reply carries [message]. */
    data class Failed(val code: String, val message: String) : DemoOutcome<Nothing>
}

/** [next] applied to the value of a successful outcome; a failure stays as it is. */
internal fun <T, R> DemoOutcome<T>.then(next: (T) -> DemoOutcome<R>): DemoOutcome<R> = when (this) {
    is DemoOutcome.Ok -> next(value)
    is DemoOutcome.Failed -> this
}

/** The services of `haac_bridge/call_service` on the demo entities (concept 20.3). */
internal object DemoServices {
    /** `supported_features` flags of a climate entity (concept 8.4). */
    private const val TARGET_TEMPERATURE = 1
    private const val TURN_OFF = 128
    private const val TURN_ON = 256

    /** HA's defaults of a climate entity in °C. */
    private const val DEFAULT_MIN = 7.0
    private const val DEFAULT_MAX = 30.0
    private const val DEFAULT_TARGET = 21.0

    /**
     * [entity] after [service] with [data] at [now], or HAB-SVC-002 if the entity's domain or features do not
     * offer the service, HAB-SVC-003 if HA would reject the values (outside the limits, unknown mode).
     */
    fun call(entity: DemoEntity, service: String, data: JsonObject, now: Long): DemoOutcome<DemoEntity> =
        when (entity.domain) {
            "switch" -> switchCall(entity, service, now)
            "climate" -> climateCall(entity, service, data, now)
            else -> notAvailable(entity, service)
        }

    /** `turn_on`, `turn_off` and `toggle` of a switch. */
    private fun switchCall(entity: DemoEntity, service: String, now: Long): DemoOutcome<DemoEntity> {
        val state = when (service) {
            "turn_on" -> "on"
            "turn_off" -> "off"
            "toggle" -> if (entity.state == "on") "off" else "on"
            else -> return notAvailable(entity, service)
        }
        return DemoOutcome.Ok(withState(entity, state, now))
    }

    /** The climate services the demo thermostat offers: target temperature, HVAC mode, power if flagged. */
    private fun climateCall(
        entity: DemoEntity,
        service: String,
        data: JsonObject,
        now: Long,
    ): DemoOutcome<DemoEntity> = when (service) {
        "set_temperature" -> setTemperature(entity, data, now)
        "set_hvac_mode" -> setMode(entity, (data["hvac_mode"] as? JsonPrimitive)?.contentOrNull, now)
        "turn_on" -> flagged(entity, TURN_ON, service) { setMode(entity, "heat", now) }
        "turn_off" -> flagged(entity, TURN_OFF, service) { setMode(entity, "off", now) }
        else -> notAvailable(entity, service)
    }

    /** The service needs `supported_features` [flag] of [entity]; otherwise HAB-SVC-002. */
    private fun flagged(
        entity: DemoEntity,
        flag: Int,
        service: String,
        call: () -> DemoOutcome<DemoEntity>,
    ): DemoOutcome<DemoEntity> =
        if (entity.supportedFeatures and flag != 0) call() else notAvailable(entity, service)

    /** A new target temperature inside `min_temp` and `max_temp`; HAB-SVC-002 without flag 1 or a temperature. */
    private fun setTemperature(entity: DemoEntity, data: JsonObject, now: Long): DemoOutcome<DemoEntity> {
        val target = (data["temperature"] as? JsonPrimitive)?.doubleOrNull
        val min = entity.attributes.number("min_temp") ?: DEFAULT_MIN
        val max = entity.attributes.number("max_temp") ?: DEFAULT_MAX
        return when {
            entity.supportedFeatures and TARGET_TEMPERATURE == 0 || "temperature" !in data ->
                notAvailable(entity, "set_temperature")
            target == null || target < min || target > max ->
                DemoOutcome.Failed(DemoCodes.SVC_FAILED, "Temperature ${data["temperature"]} is outside $min to $max")
            else -> DemoOutcome.Ok(climate(entity, entity.state, target, now))
        }
    }

    /** A new HVAC mode from `hvac_modes`. */
    private fun setMode(entity: DemoEntity, mode: String?, now: Long): DemoOutcome<DemoEntity> {
        val modes = (entity.attributes["hvac_modes"] as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        val target = entity.attributes.number("temperature") ?: DEFAULT_TARGET
        return if (mode != null && mode in modes) {
            DemoOutcome.Ok(climate(entity, mode, target, now))
        } else {
            DemoOutcome.Failed(DemoCodes.SVC_FAILED, "Unknown HVAC mode $mode")
        }
    }

    /** [entity] in [mode] with [target]; `hvac_action` follows from them (heating below the target). */
    private fun climate(entity: DemoEntity, mode: String, target: Double, now: Long): DemoEntity {
        val current = entity.attributes.number("current_temperature") ?: target
        val action = when {
            mode == "off" -> "off"
            current < target -> "heating"
            else -> "idle"
        }
        val attributes = JsonObject(
            entity.attributes + mapOf("temperature" to JsonPrimitive(target), "hvac_action" to JsonPrimitive(action)),
        )
        val changed = attributes != entity.attributes
        return withState(entity.copy(attributes = attributes), mode, now, attributesChanged = changed)
    }

    /** [entity] in [state]; times move only if something changed, `lastChanged` only if the state did. */
    private fun withState(
        entity: DemoEntity,
        state: String,
        now: Long,
        attributesChanged: Boolean = false,
    ): DemoEntity = when {
        entity.state != state -> entity.copy(state = state, lastChanged = now, lastUpdated = now)
        attributesChanged -> entity.copy(lastUpdated = now)
        else -> entity
    }

    /** HAB-SVC-002: [service] is not a service of [entity]. */
    private fun notAvailable(entity: DemoEntity, service: String): DemoOutcome.Failed =
        DemoOutcome.Failed(
            DemoCodes.SVC_NOT_AVAILABLE,
            "${entity.domain}.$service is not available for ${entity.entityId}",
        )

    /** An attribute as a number, or null. */
    private fun JsonObject.number(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull
}
