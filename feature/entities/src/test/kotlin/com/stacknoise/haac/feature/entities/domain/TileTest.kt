package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.assignment.TileSize
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TileTest {
    private fun tile(content: TileContent, control: EntityControl? = null, withdrawn: Boolean = false) =
        Tile("x.y", "Name", TileIcon.SWITCH, TileSize.SMALL, content, withdrawn = withdrawn, control = control)

    @Test
    fun `a tap does something on a switch and on a withdrawn tile`() {
        assertTrue(tile(TileContent.Switch(true), EntityControl.Toggle(true)).tappable)
        assertTrue(tile(TileContent.Sensor("21", "°C"), withdrawn = true).tappable)
    }

    @Test
    fun `a tap does nothing on other tiles, so they offer no tap action to a screen reader`() {
        assertFalse(tile(TileContent.Sensor("21", "°C")).tappable)
        assertFalse(tile(TileContent.Other("idle")).tappable)
        // An unavailable switch has no control, so its tile does not announce a tap either.
        assertFalse(tile(TileContent.Switch(null)).tappable)
    }
}

/** The shared tile state of concept 7.5 and 8.5. */
class TileModeTest {
    private val base = Tile("x.y", "Name", TileIcon.SWITCH, TileSize.SMALL, TileContent.Switch(true))

    @Test
    fun `a tile is normal until a guard or a deactivation says otherwise`() {
        assertFalse(base.disabled)
        assertFalse(base.guarded)
        assertTrue(base.copy(mode = TileMode.GUARDED).guarded)
        assertTrue(base.copy(mode = TileMode.DISABLED).disabled)
    }

    @Test
    fun `a deactivated tile is not guarded, so it opens no dialog`() {
        assertFalse(base.copy(mode = TileMode.DISABLED).guarded)
    }
}
