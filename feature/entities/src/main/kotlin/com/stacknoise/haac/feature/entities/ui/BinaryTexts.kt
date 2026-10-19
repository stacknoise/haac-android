package com.stacknoise.haac.feature.entities.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.BinaryClass
import com.stacknoise.haac.feature.entities.domain.EntityDomains
import com.stacknoise.haac.feature.entities.domain.HistoryChart
import com.stacknoise.haac.feature.entities.domain.binaryStats
import com.stacknoise.haac.feature.entities.domain.durationText
import java.util.Locale

/** The word for [state] in a timeline: the class words of a binary sensor for `on` and `off`, else the state name. */
@Composable
internal fun binaryLabel(binary: BinaryClass?, state: String): String = when {
    binary != null && state == EntityDomains.ON -> stringResource(binary.on)
    binary != null && state == EntityDomains.OFF -> stringResource(binary.off)
    else -> modeLabel(state)
}

/** "7 changes, 3 h 20 min open": the changes and the time in the `on` state of the shown range (concept 8.6). */
@Composable
internal fun BinarySummaryLine(chart: HistoryChart.Timeline, binary: BinaryClass) {
    val stats = binaryStats(chart.segments)
    val word = stringResource(binary.on).lowercase(Locale.getDefault())
    Text(
        pluralStringResource(
            R.plurals.binary_summary,
            stats.changes,
            stats.changes,
            durationText(stats.onMillis),
            word,
        ),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
}
