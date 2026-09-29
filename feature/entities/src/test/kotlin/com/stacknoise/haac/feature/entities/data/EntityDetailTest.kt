package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.core.database.entity.EntityStatus
import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.feature.entities.domain.Attribute
import com.stacknoise.haac.feature.entities.domain.ControlRequest
import com.stacknoise.haac.feature.entities.domain.DefaultEntityControlFactory
import com.stacknoise.haac.feature.entities.domain.DefaultTileFactory
import com.stacknoise.haac.feature.entities.domain.EntityControl
import com.stacknoise.haac.feature.entities.domain.Reading
import com.stacknoise.haac.feature.entities.domain.ReadingKind
import com.stacknoise.haac.feature.entities.domain.TileContent
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** [TileBuilder.detail]: names, times, readings, attributes and controls of the detail screen (concept 8, 15.4). */
class EntityDetailTest {
    private val builder = TileBuilder(Json, DefaultTileFactory(), DefaultEntityControlFactory())

    private val radiator = ExposedEntity(
        serverId = "s1",
        entityId = "climate.radiator",
        domain = "climate",
        haName = "Radiator",
        configuredName = "Heating",
        supportedFeatures = 1,
        lastState = """{"state":"heat","attributes":{"temperature":21,"current_temperature":20.84,""" +
            """"current_humidity":48,"hvac_action":"heating","hvac_modes":["off","heat"],"friendly_name":null},""" +
            """"lastChanged":1000,"lastUpdated":2000}""",
    )

    @Test
    fun `the detail holds names, times, readings and sorted attributes`() {
        val detail = builder.detail(radiator, alias = "Living radiator")
        assertEquals("Living radiator", detail.tile.name)
        assertEquals("heat", detail.state)
        assertEquals("Radiator", detail.haName)
        assertEquals("Heating", detail.configuredName)
        assertEquals(1000L, detail.lastChanged)
        assertEquals(2000L, detail.lastUpdated)
        assertEquals(
            listOf(
                Reading(ReadingKind.CURRENT_TEMPERATURE, "20.8°"),
                Reading(ReadingKind.CURRENT_HUMIDITY, "48 %"),
                Reading(ReadingKind.HVAC_ACTION, "heating"),
            ),
            detail.readings,
        )
        assertEquals(Attribute("hvac_modes", "off, heat"), detail.attributes.first { it.name == "hvac_modes" })
        assertTrue(detail.attributes.none { it.name == "friendly_name" })
        assertEquals(detail.attributes.map { it.name }.sorted(), detail.attributes.map { it.name })
    }

    @Test
    fun `a pending request shows in the controls`() {
        val detail = builder.detail(radiator, null, ControlRequest.SetHvacMode("off"))
        assertEquals("off", detail.state)
        assertEquals(EntityControl.HvacModes("off", listOf("off", "heat")), detail.controls.first())
    }

    @Test
    fun `withdrawn and unavailable entities have no controls`() {
        val withdrawn = radiator.copy(status = EntityStatus.WITHDRAWN)
        assertEquals(emptyList<EntityControl>(), builder.detail(withdrawn, null).controls)
        val unavailable = radiator.copy(lastState = """{"state":"unavailable"}""")
        assertEquals(emptyList<EntityControl>(), builder.detail(unavailable, null).controls)
        assertNull(builder.detail(unavailable, null).lastChanged)
    }

    @Test
    fun `timestamp sensors become times`() {
        val sensor = ExposedEntity(
            "s1",
            "sensor.next_alarm",
            "sensor",
            "Next alarm",
            deviceClass = "timestamp",
            lastState = """{"state":"2026-09-29T14:05:00+00:00"}""",
        )
        assertEquals(TileContent.Timestamp(1790690700000), builder.detail(sensor, null).tile.content)
        val broken = sensor.copy(lastState = """{"state":"soon"}""")
        assertEquals(TileContent.Sensor("soon", null), builder.detail(broken, null).tile.content)
    }
}
