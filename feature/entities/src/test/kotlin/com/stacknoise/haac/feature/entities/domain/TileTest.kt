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
