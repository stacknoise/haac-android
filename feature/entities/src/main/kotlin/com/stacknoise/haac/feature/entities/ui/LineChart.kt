package com.stacknoise.haac.feature.entities.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.feature.entities.domain.HistoryChart
import com.stacknoise.haac.feature.entities.domain.LineSeries
import com.stacknoise.haac.feature.entities.domain.degrees
import com.stacknoise.haac.feature.entities.domain.rangeAt
import com.stacknoise.haac.feature.entities.domain.valueAt
import com.stacknoise.haac.feature.entities.domain.valueAxis
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Numbers over time (concept 8.3, 8.4): heating and cooling phases behind, the min/max band of statistics, then
 * the lines (2 dp). One value axis only; current and target temperature share it. A readout above shows the
 * values at the selected time, a legend below names every line, band and phase.
 */
@Composable
internal fun LineChart(chart: HistoryChart.Line) {
    var selected by remember(chart) { mutableStateOf<Long?>(null) }
    val values = chart.series.flatMap { series -> series.points.map { it.value } } +
        chart.band.flatMap { listOf(it.low, it.high) }
    val axis = valueAxis(values.min(), values.max())
    Column {
        Text(
            selected?.let { lineReadout(chart, it) }.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
        val ring = HaacColors.Background
        val palette = chartPalette()
        ChartCanvas(chart.start, chart.end, axis, selected, onSelect = { selected = it }) { area ->
            drawPhases(chart, area, palette)
            drawBand(chart, area, palette.green)
            chart.series.forEach { drawSeries(it, area, palette) }
            selected?.let { at -> drawSelectedPoints(chart.series, at, area, ring, palette) }
        }
        LineLegend(chart)
    }
}

/** "14:05 · Current temperature 20.8° · Target temperature 21.0°" for the time [at]. */
@Composable
private fun lineReadout(chart: HistoryChart.Line, at: Long): String {
    val length = chart.end - chart.start
    val lines = chart.series.mapNotNull { series ->
        series.valueAt(at)?.let { "${seriesLabel(series.kind)} ${number(it, chart.unit)}" }
    }
    val range = chart.band.rangeAt(at)?.let { "${number(it.low, chart.unit)} – ${number(it.high, chart.unit)}" }
    val parts = lines + listOfNotNull(range)
    return (listOf(chartTime(at, length, withHour = true)) + parts).joinToString(" · ")
}

/** A value with its unit; temperatures of climate entities with one decimal and °. */
private fun number(value: Double, unit: String?): String = when (unit) {
    "°" -> degrees(value)
    null -> trimmed(value)
    else -> "${trimmed(value)} $unit"
}

/** A value rounded to two decimals without trailing zeros. */
private fun trimmed(value: Double): String =
    BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

/** Heating and cooling phases as tinted columns behind the lines. */
private fun DrawScope.drawPhases(chart: HistoryChart.Line, area: PlotArea, palette: ChartPalette) {
    chart.phases.forEach { phase ->
        drawRect(
            phaseColor(phase.kind, palette).copy(alpha = ChartColors.AREA_ALPHA),
            topLeft = Offset(area.x(phase.start), area.top),
            size = Size(area.x(phase.end) - area.x(phase.start), area.bottom - area.top),
        )
    }
}

/** The min/max band of statistics as a tinted area. */
private fun DrawScope.drawBand(chart: HistoryChart.Line, area: PlotArea, color: Color) {
    if (chart.band.size < 2) return
    val path = Path().apply {
        chart.band.forEachIndexed { index, point ->
            val x = area.x(point.at)
            val y = area.y(point.high)
            if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
        chart.band.asReversed().forEach { lineTo(area.x(it.at), area.y(it.low)) }
        close()
    }
    drawPath(path, color.copy(alpha = ChartColors.AREA_ALPHA))
}

/** One line, segment by segment, so gaps stay open. */
private fun DrawScope.drawSeries(series: LineSeries, area: PlotArea, palette: ChartPalette) {
    val stroke = Stroke(width = LineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    series.segments.forEach { points ->
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = area.x(point.at)
            val y = area.y(point.value)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, seriesColor(series.kind, palette), style = stroke)
    }
}

/** A dot (8 dp, with a ring in the background colour) on every line at the selected time. */
private fun DrawScope.drawSelectedPoints(
    series: List<LineSeries>,
    at: Long,
    area: PlotArea,
    ring: Color,
    palette: ChartPalette,
) {
    series.forEach { line ->
        val value = line.valueAt(at) ?: return@forEach
        val center = Offset(area.x(at), area.y(value))
        drawCircle(ring, radius = 5.dp.toPx(), center = center)
        drawCircle(seriesColor(line.kind, palette), radius = 4.dp.toPx(), center = center)
    }
}
