package com.stacknoise.haac.feature.entities.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.feature.entities.domain.ValueAxis
import java.math.BigDecimal

/**
 * The frame of every history chart: value gridlines with labels on the left (when there is an [axis]), time
 * labels below, and the selection line at [selected]. A tap or a horizontal drag selects a time ([onSelect]),
 * the readout above the chart shows its values. [marks] draws the data.
 */
@Composable
internal fun ChartCanvas(
    start: Long,
    end: Long,
    axis: ValueAxis?,
    selected: Long?,
    onSelect: (Long) -> Unit,
    plotHeight: Dp = ChartHeight,
    marks: DrawScope.(PlotArea) -> Unit,
) {
    val measurer = rememberTextMeasurer()
    val label = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val cursor = MaterialTheme.colorScheme.onBackground
    val gutter = if (axis == null) 0.dp else AxisGutter
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(plotHeight + TimeGutter + TopGap)
            .pointerInput(start, end, axis) {
                detectTapGestures { onSelect(areaOf(size.width.toFloat(), 0f, gutter.toPx(), start, end).timeAt(it.x)) }
            }
            .pointerInput(start, end, axis) {
                val area = areaOf(size.width.toFloat(), 0f, gutter.toPx(), start, end)
                detectHorizontalDragGestures(onDragStart = { onSelect(area.timeAt(it.x)) }) { change, _ ->
                    onSelect(area.timeAt(change.position.x))
                }
            },
    ) {
        val area = PlotArea(gutter.toPx(), TopGap.toPx(), size.width, size.height - TimeGutter.toPx(), start, end, axis)
        axis?.let { drawValueGrid(area, measurer, label, grid) }
        drawTimeLabels(area, measurer, label)
        marks(area)
        selected?.let { at ->
            val x = area.x(at)
            drawLine(cursor, Offset(x, area.top), Offset(x, area.bottom), 1.dp.toPx())
        }
    }
}

/** A plot area of [width] for the time lookup of gestures (values are not needed there). */
private fun areaOf(width: Float, height: Float, left: Float, start: Long, end: Long) =
    PlotArea(left, 0f, width, height, start, end, null)

/** Horizontal gridlines at the axis ticks with their values on the left. */
private fun DrawScope.drawValueGrid(area: PlotArea, measurer: TextMeasurer, style: TextStyle, color: Color) {
    val axis = area.axis ?: return
    axis.ticks.forEach { tick ->
        val y = area.y(tick)
        drawLine(color, Offset(area.left, y), Offset(area.right, y), 1f)
        val text = measurer.measure(tickText(tick), style)
        drawText(text, topLeft = Offset(area.left - text.size.width - 6.dp.toPx(), y - text.size.height / 2f))
    }
}

/** Times at the start, middle and end of the period below the plot. */
private fun DrawScope.drawTimeLabels(area: PlotArea, measurer: TextMeasurer, style: TextStyle) {
    val length = area.end - area.start
    LabelPositions.forEach { fraction ->
        val at = area.start + (length * fraction).toLong()
        val text = measurer.measure(chartTime(at, length), style)
        val x = (area.x(at) - text.size.width * fraction).coerceIn(0f, size.width - text.size.width)
        drawText(text, topLeft = Offset(x, area.bottom + 6.dp.toPx()))
    }
}

/** A tick value without trailing zeros (20, 20.5). */
private fun tickText(value: Double): String = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

/** Space for the value labels left of the plot and the time labels below it. */
private val AxisGutter = 40.dp
private val TimeGutter = 22.dp
private val TopGap = 8.dp

/** Where the time labels sit: start, middle and end of the period. */
private val LabelPositions = listOf(0f, 0.5f, 1f)
