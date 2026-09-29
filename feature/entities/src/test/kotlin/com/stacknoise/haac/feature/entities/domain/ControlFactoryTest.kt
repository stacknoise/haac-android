package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.error.ErrorCode
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

/** [EntityControlFactory], [ServiceCallFactory], [EntityControl] and [ControlRequest] (concept 8). */
class ControlFactoryTest {
    private val controls = DefaultEntityControlFactory()
    private val calls = DefaultServiceCallFactory()
    private val switch = ExposedEntity("s1", "switch.lamp", "switch", "Lamp")
    private val climate = ExposedEntity("s1", "climate.radiator", "climate", "Radiator", supportedFeatures = 1)

    private fun attributes(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    @Test
    fun `switches toggle only with a known state`() {
        assertEquals(EntityControl.Toggle(on = true), controls.create(switch, EntityState("on")))
        assertEquals(EntityControl.Toggle(on = false), controls.create(switch, EntityState("off")))
        assertNull(controls.create(switch, EntityState("unknown")))
        assertNull(controls.create(switch.copy(domain = "sensor"), EntityState("21")))
    }

    @Test
    fun `climate uses HA's limits and needs flag 1 and a target`() {
        val limits = """{"temperature":21,"min_temp":5,"max_temp":30,"target_temp_step":1}"""
        val state = EntityState("heat", attributes(limits))
        assertEquals(EntityControl.TargetTemperature(21.0, 5.0, 30.0, 1.0), controls.create(climate, state))
        val bare = EntityState("heat", attributes("""{"temperature":21.5}"""))
        assertEquals(EntityControl.TargetTemperature(21.5, 7.0, 35.0, 0.5), controls.create(climate, bare))
        assertNull(controls.create(climate.copy(supportedFeatures = 0), bare))
        assertNull(controls.create(climate, EntityState("off")))
    }

    @Test
    fun `steps stay within the limits and are rounded`() {
        val control = EntityControl.TargetTemperature(target = 21.2, min = 7.0, max = 21.3, step = 0.1)
        assertEquals(ControlRequest.SetTemperature(21.1), control.stepped(up = false))
        assertEquals(ControlRequest.SetTemperature(21.3), control.stepped(up = true))
        assertNull(control.copy(target = 21.3).stepped(up = true))
        assertEquals(0.5f, EntityControl.TargetTemperature(20.0, 10.0, 30.0, 1.0).fraction)
    }

    @Test
    fun `switch requests become turn_on and turn_off`() {
        val call = calls.create(switch, ControlRequest.SwitchTo(on = true))
        assertEquals(ServiceCall("switch.lamp", "turn_on"), call)
        assertEquals(
            attributes("""{"entity_id":"switch.lamp","service":"turn_off","service_data":{}}"""),
            calls.create(switch, ControlRequest.SwitchTo(on = false)).fields(),
        )
    }

    @Test
    fun `set_temperature carries only the temperature`() {
        val call = calls.create(climate, ControlRequest.SetTemperature(22.5))
        assertEquals("set_temperature", call.service)
        assertEquals(attributes("""{"temperature":22.5}"""), call.data)
    }

    @Test
    fun `unsupported requests are HAAC-ENT-002`() {
        val wrongDomain = assertThrows<ValidationException> { calls.create(climate, ControlRequest.SwitchTo(true)) }
        assertEquals(ErrorCode.ENT_ACTION_NOT_SUPPORTED, wrongDomain.code)
        val noFlag = climate.copy(supportedFeatures = 2)
        assertThrows<ValidationException> { calls.create(noFlag, ControlRequest.SetTemperature(20.0)) }
    }

    @Test
    fun `requests know their expected and confirmed state`() {
        val heat = EntityState("heat", attributes("""{"temperature":21,"hvac_action":"idle"}"""))
        val expected = ControlRequest.SetTemperature(22.0).applyTo(heat)
        assertEquals(attributes("""{"temperature":22.0,"hvac_action":"idle"}"""), expected.attributes)
        val confirmed = heat.copy(attributes = attributes("""{"temperature":22}"""))
        assertTrue(ControlRequest.SetTemperature(22.0).isConfirmedBy(confirmed))
        assertFalse(ControlRequest.SetTemperature(22.0).isConfirmedBy(heat))
        assertEquals("off", ControlRequest.SwitchTo(on = false).applyTo(EntityState("on")).state)
        assertTrue(ControlRequest.SwitchTo(on = true).isConfirmedBy(EntityState("on")))
    }
}
