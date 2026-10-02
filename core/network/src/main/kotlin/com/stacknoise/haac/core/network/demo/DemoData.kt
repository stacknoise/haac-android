package com.stacknoise.haac.core.network.demo

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * One entity of the demo. [attributes] holds the domain's own attributes; name, device class, unit, state class
 * and `supported_features` are added when the entity is sent, as HA reports them among the attributes.
 */
@Serializable
data class DemoEntity(
    val entityId: String,
    val name: String,
    val state: String,
    val lastChanged: Long,
    val lastUpdated: Long,
    val areaId: String? = null,
    val deviceClass: String? = null,
    val unit: String? = null,
    val stateClass: String? = null,
    val displayPrecision: Int? = null,
    val supportedFeatures: Int = 0,
    val attributes: JsonObject = JsonObject(emptyMap()),
) {
    /** The domain, the part of the entity ID before the dot. */
    val domain: String get() = entityId.substringBefore('.')
}

/** A floor of the demo (concept 6.3). */
@Serializable
data class DemoFloor(val id: String, val name: String, val level: Int?)

/** An area of the demo; [floorId] is null for an area without a floor. */
@Serializable
data class DemoArea(val id: String, val name: String, val floorId: String?)

/** The outcome of the last run of a schedule (concept 19.2); [at] is an ISO time. */
@Serializable
data class DemoLastRun(val at: String, val result: String, val code: String? = null)

/** A schedule of the demo user (concept 19.2); the times are ISO strings as the bridge sends them. */
@Serializable
data class DemoSchedule(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val whenType: String,
    val time: String?,
    val days: List<Int>,
    val offsetMin: Int?,
    val action: String,
    val entities: List<String>,
    val createdAt: String,
    val updatedAt: String,
    val lastRun: DemoLastRun? = null,
)

/** Everything `demo-world.json` holds (concept 20.5). */
@Serializable
data class DemoWorldData(
    val entities: List<DemoEntity>,
    val floors: List<DemoFloor>,
    val areas: List<DemoArea>,
    val schedules: List<DemoSchedule>,
) {
    /** The IDs the demo refers to and the data it starts with (concept 20.3). */
    @Suppress("MagicNumber") // the sample data reads best as literals
    companion object {
        const val LIVING_ROOM = "living_room"
        const val KITCHEN = "kitchen"
        const val BEDROOM = "bedroom"
        const val LIVING_ROOM_LIGHT = "switch.demo_living_room_light"
        const val MORNING_LIGHT = "demo-morning-light"
        const val THERMOSTAT = "climate.demo_thermostat"

        /** Switches, sensors, thermostat, a floor with three areas and "Morning light", all created at [now]. */
        fun initial(now: Long, nowIso: String): DemoWorldData = DemoWorldData(
            entities = switches(now) + sensors(now) + thermostat(now),
            floors = listOf(DemoFloor("ground_floor", "Ground floor", 0)),
            areas = listOf(
                DemoArea(LIVING_ROOM, "Living room", "ground_floor"),
                DemoArea(KITCHEN, "Kitchen", "ground_floor"),
                DemoArea(BEDROOM, "Bedroom", "ground_floor"),
            ),
            schedules = listOf(
                DemoSchedule(
                    id = MORNING_LIGHT,
                    name = "Morning light",
                    enabled = true,
                    whenType = "time",
                    time = "07:00",
                    days = listOf(0, 1, 2, 3, 4),
                    offsetMin = null,
                    action = "turn_on",
                    entities = listOf(LIVING_ROOM_LIGHT),
                    createdAt = nowIso,
                    updatedAt = nowIso,
                ),
            ),
        )

        /** Four switches, two of them on. */
        private fun switches(now: Long): List<DemoEntity> = listOf(
            switch(LIVING_ROOM_LIGHT, "Living room light", "on", LIVING_ROOM, now),
            switch("switch.demo_kitchen_light", "Kitchen light", "off", KITCHEN, now),
            switch("switch.demo_bedroom_lamp", "Bedroom lamp", "off", BEDROOM, now),
            switch("switch.demo_socket", "Socket", "on", LIVING_ROOM, now, deviceClass = "outlet"),
        )

        /** A switch that last changed at [now]. */
        private fun switch(
            id: String,
            name: String,
            state: String,
            area: String,
            now: Long,
            deviceClass: String? = null,
        ) = DemoEntity(id, name, state, now, now, area, deviceClass)

        /** Temperature (measurement) and energy (total increasing). */
        private fun sensors(now: Long): List<DemoEntity> = listOf(
            DemoEntity(
                "sensor.demo_temperature", "Temperature", "21.4", now, now, LIVING_ROOM,
                deviceClass = "temperature", unit = "°C", stateClass = "measurement", displayPrecision = 1,
            ),
            DemoEntity(
                "sensor.demo_energy", "Energy", "1234.5", now, now, KITCHEN,
                deviceClass = "energy", unit = "kWh", stateClass = "total_increasing", displayPrecision = 1,
            ),
        )

        /** A thermostat in mode `heat` that supports a target temperature (flag 1). */
        private fun thermostat(now: Long): List<DemoEntity> = listOf(
            DemoEntity(
                THERMOSTAT, "Thermostat", "heat", now, now, LIVING_ROOM,
                supportedFeatures = 1,
                attributes = buildJsonObject {
                    put("current_temperature", 20.5)
                    put("temperature", 21.0)
                    put("hvac_modes", JsonArray(listOf("off", "heat", "auto").map(::JsonPrimitive)))
                    put("hvac_action", "heating")
                    put("min_temp", 7.0)
                    put("max_temp", 30.0)
                    put("target_temp_step", 0.5)
                },
            ),
        )
    }
}
