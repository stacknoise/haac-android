package com.stacknoise.haac.feature.entities.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.feature.entities.data.EntityController
import com.stacknoise.haac.feature.entities.data.EntityHistory
import com.stacknoise.haac.feature.entities.domain.HistoryChart
import com.stacknoise.haac.feature.entities.domain.HistoryPreset
import com.stacknoise.haac.feature.entities.domain.HistoryWindow
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/** Where loading the history stands. */
sealed interface HistoryStatus {
    /** The request is running. */
    data object Loading : HistoryStatus

    /** No connection; the history loads once there is one. */
    data object Offline : HistoryStatus

    /** The [chart] of the window. */
    data class Loaded(val chart: HistoryChart) : HistoryStatus

    /** Loading failed with [code]. */
    data class Failed(val code: ErrorCode) : HistoryStatus
}

/** What the history section shows: the chosen [window] and its [status]. */
data class HistoryUiState(val window: HistoryWindow, val status: HistoryStatus = HistoryStatus.Loading)

/**
 * The history of the detail screen's entity (concept 8.1): 24 h, 7 days or a custom range of days, loaded over
 * the live connection whenever the range, the instance or the connection changes, or on *Try again*.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EntityHistoryViewModel @Inject constructor(
    savedState: SavedStateHandle,
    active: ActiveInstanceStore,
    history: EntityHistory,
    controller: EntityController,
) : ViewModel() {
    private val entityId: String = savedState[EntityDetailViewModel.ENTITY_ARG] ?: ""
    private val window = MutableStateFlow(presetWindow(HistoryPreset.DAY, System.currentTimeMillis()))
    private val attempts = MutableStateFlow(0)

    /** The current state of the section. */
    val state: StateFlow<HistoryUiState> =
        combine(active.activeServerId, window, controller.connected, attempts) { id, shown, connected, _ ->
            Request(id, shown, connected)
        }.flatMapLatest { request ->
            flow {
                val id = request.serverId
                when {
                    id == null || !request.connected -> emit(HistoryUiState(request.window, HistoryStatus.Offline))
                    else -> {
                        emit(HistoryUiState(request.window, HistoryStatus.Loading))
                        emit(HistoryUiState(request.window, load(history, id, request.window)))
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HistoryUiState(window.value))

    /** *24 h* or *7 days*, counted back from now. */
    fun onPreset(preset: HistoryPreset) {
        window.value = presetWindow(preset, System.currentTimeMillis())
    }

    /** A custom range from the date picker: whole days [firstDay] to [lastDay] (UTC midnight, as it gives them). */
    fun onCustom(firstDay: Long, lastDay: Long) {
        window.value = customWindow(firstDay, lastDay, System.currentTimeMillis(), ZoneId.systemDefault())
    }

    /** *Try again* after an error. */
    fun onRetry() {
        attempts.value++
    }

    /** The chart, or the error it failed with. */
    private suspend fun load(history: EntityHistory, serverId: String, shown: HistoryWindow): HistoryStatus = try {
        HistoryStatus.Loaded(history.load(serverId, entityId, shown))
    } catch (e: HaacException) {
        HistoryStatus.Failed(e.code)
    }

    /** One load: instance, window and whether the connection is open. */
    private data class Request(val serverId: String?, val window: HistoryWindow, val connected: Boolean)

    /** Sharing timeout. */
    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** The last 24 hours or 7 days before [now]. */
internal fun presetWindow(preset: HistoryPreset, now: Long): HistoryWindow {
    val days = if (preset == HistoryPreset.WEEK) WeekDays else 1
    return HistoryWindow(now - days * DayMs, now, preset)
}

/**
 * The days [firstDay] to [lastDay] in [zone], from the start of the first to the end of the last, but not past
 * [now]; the picker passes UTC midnight of each day.
 */
internal fun customWindow(firstDay: Long, lastDay: Long, now: Long, zone: ZoneId): HistoryWindow {
    /** The date of a picker value. */
    fun day(utcMidnight: Long): LocalDate = Instant.ofEpochMilli(utcMidnight).atZone(ZoneOffset.UTC).toLocalDate()
    val start = day(firstDay).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = day(lastDay).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return HistoryWindow(start, end.coerceAtMost(now).coerceAtLeast(start + 1), HistoryPreset.CUSTOM)
}

/** Length of a day and of the week preset. */
private const val DayMs = 86_400_000L
private const val WeekDays = 7
