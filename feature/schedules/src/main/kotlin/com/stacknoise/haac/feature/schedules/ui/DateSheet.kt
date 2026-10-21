package com.stacknoise.haac.feature.schedules.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.HaacSheet
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.common.ui.theme.haacButtonColors
import com.stacknoise.haac.feature.schedules.R
import com.stacknoise.haac.feature.schedules.domain.WeekDays
import com.stacknoise.haac.feature.schedules.domain.isPickable
import com.stacknoise.haac.feature.schedules.domain.monthCells
import java.time.LocalDate
import java.time.YearMonth

/** The months of a year; a month is held as `year * 12 + month - 1` while the calendar page turns. */
private const val MonthsPerYear = 12

/** What a date sheet is for: its [title], an optional [hint] under the calendar and the days that can be picked. */
internal class DateSheetSpec(
    @StringRes val title: Int,
    @StringRes val hint: Int?,
    val min: LocalDate,
    val max: LocalDate?,
)

/**
 * The date sheet (M-24): the chosen date big, a calendar with the month arrows and, if there is a [spec] hint, a note
 * under it. [onClear] adds a *No end date* button. Nothing changes until *Apply*; [onConfirm] gets the date.
 */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
internal fun DateSheet(
    spec: DateSheetSpec,
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    onClear: (() -> Unit)? = null,
) {
    var chosen by rememberSaveable { mutableLongStateOf(initial.toEpochDay()) }
    var page by rememberSaveable { mutableIntStateOf(monthIndex(YearMonth.from(initial))) }
    HaacSheet(onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 16.dp),
        ) {
            Text(stringResource(spec.title), style = SectionLabelStyle)
            Text(
                fullDate(LocalDate.ofEpochDay(chosen)),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )
            MonthHeader(yearMonth(page), onPage = { page += it })
            Calendar(yearMonth(page), LocalDate.ofEpochDay(chosen), spec) { chosen = it.toEpochDay() }
            spec.hint?.let { InfoNote(it) }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (onClear != null) {
                    TextButton(onClick = onClear) { Text(stringResource(R.string.schedules_date_no_end)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.schedules_cancel)) }
                Button(
                    onClick = { onConfirm(LocalDate.ofEpochDay(chosen)) },
                    colors = haacButtonColors(),
                    shape = HaacShapes.Button,
                    modifier = Modifier.height(52.dp).padding(start = 8.dp),
                ) { Text(stringResource(R.string.schedules_apply)) }
            }
        }
    }
}

/** The month as its running number, so it can be saved while the page turns. */
private fun monthIndex(month: YearMonth): Int = month.year * MonthsPerYear + month.monthValue - 1

/** The month of running number [index]. */
private fun yearMonth(index: Int): YearMonth =
    YearMonth.of(Math.floorDiv(index, MonthsPerYear), Math.floorMod(index, MonthsPerYear) + 1)

/** The month and year with the arrows that turn the page by one month ([onPage] gets -1 or 1). */
@Composable
private fun MonthHeader(month: YearMonth, onPage: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            monthTitle(month),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onPage(-1) }) {
            val description = stringResource(R.string.schedules_date_previous_month)
            Icon(painterResource(R.drawable.ic_schedules_chevron_left), description)
        }
        IconButton(onClick = { onPage(1) }) {
            val description = stringResource(R.string.schedules_date_next_month)
            Icon(painterResource(R.drawable.ic_schedules_chevron_right), description)
        }
    }
}

/** The weekday header and the weeks of [month], Monday first; [chosen] is filled and today outlined. */
@Composable
private fun Calendar(month: YearMonth, chosen: LocalDate, spec: DateSheetSpec, onPick: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    Row(modifier = Modifier.fillMaxWidth()) {
        for (day in 0 until WeekDays.COUNT) {
            Text(
                dayName(day),
                style = MaterialTheme.typography.labelLarge,
                color = HaacColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
    monthCells(month).chunked(WeekDays.COUNT).forEach { week ->
        Row(modifier = Modifier.fillMaxWidth()) {
            for (index in 0 until WeekDays.COUNT) {
                val date = week.getOrNull(index)
                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).height(DayCellSize)) {
                    if (date != null) {
                        val pickable = isPickable(date, spec.min, spec.max)
                        DayMark(date, date == chosen, date == today, pickable, onPick)
                    }
                }
            }
        }
    }
}

/** The height of a calendar row and the diameter of the day marks. */
private val DayCellSize = 44.dp

/** One day: [isSelected] filled with the accent, [today] outlined, a day that is not [pickable] grey. */
@Composable
private fun DayMark(
    date: LocalDate,
    isSelected: Boolean,
    today: Boolean,
    pickable: Boolean,
    onPick: (LocalDate) -> Unit,
) {
    val marked = when {
        isSelected -> Modifier.background(HaacColors.Accent, CircleShape)
        today -> Modifier.border(1.5.dp, HaacColors.Accent, CircleShape)
        else -> Modifier
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(DayCellSize)
            .then(marked)
            .then(if (pickable) Modifier.clickable(role = Role.Button) { onPick(date) } else Modifier)
            .semantics { contentDescription = fullDate(date); selected = isSelected },
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isSelected -> HaacColors.OnAccent
                pickable -> HaacColors.OnSurface
                else -> HaacColors.OnSurfaceVariant.copy(alpha = DisabledAlpha)
            },
        )
    }
}

/** How strongly days that cannot be picked fade. */
private const val DisabledAlpha = 0.45f

/** A note with the info icon, for example "Runs once and is removed automatically afterwards." */
@Composable
internal fun InfoNote(@StringRes text: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().background(HaacColors.AccentTint, HaacShapes.Small).padding(14.dp),
    ) {
        GlyphIcon(R.drawable.ic_schedules_info, HaacColors.OnSurface, 20.dp)
        Text(
            stringResource(text),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
