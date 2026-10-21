package com.stacknoise.haac.app.navigation

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.stacknoise.haac.R
import com.stacknoise.haac.core.common.ui.LocalWindowWidthClass
import com.stacknoise.haac.core.common.ui.WindowWidthClass
import com.stacknoise.haac.core.common.ui.argsViewModel
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacEmptyState
import com.stacknoise.haac.feature.layout.ui.PlaceEditorScreen
import com.stacknoise.haac.feature.layout.ui.PlaceEditorViewModel
import com.stacknoise.haac.feature.layout.ui.PlacesScreen
import com.stacknoise.haac.feature.schedules.ui.ScheduleDetailScreen
import com.stacknoise.haac.feature.schedules.ui.ScheduleDetailViewModel
import com.stacknoise.haac.feature.schedules.ui.ScheduleEditorScreen
import com.stacknoise.haac.feature.schedules.ui.ScheduleEditorViewModel
import com.stacknoise.haac.feature.schedules.ui.SchedulesScreen

/** Width of the list beside its detail in landscape (concept 15.7). */
private val ListWidth = 420.dp

/** The list on the left and its detail or, without one, an empty state with [emptyText] on the right. */
@Composable
private fun ListDetailPane(
    @StringRes emptyText: Int,
    detailOpen: Boolean,
    list: @Composable () -> Unit,
    detail: @Composable () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.width(ListWidth).fillMaxHeight().border(1.dp, HaacColors.Outline)) { list() }
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (detailOpen) {
                detail()
            } else {
                HaacEmptyState(painterResource(R.drawable.ic_nav_places), stringResource(emptyText))
            }
        }
    }
}

/**
 * The Places tab (M-02): in Expanded width the list with the place form beside it (concept 15.7, T-05), else the
 * list that opens the form as a screen of its own.
 */
@Composable
internal fun PlacesRoute(navController: NavController) {
    var kind by rememberSaveable { mutableStateOf<String?>(null) }
    var placeId by rememberSaveable { mutableStateOf<String?>(null) }
    var opened by rememberSaveable { mutableIntStateOf(0) }
    val onImport = { navController.navigate(PlaceRoutes.IMPORT) }
    if (LocalWindowWidthClass.current != WindowWidthClass.Expanded) {
        PlacesScreen(onOpen = { k, id -> navController.navigate(PlaceRoutes.editor(k, id)) }, onImport = onImport)
        return
    }
    ListDetailPane(
        emptyText = R.string.list_detail_places_empty,
        detailOpen = kind != null,
        list = {
            PlacesScreen(
                onOpen = { k, id ->
                    kind = k.name
                    placeId = id
                    opened++
                },
                onImport = onImport,
            )
        },
        detail = {
            val args = Bundle().apply {
                putString(PlaceEditorViewModel.KIND_ARG, kind)
                putString(PlaceEditorViewModel.ID_ARG, placeId)
            }
            val model = argsViewModel(PlaceEditorViewModel::class.java, "place/$opened", args)
            PlaceEditorScreen(onClose = { kind = null }, viewModel = model)
        },
    )
}

/** What the right pane of the Schedules tab shows. */
private enum class ScheduleMode {
    NONE,
    DETAIL,
    EDITOR,
}

/**
 * The Schedules tab (M-10): in Expanded width the list with detail or editor beside it (concept 15.7, T-06), else
 * the list that opens them as screens of their own.
 */
@Composable
internal fun SchedulesRoute(navController: NavController) {
    var mode by rememberSaveable { mutableStateOf(ScheduleMode.NONE) }
    var scheduleId by rememberSaveable { mutableStateOf<String?>(null) }
    var opened by rememberSaveable { mutableIntStateOf(0) }
    if (LocalWindowWidthClass.current != WindowWidthClass.Expanded) {
        SchedulesScreen(
            onOpen = { navController.navigate(ScheduleRoutes.detail(it)) },
            onCreate = { navController.navigate(ScheduleRoutes.editor()) },
        )
        return
    }
    val show = { next: ScheduleMode, id: String? ->
        mode = next
        scheduleId = id
        opened++
    }
    ListDetailPane(
        emptyText = R.string.list_detail_schedules_empty,
        detailOpen = mode != ScheduleMode.NONE,
        list = {
            SchedulesScreen(onOpen = { show(ScheduleMode.DETAIL, it) }, onCreate = { show(ScheduleMode.EDITOR, null) })
        },
        detail = {
            val args = Bundle().apply { putString(ScheduleDetailViewModel.ID_ARG, scheduleId) }
            if (mode == ScheduleMode.DETAIL) {
                val model = argsViewModel(ScheduleDetailViewModel::class.java, "schedule/$opened", args)
                ScheduleDetailScreen(
                    onClose = { show(ScheduleMode.NONE, null) },
                    onEdit = { show(ScheduleMode.EDITOR, it) },
                    viewModel = model,
                )
            } else {
                val model = argsViewModel(ScheduleEditorViewModel::class.java, "schedule-editor/$opened", args)
                val back = if (scheduleId == null) ScheduleMode.NONE else ScheduleMode.DETAIL
                ScheduleEditorScreen(onClose = { show(back, scheduleId) }, viewModel = model)
            }
        },
    )
}
