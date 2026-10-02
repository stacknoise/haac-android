package com.stacknoise.haac.feature.schedules.ui

import com.stacknoise.haac.feature.schedules.domain.ScheduleAction
import com.stacknoise.haac.feature.schedules.domain.ScheduleDraft
import com.stacknoise.haac.feature.schedules.domain.ScheduleItem
import com.stacknoise.haac.feature.schedules.domain.WhenType
import java.time.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScheduleEditorStateTest {
    private fun item(own: Boolean) = ScheduleItem(
        id = "a",
        name = "Hall light",
        enabled = true,
        whenType = WhenType.TIME,
        time = LocalTime.of(6, 45),
        days = listOf(0, 1),
        offsetMin = 0,
        action = ScheduleAction.TURN_ON,
        entityIds = listOf("switch.a"),
        own = own,
        ownerName = "Lena",
        updatedAt = "x",
        pausedReason = null,
        lastRun = null,
        nextRun = null,
    )

    private fun state(original: ScheduleItem?, draft: ScheduleDraft, connected: Boolean = true) = ScheduleEditorUiState(
        form = EditorForm(draft = draft, original = original, loaded = true),
        connected = connected,
    )

    @Test
    fun `a new schedule needs entities and its owner is the user`() {
        val empty = state(null, ScheduleDraft(name = "Light"))
        assertTrue(empty.ownEntities)
        assertNull(empty.owner)
        assertFalse(empty.canSave)
        assertTrue(state(null, ScheduleDraft(name = "Light", entityIds = listOf("switch.a"))).canSave)
    }

    @Test
    fun `an admin editing a foreign schedule sees the owner and needs no entity check`() {
        val foreign = item(own = false)
        val admin = state(foreign, ScheduleDraft.of(foreign))
        assertEquals("Lena", admin.owner)
        assertFalse(admin.ownEntities)
        assertTrue(admin.canSave)
        assertFalse(state(foreign, ScheduleDraft.of(foreign).copy(name = " ")).canSave)
    }

    @Test
    fun `the owner of an own schedule sees no banner`() {
        val mine = item(own = true)
        assertNull(state(mine, ScheduleDraft.of(mine)).owner)
    }

    @Test
    fun `saving is impossible without a connection or while stale`() {
        val mine = item(own = true)
        assertFalse(state(mine, ScheduleDraft.of(mine), connected = false).canSave)
        val stale = state(mine, ScheduleDraft.of(mine)).copy(stale = true)
        assertFalse(stale.canSave)
    }
}
