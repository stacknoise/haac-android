package com.stacknoise.haac.feature.entities.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.EntityDomains
import com.stacknoise.haac.feature.entities.domain.PhaseKind
import com.stacknoise.haac.feature.entities.domain.SeriesKind
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Height of a chart's plot area. */
internal val ChartHeight = 180.dp

/** Width of a line (2 dp, concept of thin marks). */
internal val LineWidth = 2.dp

/** Colour of a line kind: the measured value green, the target blue. */
internal fun seriesColor(kind: SeriesKind): Color =
    if (kind == SeriesKind.TARGET) ChartColors.Blue else ChartColors.Green

/** Colour of a climate phase: heating yellow, cooling magenta. */
internal fun phaseColor(kind: PhaseKind): Color =
    if (kind == PhaseKind.HEATING) ChartColors.Yellow else ChartColors.Magenta

/**
 * Colours of timeline [states] in order of first appearance: `on` green, `off` [neutral], `unavailable` and
 * `unknown` transparent (a gap), others in categorical order; beyond four they share [neutral].
 */
internal fun stateColors(states: List<String>, neutral: Color): Map<String, Color> {
    val fixed = setOf(EntityDomains.ON, EntityDomains.OFF) + EntityDomains.UNAVAILABLE
    val others = states.distinct().filter { it !in fixed }
    return buildMap {
        put(EntityDomains.ON, ChartColors.Green)
        put(EntityDomains.OFF, neutral)
        EntityDomains.UNAVAILABLE.forEach { put(it, Color.Transparent) }
        others.forEachIndexed { index, state -> put(state, ChartColors.Categorical.getOrElse(index) { neutral }) }
    }
}

/** Legend label of a line kind. */
@Composable
internal fun seriesLabel(kind: SeriesKind): String = stringResource(
    when (kind) {
        SeriesKind.VALUE -> R.string.history_value
        SeriesKind.MEAN -> R.string.history_mean
        SeriesKind.CURRENT -> R.string.detail_current_temperature
        SeriesKind.TARGET -> R.string.detail_target
    },
)

/** Legend label of a phase. */
@Composable
internal fun phaseLabel(kind: PhaseKind): String =
    stringResource(if (kind == PhaseKind.HEATING) R.string.history_heating else R.string.history_cooling)

/** A time for axis and readout: hours and minutes up to two days, else day and month (and hour for readouts). */
internal fun chartTime(at: Long, windowLength: Long, withHour: Boolean = false): String {
    val format = when {
        windowLength <= TwoDaysMs -> DateFormat.getTimeInstance(DateFormat.SHORT)
        withHour -> localPattern("dMMMjmm")
        else -> localPattern("dMMM")
    }
    return format.format(Date(at))
}

/** A date format for the fields of [skeleton] in the device's language and order. */
private fun localPattern(skeleton: String): DateFormat {
    val locale = Locale.getDefault()
    return SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}

/** Two days in milliseconds: shorter windows label hours. */
private const val TwoDaysMs = 172_800_000L
