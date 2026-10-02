package com.stacknoise.haac.app.navigation

import android.net.Uri
import com.stacknoise.haac.feature.schedules.ui.ScheduleDetailViewModel
import com.stacknoise.haac.feature.schedules.ui.ScheduleEditorViewModel

/** Routes of the schedule screens inside the main area (M-12, M-15). */
object ScheduleRoutes {
    /** The detail screen of one schedule. */
    const val DETAIL = "schedules/detail/{${ScheduleDetailViewModel.ID_ARG}}"

    /** The editor; the optional id edits an existing schedule, without it a new one is created. */
    const val EDITOR = "schedules/edit?${ScheduleEditorViewModel.ID_ARG}={${ScheduleEditorViewModel.ID_ARG}}"

    /** The detail screen of schedule [scheduleId]. */
    fun detail(scheduleId: String): String = "schedules/detail/${Uri.encode(scheduleId)}"

    /** The editor for schedule [scheduleId], or for a new one when it is null. */
    fun editor(scheduleId: String? = null): String = when (scheduleId) {
        null -> "schedules/edit"
        else -> "schedules/edit?${ScheduleEditorViewModel.ID_ARG}=${Uri.encode(scheduleId)}"
    }
}
