package com.stacknoise.haac.app.navigation

import android.net.Uri
import com.stacknoise.haac.feature.schedules.ui.ScheduleDetailViewModel

/** Routes of the schedule screens inside the main area (M-15). */
object ScheduleRoutes {
    /** The detail screen of one schedule. */
    const val DETAIL = "schedules/detail/{${ScheduleDetailViewModel.ID_ARG}}"

    /** The detail screen of schedule [scheduleId]. */
    fun detail(scheduleId: String): String = "schedules/detail/${Uri.encode(scheduleId)}"
}
