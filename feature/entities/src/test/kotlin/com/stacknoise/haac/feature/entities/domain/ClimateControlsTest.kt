package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.error.ValidationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** Climate controls of the detail screen and their service calls (concept 8.4). */
class ClimateControlsTest {
    private val controls = DefaultEntityControlFactory()
    private val calls = DefaultServiceCallFactory()

    /** Flags 1 + 2 + 4 + 8 + 16 + 128 + 256, like a full-featured thermostat. */
    private val thermostat = ExposedEntity("s1", "climate.living", "climate", "Living", supportedFeatures = 415)

    private fun json(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    private val heat = EntityState(
        "heat",
        json(
            """{"hvac_modes":["off","heat","auto"],"temperature":21,"target_temp_low":18,"target_temp_high":24,
            |"humidity":45,"fan_modes":["low","high"],"fan_mode":"low","preset_modes":["eco"],
            |"swing_modes":["on","off"]}""".trimMargin(),
        ),
    )

    @Test
    fun `the detail lists every supported control in order`() {
        val list = controls.detail(thermostat, heat)
        assertEquals(
            listOf(
                EntityControl.Power(on = true, canTurnOn = true, canTurnOff = true),
                EntityControl.HvacModes("heat", listOf("off", "heat", "auto")),
                EntityControl.TargetTemperature(21.0, 7.0, 35.0, 0.5),
                EntityControl.TargetRange(18.0, 24.0, 7.0, 35.0, 0.5),
                EntityControl.Humidity(45.0, 30.0, 99.0),
                EntityControl.Modes(ClimateMode.FAN, "low", listOf("low", "high")),
                EntityControl.Modes(ClimateMode.PRESET, null, listOf("eco")),
            ),
            list,
        )
    }

    @Test
    fun `without flags only the HVAC modes remain`() {
        val list = controls.detail(thermostat.copy(supportedFeatures = 0), heat)
        assertEquals(listOf(EntityControl.HvacModes("heat", listOf("off", "heat", "auto"))), list)
        assertEquals(emptyList<EntityControl>(), controls.detail(thermostat.copy(domain = "sensor"), heat))
    }

    @Test
    fun `power follows the flags`() {
        val offOnly = EntityControl.Power(on = false, canTurnOn = false, canTurnOff = true)
        assertNull(offOnly.flipped())
        assertEquals(ControlRequest.PowerTo(on = false), offOnly.copy(on = true).flipped())
    }

    @Test
    fun `range and humidity snap to their steps and limits`() {
        val range = EntityControl.TargetRange(18.0, 24.0, min = 7.0, max = 35.0, step = 0.5)
        assertEquals(ControlRequest.SetTemperatureRange(18.5, 23.0), range.request(18.62, 23.1))
        assertEquals(ControlRequest.SetTemperatureRange(26.0, 26.0), range.request(26.0, 20.0))
        assertEquals(ControlRequest.SetHumidity(99.0), EntityControl.Humidity(45.0, 30.0, 99.0).request(120.0))
    }

    @Test
    fun `climate calls carry their service and data`() {
        fun call(request: ControlRequest) = calls.create(thermostat, request)
        assertEquals(ServiceCall("climate.living", "turn_off"), call(ControlRequest.PowerTo(on = false)))
        assertEquals(json("""{"hvac_mode":"auto"}"""), call(ControlRequest.SetHvacMode("auto")).data)
        assertEquals(
            json("""{"target_temp_low":18.0,"target_temp_high":22.5}"""),
            call(ControlRequest.SetTemperatureRange(18.0, 22.5)).data,
        )
        assertEquals("set_humidity", call(ControlRequest.SetHumidity(50.0)).service)
        val fan = call(ControlRequest.SetMode(ClimateMode.FAN, "high"))
        assertEquals(ServiceCall("climate.living", "set_fan_mode", json("""{"fan_mode":"high"}""")), fan)
    }

    @Test
    fun `calls without their flag are HAAC-ENT-002`() {
        val swing = ControlRequest.SetMode(ClimateMode.SWING, "on")
        assertThrows<ValidationException> { calls.create(thermostat, swing) }
        val noPower = thermostat.copy(supportedFeatures = 1)
        assertThrows<ValidationException> { calls.create(noPower, ControlRequest.PowerTo(on = true)) }
        assertThrows<ValidationException> { calls.create(noPower, ControlRequest.SetHumidity(40.0)) }
    }

    @Test
    fun `requests show and confirm their result`() {
        val off = ControlRequest.PowerTo(on = false)
        assertEquals("off", off.applyTo(heat).state)
        assertTrue(ControlRequest.PowerTo(on = true).isConfirmedBy(heat))
        assertFalse(off.isConfirmedBy(heat))
        val range = ControlRequest.SetTemperatureRange(19.0, 24.0)
        assertTrue(range.isConfirmedBy(range.applyTo(heat)))
        assertFalse(range.isConfirmedBy(heat))
        val preset = ControlRequest.SetMode(ClimateMode.PRESET, "eco")
        assertTrue(preset.isConfirmedBy(preset.applyTo(heat)))
        assertTrue(ControlRequest.SetHumidity(45.0).isConfirmedBy(heat))
        assertTrue(ControlRequest.SetHumidity(45.0).debounced)
        assertFalse(ControlRequest.SetHvacMode("auto").debounced)
    }
}
