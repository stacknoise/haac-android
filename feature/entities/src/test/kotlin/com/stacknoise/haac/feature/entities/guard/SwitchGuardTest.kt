package com.stacknoise.haac.feature.entities.guard

import com.stacknoise.haac.core.database.assignment.TileSize
import com.stacknoise.haac.core.database.guard.EntityGuard
import com.stacknoise.haac.feature.entities.domain.ControlRequest
import com.stacknoise.haac.feature.entities.domain.EntityControl
import com.stacknoise.haac.feature.entities.domain.Tile
import com.stacknoise.haac.feature.entities.domain.TileContent
import com.stacknoise.haac.feature.entities.domain.TileIcon
import com.stacknoise.haac.feature.entities.domain.TileMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** [SwitchGuard], [GuardGate] and the guard rows (concept 8.5). */
class SwitchGuardTest {
    private val sent = mutableListOf<Pair<String, ControlRequest>>()
    private val gate = GuardGate { id, request -> sent += id to request }

    private fun lamp(
        on: Boolean = false,
        mode: TileMode = TileMode.NORMAL,
        withdrawn: Boolean = false,
    ) = Tile(
        entityId = "switch.lamp",
        name = "Lamp",
        icon = TileIcon.SWITCH,
        size = TileSize.SMALL,
        content = TileContent.Switch(on),
        withdrawn = withdrawn,
        control = EntityControl.Toggle(on),
        mode = mode,
    )

    @Test
    fun `direction decides which target needs a confirmation`() {
        assertTrue(SwitchGuard(GuardDirection.ON).requiresConfirmation(true))
        assertFalse(SwitchGuard(GuardDirection.ON).requiresConfirmation(false))
        assertFalse(SwitchGuard(GuardDirection.OFF).requiresConfirmation(true))
        assertTrue(SwitchGuard(GuardDirection.OFF).requiresConfirmation(false))
        assertTrue(SwitchGuard(GuardDirection.BOTH).requiresConfirmation(true))
        assertTrue(SwitchGuard(GuardDirection.BOTH).requiresConfirmation(false))
    }

    @Test
    fun `a guarded switch sends nothing until the dialog is confirmed`() {
        gate.submit(lamp(), SwitchGuard(), ControlRequest.SwitchTo(true))
        assertTrue(sent.isEmpty())
        assertEquals(ControlRequest.SwitchTo(true), gate.prompt.value?.request)
        gate.confirm()
        assertEquals(listOf("switch.lamp" to ControlRequest.SwitchTo(true)), sent)
        assertNull(gate.prompt.value)
    }

    @Test
    fun `cancel back and timeout send no command`() {
        gate.submit(lamp(), SwitchGuard(), ControlRequest.SwitchTo(true))
        gate.dismiss()
        gate.confirm()
        assertTrue(sent.isEmpty())
        assertNull(gate.prompt.value)
    }

    @Test
    fun `a direction that is not guarded is sent at once`() {
        gate.submit(lamp(on = true), SwitchGuard(GuardDirection.ON), ControlRequest.SwitchTo(false))
        assertEquals(1, sent.size)
        assertNull(gate.prompt.value)
    }

    @Test
    fun `no dialog without a guard for deactivated withdrawn and non switch requests`() {
        gate.submit(lamp(), null, ControlRequest.SwitchTo(true))
        gate.submit(lamp(mode = TileMode.DISABLED), SwitchGuard(), ControlRequest.SwitchTo(true))
        gate.submit(lamp(withdrawn = true), SwitchGuard(), ControlRequest.SwitchTo(true))
        gate.submit(lamp(), SwitchGuard(), ControlRequest.SetTemperature(21.0))
        assertEquals(4, sent.size)
        assertNull(gate.prompt.value)
    }

    @Test
    fun `the prompt carries the name and the current state`() {
        gate.submit(lamp(on = true), SwitchGuard(), ControlRequest.SwitchTo(false))
        assertEquals(GuardPrompt("switch.lamp", "Lamp", true, ControlRequest.SwitchTo(false)), gate.prompt.value)
    }

    @Test
    fun `withGuards marks only normal switch tiles`() {
        val guards = mapOf("switch.lamp" to SwitchGuard())
        assertEquals(TileMode.GUARDED, lamp().withGuard(guards["switch.lamp"]).mode)
        assertEquals(TileMode.DISABLED, lamp(mode = TileMode.DISABLED).withGuard(guards["switch.lamp"]).mode)
        assertEquals(TileMode.NORMAL, lamp().withGuard(null).mode)
        val sensor = lamp().copy(entityId = "sensor.x", content = TileContent.Sensor("1", null), control = null)
        assertEquals(TileMode.NORMAL, sensor.withGuard(SwitchGuard()).mode)
        assertEquals(listOf(TileMode.GUARDED), listOf(lamp()).withGuards(guards).map { it.mode })
    }

    @Test
    fun `rows and guards convert both ways`() {
        GuardDirection.entries.forEach {
            val row = SwitchGuard(it).toRow("s1", "switch.lamp")
            assertEquals(SwitchGuard(it), row.toSwitchGuard())
        }
        assertEquals(
            EntityGuard("s1", "switch.lamp", onTurnOn = true, onTurnOff = false),
            SwitchGuard(GuardDirection.ON).toRow("s1", "switch.lamp"),
        )
        assertNull(EntityGuard("s1", "switch.lamp", onTurnOn = false, onTurnOff = false).toSwitchGuard())
    }
}
