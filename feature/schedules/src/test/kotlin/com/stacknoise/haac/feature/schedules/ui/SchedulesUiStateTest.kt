package com.stacknoise.haac.feature.schedules.ui

import com.stacknoise.haac.feature.schedules.data.ScheduleView
import com.stacknoise.haac.feature.schedules.domain.ScheduleAction
import com.stacknoise.haac.feature.schedules.domain.ScheduleItem
import com.stacknoise.haac.feature.schedules.domain.WhenType
import java.time.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SchedulesUiStateTest {
    private fun view(id: String, own: Boolean = true, nextRun: Long? = null) = ScheduleView(
        ScheduleItem(
            id = id,
            name = id,
            enabled = true,
            whenType = WhenType.TIME,
            time = LocalTime.of(6, 45),
            days = listOf(0),
            offsetMin = 0,
            action = ScheduleAction.TURN_ON,
            entityIds = listOf("switch.a"),
            own = own,
            ownerName = "Lena",
            updatedAt = "x",
            pausedReason = null,
            lastRun = null,
            nextRun = nextRun,
        ),
        listOf("Lamp"),
    )

    @Test
    fun `editing is possible only with a connection and never while stale`() {
        assertTrue(SchedulesUiState(connected = true).editable)
        assertFalse(SchedulesUiState(connected = false).editable)
        assertFalse(SchedulesUiState(connected = false, stale = true).editable)
        assertFalse(ScheduleDetailUiState(connected = false, stale = true).editable)
        assertTrue(ScheduleDetailUiState(connected = true).editable)
    }

    @Test
    fun `regular users see no filter, admins with foreign schedules do`() {
        assertFalse(SchedulesUiState(listOf(view("a"), view("b"))).admin)
        assertTrue(SchedulesUiState(listOf(view("a"), view("b", own = false))).admin)
    }

    @Test
    fun `mine hides foreign schedules and the banner follows the filter`() {
        val state = SchedulesUiState(
            listOf(view("mine", nextRun = 9), view("hers", own = false, nextRun = 5)),
            filter = ScheduleFilter.MINE,
        )
        assertEquals(listOf("mine"), state.visible.map { it.item.id })
        assertEquals("mine", state.nextUp?.item?.id)
        assertEquals("hers", state.copy(filter = ScheduleFilter.ALL).nextUp?.item?.id)
    }
}
