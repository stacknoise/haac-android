package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.assignment.TileSize
import com.stacknoise.haac.feature.entities.ui.GridGeometry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** [LayoutDraft] of the edit layout and the grid geometry it is dragged on (concept 7.2, M-06, M-07). */
class LayoutDraftTest {
    private fun tile(id: String, size: TileSize = TileSize.SMALL, name: String = id) =
        Tile(id, name, TileIcon.SWITCH, size, TileContent.Switch(true))

    private val draft = LayoutDraft(listOf(tile("a"), tile("b"), tile("c", TileSize.LARGE)))

    @Test
    fun `moving, resizing and removing mark the draft changed`() {
        assertFalse(draft.changed)
        val moved = draft.move(2, 0)
        assertEquals(listOf("c", "a", "b"), moved.tiles.map { it.entityId })
        assertTrue(moved.changed)
        val resized = moved.resize("a", TileSize.WIDE)
        val expected = listOf("c" to TileSize.LARGE, "a" to TileSize.WIDE, "b" to TileSize.SMALL)
        assertEquals(expected, resized.arrangement())
        val removed = resized.remove("b")
        assertEquals(setOf("b"), removed.removed)
        assertEquals(listOf("c", "a"), removed.tiles.map { it.entityId })
    }

    @Test
    fun `no-op edits keep the draft unchanged`() {
        assertSame(draft, draft.move(1, 1))
        assertSame(draft, draft.move(0, 5))
        assertSame(draft, draft.resize("a", TileSize.SMALL))
    }

    @Test
    fun `refresh takes new names but keeps order, sizes and removals`() {
        val edited = draft.move(2, 0).resize("a", TileSize.WIDE).remove("b")
        val current = listOf(tile("a", name = "Lamp"), tile("b"), tile("c", TileSize.LARGE), tile("d"))
        val refreshed = edited.refresh(current)
        assertEquals(listOf("c", "a", "d"), refreshed.tiles.map { it.entityId })
        assertEquals("Lamp", refreshed.tiles[1].name)
        assertEquals(TileSize.WIDE, refreshed.tiles[1].size)
        assertEquals(listOf("a"), edited.refresh(listOf(tile("a"))).tiles.map { it.entityId })
    }

    @Test
    fun `the grid geometry packs tiles like the room grid`() {
        val rects = GridGeometry(width = 212, gap = 12, rowHeight = 100).rects(
            listOf(TileSize.LARGE, TileSize.SMALL, TileSize.SMALL, TileSize.WIDE),
        )
        assertEquals(listOf(0, 0, 100, 212), rects[0].let { listOf(it.left, it.top, it.right, it.bottom) })
        assertEquals(listOf(112, 0), rects[1].let { listOf(it.left, it.top) })
        assertEquals(listOf(112, 112), rects[2].let { listOf(it.left, it.top) })
        assertEquals(listOf(0, 224, 212, 324), rects[3].let { listOf(it.left, it.top, it.right, it.bottom) })
        assertEquals(324, GridGeometry(212, 12, 100).height(rects))
    }
}
