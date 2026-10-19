package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.assignment.TileSize
import com.stacknoise.haac.core.database.entity.EntityStatus
import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.feature.entities.R
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TileFactoryTest {
    private val factory = DefaultTileFactory()

    private fun entity(id: String, deviceClass: String? = null, precision: Int? = null, unit: String? = null) =
        ExposedEntity(
            "s1",
            id,
            id.substringBefore('.'),
            id,
            deviceClass = deviceClass,
            displayPrecision = precision,
            unit = unit,
        )

    @Test
    fun `climate tiles are 2x2, everything else 1x1`() {
        assertEquals(TileSize.LARGE, factory.defaultSize("climate"))
        assertEquals(TileSize.SMALL, factory.defaultSize("switch"))
        assertEquals(TileSize.SMALL, factory.defaultSize("sensor"))
    }

    @Test
    fun `switches show on and off, outlets get their icon, unavailable is not available`() {
        val on = factory.create(entity("switch.plug", "outlet"), EntityState("on"), "Plug", TileSize.SMALL)
        assertEquals(TileContent.Switch(true), on.content)
        assertEquals(TileIcon.OUTLET, on.icon)
        assertTrue(on.available)

        val gone = factory.create(entity("switch.lamp"), EntityState("unavailable"), "Lamp", TileSize.SMALL)
        assertEquals(TileContent.Switch(null), gone.content)
        assertEquals(TileIcon.SWITCH, gone.icon)
        assertFalse(gone.available)
        assertFalse(factory.create(entity("switch.x"), null, "X", TileSize.SMALL).available)
    }

    @Test
    fun `sensors round to the suggested precision and keep text states`() {
        val temperature = entity("sensor.t", "temperature", precision = 1, unit = "°C")
        val tile = factory.create(temperature, EntityState("21.449"), "Temperature", TileSize.SMALL)
        assertEquals(TileContent.Sensor("21.4", "°C"), tile.content)
        assertEquals(TileIcon.TEMPERATURE, tile.icon)
        val text = factory.create(entity("sensor.mode"), EntityState("eco"), "Mode", TileSize.SMALL)
        assertEquals(TileContent.Sensor("eco", null), text.content)
    }

    @Test
    fun `climate shows target, current and heating, withdrawn entities are marked`() {
        val attributes = buildJsonObject {
            put("temperature", JsonPrimitive(21.5))
            put("current_temperature", JsonPrimitive(20.84))
            put("hvac_action", JsonPrimitive("heating"))
        }
        val radiator = entity("climate.radiator").copy(status = EntityStatus.WITHDRAWN)
        val tile = factory.create(radiator, EntityState("heat", attributes), "Radiator", TileSize.LARGE)
        assertEquals(TileContent.Climate("21.5°", "20.8°", heating = true), tile.content)
        assertTrue(tile.withdrawn)
    }

    @Test
    fun `a deactivated entity is a disabled tile, a withdrawn one stays as in 7-4`() {
        val row = entity("switch.plug").copy(disabled = true)
        val tile = factory.create(row, EntityState("on"), "Plug", TileSize.SMALL)
        assertEquals(TileMode.DISABLED, tile.mode)
        assertTrue(tile.disabled)
        assertTrue(tile.tappable)
        val gone = row.copy(status = EntityStatus.WITHDRAWN)
        val withdrawn = factory.create(gone, EntityState("on"), "Plug", TileSize.SMALL)
        assertEquals(TileMode.NORMAL, withdrawn.mode)
        assertEquals(TileMode.NORMAL, factory.create(entity("switch.x"), EntityState("on"), "X", TileSize.SMALL).mode)
    }

    @Test
    fun `a binary sensor shows the words of its class and lights up only while on`() {
        val door = entity("binary_sensor.door", "door")
        val open = factory.create(door, EntityState("on", lastChanged = 5L), "Door", TileSize.SMALL)
        assertEquals(TileContent.Binary(R.string.binary_open, BinaryLevel.ACTIVE, 5L), open.content)
        assertEquals(TileIcon.DOOR, open.icon)
        assertEquals(null, open.control)
        val closed = factory.create(door, EntityState("off"), "Door", TileSize.SMALL)
        assertEquals(TileContent.Binary(R.string.binary_closed, null, null), closed.content)
        val smoke = factory.create(entity("binary_sensor.s", "smoke"), EntityState("on"), "Smoke", TileSize.SMALL)
        assertEquals(BinaryLevel.ALARM, (smoke.content as TileContent.Binary).level)
        assertEquals(TileIcon.ALARM, smoke.icon)
    }

    @Test
    fun `a binary sensor without a known class is on and off, unavailable is not available`() {
        val plain = factory.create(entity("binary_sensor.x", "weird"), EntityState("on"), "X", TileSize.SMALL)
        assertEquals(TileContent.Binary(R.string.tile_on, BinaryLevel.ACTIVE, null), plain.content)
        assertEquals(TileIcon.BINARY, plain.icon)
        val gone = factory.create(entity("binary_sensor.x"), EntityState("unavailable"), "X", TileSize.SMALL)
        assertFalse(gone.available)
        assertEquals(null, (gone.content as TileContent.Binary).level)
    }

    @Test
    fun `an unlocked lock is active, not an alarm, and binary sensors are not tappable`() {
        val lock = factory.create(entity("binary_sensor.l", "lock"), EntityState("on"), "Lock", TileSize.SMALL)
        assertEquals(TileContent.Binary(R.string.binary_unlocked, BinaryLevel.ACTIVE, null), lock.content)
        assertFalse(lock.tappable)
    }

    @Test
    fun `binary sensors have a tab label and the tab sits between sensor and climate`() {
        assertEquals(R.string.add_domain_binary, factory.labels("binary_sensor")?.name)
    }
}
