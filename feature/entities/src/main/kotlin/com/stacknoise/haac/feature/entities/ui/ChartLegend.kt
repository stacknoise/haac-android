package com.stacknoise.haac.feature.entities.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.HistoryChart

/** Legend of a line chart: every line, the min/max band and the phases; none for a single plain line. */
@Composable
internal fun LineLegend(chart: HistoryChart.Line) {
    val lines = chart.series.filter { it.points.isNotEmpty() }.map { seriesLabel(it.kind) to seriesColor(it.kind) }
    val range = stringResource(R.string.history_range)
    val band = if (chart.band.isEmpty()) emptyList() else listOf(range to area(ChartColors.Green))
    val phases = chart.phases.map { it.kind }.distinct().map { phaseLabel(it) to area(phaseColor(it)) }
    val entries = lines + band + phases
    if (entries.size > 1) ChartLegend(entries)
}

/** [color] as a band or phase is drawn. */
private fun area(color: Color): Color = color.copy(alpha = ChartColors.AREA_ALPHA)

/** Colour swatches with their labels; the text keeps the text colour, the swatch carries the identity. */
@Composable
internal fun ChartLegend(entries: List<Pair<String, Color>>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        entries.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Box(Modifier.width(14.dp).height(8.dp).background(color, RoundedCornerShape(2.dp)))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}
