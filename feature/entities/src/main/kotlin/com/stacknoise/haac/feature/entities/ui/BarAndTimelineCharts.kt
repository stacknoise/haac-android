package com.stacknoise.haac.feature.entities.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.feature.entities.domain.HistoryChart
import com.stacknoise.haac.feature.entities.domain.barAt
import com.stacknoise.haac.feature.entities.domain.segmentAt
import com.stacknoise.haac.feature.entities.domain.valueAxis
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * Consumption per hour or day of a counter (concept 8.3): bars from zero with a 2 dp gap, rounded at the top; the
 * readout shows the period and value of the selected bar.
 */
@Composable
internal fun BarChart(chart: HistoryChart.Bars) {
    var selected by remember(chart) { mutableStateOf<Long?>(null) }
    val values = chart.bars.map { it.value }
    val axis = valueAxis(values.min(), values.max(), fromZero = true)
    val length = chart.end - chart.start
    val green = chartPalette().green
    Column {
        val bar = selected?.let { chart.bars.barAt(it) }
        Text(
            bar?.let { "${chartTime(it.start, length, withHour = true)} · ${amount(it.value, chart.unit)}" }.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
        ChartCanvas(chart.start, chart.end, axis, selected, onSelect = { selected = it }) { area ->
            val gap = 2.dp.toPx()
            val corner = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            chart.bars.forEach { item ->
                val left = area.x(item.start) + gap / 2
                val width = (area.x(item.end) - area.x(item.start) - gap).coerceAtLeast(1f)
                val top = minOf(area.y(item.value), area.y(0.0))
                val height = abs(area.y(0.0) - area.y(item.value))
                val color = if (item == bar) green else green.copy(alpha = UnselectedAlpha)
                drawRoundRect(color, Offset(left, top), Size(width, height), corner)
            }
        }
    }
}

/**
 * States over time (concept 8.2, 8.3): one band, a colour per state (`on` green, `off` neutral, gaps where HA
 * had no state); the readout shows the state and period at the selected time, the legend names every colour.
 */
@Composable
internal fun TimelineChart(chart: HistoryChart.Timeline) {
    var selected by remember(chart) { mutableStateOf<Long?>(null) }
    val palette = chartPalette()
    val colors = stateColors(chart.segments.map { it.state }, MaterialTheme.colorScheme.surfaceVariant, palette)
    val length = chart.end - chart.start
    Column {
        val segment = selected?.let { chart.segments.segmentAt(it) }
        Text(
            segment?.let {
                val from = chartTime(it.start, length, withHour = true)
                "${modeLabel(it.state)} · $from – ${chartTime(it.end, length, withHour = true)}"
            }.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
        ChartCanvas(chart.start, chart.end, null, selected, { selected = it }, plotHeight = TimelineHeight) { area ->
            val gap = 1.dp.toPx()
            chart.segments.forEach { item ->
                val color = colors[item.state] ?: Color.Transparent
                val width = (area.x(item.end) - area.x(item.start) - gap).coerceAtLeast(1f)
                drawRect(color, Offset(area.x(item.start), area.top), Size(width, area.bottom - area.top))
            }
        }
        ChartLegend(chart.segments.map { it.state }.distinct().mapNotNull { state ->
            colors[state]?.takeIf { it != Color.Transparent }?.let { modeLabel(state) to it }
        })
    }
}

/** A counter value with up to two decimals and its unit. */
private fun amount(value: Double, unit: String?): String {
    val text = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    return listOfNotNull(text, unit).joinToString(" ")
}

/** Opacity of the bars that are not selected, so the selected one stands out. */
private const val UnselectedAlpha = 0.75f

/** Height of the timeline band. */
private val TimelineHeight = 32.dp
