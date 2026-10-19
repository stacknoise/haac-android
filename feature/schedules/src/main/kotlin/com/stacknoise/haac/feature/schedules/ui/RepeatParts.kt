package com.stacknoise.haac.feature.schedules.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.haacFilterChipBorder
import com.stacknoise.haac.core.common.ui.theme.haacFilterChipColors
import com.stacknoise.haac.core.common.ui.theme.haacSegmentedColors
import com.stacknoise.haac.core.common.ui.theme.haacSwitchColors
import com.stacknoise.haac.feature.schedules.R
import com.stacknoise.haac.feature.schedules.domain.RepeatFreq
import com.stacknoise.haac.feature.schedules.domain.RepeatRule

/** *Once* and *Repeat* as a segmented control above the date rule (M-23, M-24); [onMode] gets true for *Once*. */
@Composable
internal fun ModeSegments(once: Boolean, onMode: (Boolean) -> Unit) {
    val modes = listOf(true to R.string.schedules_mode_once, false to R.string.schedules_mode_repeat)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().height(44.dp)) {
        modes.forEachIndexed { index, (isOnce, label) ->
            SegmentedButton(
                selected = isOnce == once,
                onClick = { onMode(isOnce) },
                shape = SegmentedButtonDefaults.itemShape(index, modes.size, HaacShapes.Small),
                colors = haacSegmentedColors(),
            ) { Text(stringResource(label)) }
        }
    }
}

/** The frequencies *Daily*, *Weekly*, *Monthly* and *Yearly* as chips that wrap; the chosen one is dark. */
@Composable
internal fun FreqChips(freq: RepeatFreq, onFreq: (RepeatFreq) -> Unit) {
    val labels = listOf(
        RepeatFreq.DAILY to R.string.schedules_freq_daily,
        RepeatFreq.WEEKLY to R.string.schedules_freq_weekly,
        RepeatFreq.MONTHLY to R.string.schedules_freq_monthly,
        RepeatFreq.YEARLY to R.string.schedules_freq_yearly,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEach { (entry, label) ->
            FilterChip(
                selected = entry == freq,
                onClick = { onFreq(entry) },
                label = { Text(stringResource(label)) },
                colors = haacFilterChipColors(),
                border = haacFilterChipBorder(enabled = true, selected = entry == freq),
                modifier = Modifier.height(40.dp),
            )
        }
    }
}

/** The interval in words ("Every 2 weeks") with the round − and + buttons; yearly rules have no interval. */
@Composable
internal fun IntervalRow(rule: RepeatRule, onStep: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            intervalLabel(rule.freq, rule.interval),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f),
        )
        StepButton(R.drawable.ic_schedules_minus, R.string.schedules_interval_less, rule.interval > 1) { onStep(-1) }
        StepButton(
            R.drawable.ic_schedules_add,
            R.string.schedules_interval_more,
            rule.interval < rule.freq.maxInterval,
        ) { onStep(1) }
    }
}

/** A round 44 dp outlined button with an icon; [enabled] is false at the end of the interval range. */
@Composable
private fun StepButton(icon: Int, description: Int, enabled: Boolean, onClick: () -> Unit) {
    val outline = if (enabled) HaacColors.OutlineStrong else HaacColors.Outline
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(start = 12.dp)
            .size(44.dp)
            .background(HaacColors.Surface, CircleShape)
            .border(1.dp, outline, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Icon(
            painterResource(icon),
            stringResource(description),
            tint = if (enabled) HaacColors.OnSurface else HaacColors.OnSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** A row with the calendar icon, the [text] and the [action] word ("Change", "Set"); a tap opens the sheet. */
@Composable
internal fun DateRow(text: String, action: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        GlyphIcon(R.drawable.ic_schedules_calendar, HaacColors.OnSurface, 22.dp)
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
        )
        Text(action, style = MaterialTheme.typography.titleSmall, color = HaacColors.OnSurfaceVariant)
    }
}

/** *Show in notifications* with the bell and a line that says what the switch does (concept 19.11). */
@Composable
internal fun NotifyRow(notify: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        GlyphIcon(R.drawable.ic_schedules_bell, HaacColors.OnSurface, 22.dp)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                stringResource(R.string.schedules_notify_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                stringResource(if (notify) R.string.schedules_notify_on else R.string.schedules_notify_off),
                style = MaterialTheme.typography.bodyMedium,
                color = HaacColors.OnSurfaceVariant,
            )
        }
        Switch(checked = notify, onCheckedChange = onChange, colors = haacSwitchColors())
    }
}
