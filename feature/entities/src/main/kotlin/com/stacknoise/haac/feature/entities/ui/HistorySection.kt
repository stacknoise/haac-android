package com.stacknoise.haac.feature.entities.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.ErrorMessage
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.haacOutlinedButtonColors
import com.stacknoise.haac.core.common.ui.theme.haacSegmentedColors
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.HistoryChart
import com.stacknoise.haac.feature.entities.domain.HistoryPreset

/**
 * History of the detail screen (concept 8.1 – 8.4): *24 h*, *7 days* or *Custom* (a range of days), then the
 * chart that fits the entity, or loading, offline, empty and error states with the code.
 */
@Composable
internal fun HistorySection(viewModel: EntityHistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var picking by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        PresetRow(state.window.preset, onPreset = viewModel::onPreset, onCustom = { picking = true })
        if (state.window.preset == HistoryPreset.CUSTOM) {
            val length = state.window.length
            Text(
                stringResource(
                    R.string.history_custom_range,
                    chartTime(state.window.start, length, withHour = true),
                    chartTime(state.window.end, length, withHour = true),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = secondaryColor(),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Box(modifier = Modifier.padding(top = 12.dp)) { HistoryBody(state.status, viewModel::onRetry) }
    }
    if (picking) {
        RangePicker(
            onPick = { first, last ->
                picking = false
                viewModel.onCustom(first, last)
            },
            onDismiss = { picking = false },
        )
    }
}

/** *24 h · 7 days · Custom*. */
@Composable
private fun PresetRow(current: HistoryPreset, onPreset: (HistoryPreset) -> Unit, onCustom: () -> Unit) {
    val labels = listOf(R.string.history_day, R.string.history_week, R.string.history_custom)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        HistoryPreset.entries.forEachIndexed { index, preset ->
            SegmentedButton(
                selected = preset == current,
                onClick = { if (preset == HistoryPreset.CUSTOM) onCustom() else onPreset(preset) },
                shape = SegmentedButtonDefaults.itemShape(index, HistoryPreset.entries.size, HaacShapes.Small),
                colors = haacSegmentedColors(),
            ) { Text(stringResource(labels[index]), maxLines = 1) }
        }
    }
}

/** The chart, or what stands in for it. */
@Composable
private fun HistoryBody(status: HistoryStatus, onRetry: () -> Unit) {
    when (status) {
        HistoryStatus.Loading ->
            Box(Modifier.fillMaxWidth().height(ChartHeight), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        HistoryStatus.Offline -> HistoryNote(stringResource(R.string.history_offline))
        is HistoryStatus.Failed -> Column {
            ErrorMessage(status.code)
            OutlinedButton(
                onClick = onRetry,
                shape = HaacShapes.Button,
                colors = haacOutlinedButtonColors(),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.history_retry))
            }
        }
        is HistoryStatus.Loaded -> Chart(status.chart)
    }
}

/** The chart of its kind, or a note when nothing was recorded. */
@Composable
private fun Chart(chart: HistoryChart) {
    if (chart.isEmpty) {
        HistoryNote(stringResource(R.string.history_empty))
        return
    }
    Column {
        when (chart) {
            is HistoryChart.Line -> LineChart(chart)
            is HistoryChart.Bars -> BarChart(chart)
            is HistoryChart.Timeline -> TimelineChart(chart)
        }
        HistoryNote(stringResource(R.string.history_hint))
    }
}

/** A short note in the secondary colour. */
@Composable
private fun HistoryNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = secondaryColor(),
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** Material's date range picker for *Custom*; [onPick] gets the first and last day (UTC midnight). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangePicker(onPick: (Long, Long) -> Unit, onDismiss: () -> Unit) {
    val picker = rememberDateRangePickerState()
    val first = picker.selectedStartDateMillis
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { first?.let { onPick(it, picker.selectedEndDateMillis ?: it) } },
                enabled = first != null,
            ) { Text(stringResource(R.string.history_show)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.rename_cancel)) } },
    ) {
        DateRangePicker(state = picker, modifier = Modifier.weight(1f))
    }
}
