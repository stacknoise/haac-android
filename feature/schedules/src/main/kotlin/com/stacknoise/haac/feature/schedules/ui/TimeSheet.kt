package com.stacknoise.haac.feature.schedules.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.haacButtonColors
import com.stacknoise.haac.core.common.ui.theme.haacSegmentedColors
import com.stacknoise.haac.feature.schedules.R
import com.stacknoise.haac.feature.schedules.domain.ScheduleDraft
import com.stacknoise.haac.feature.schedules.domain.WhenType
import com.stacknoise.haac.feature.schedules.domain.plusHoursWrapped
import com.stacknoise.haac.feature.schedules.domain.plusMinutesWrapped
import com.stacknoise.haac.feature.schedules.domain.stepOffset
import java.time.LocalTime

/** The quick times of the *Time* tab. */
private val QuickTimes = listOf(LocalTime.of(6, 0), LocalTime.of(6, 30), LocalTime.of(7, 0), LocalTime.of(7, 30))

/**
 * The *Run at* sheet (M-13): *Time* with hour and minute steppers and quick times, or *Sunrise* and *Sunset* with an
 * offset in 5-minute steps. Nothing changes until *OK*; [onConfirm] gets the chosen trigger.
 */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
internal fun TimeSheet(
    draft: ScheduleDraft,
    onConfirm: (WhenType, LocalTime, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var type by rememberSaveable { mutableStateOf(draft.whenType) }
    var time by remember { mutableStateOf(draft.time) }
    var offset by rememberSaveable { mutableStateOf(draft.offsetMin) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = HaacColors.Surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 16.dp),
        ) {
            Text(stringResource(R.string.schedules_run_at), style = MaterialTheme.typography.headlineMedium)
            TypeTabs(type) { type = it }
            if (type == WhenType.TIME) {
                TimeSteppers(time) { time = it }
                QuickChips(time) { time = it }
            } else {
                OffsetStepper(type, offset) { offset = it }
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.schedules_cancel)) }
                Button(
                    onClick = { onConfirm(type, time, offset) },
                    colors = haacButtonColors(),
                    shape = HaacShapes.Button,
                    modifier = Modifier.height(52.dp).padding(start = 8.dp),
                ) { Text(stringResource(R.string.schedules_ok)) }
            }
        }
    }
}

/** *Time / Sunrise / Sunset*. */
@Composable
private fun TypeTabs(type: WhenType, onType: (WhenType) -> Unit) {
    val labels = mapOf(
        WhenType.TIME to R.string.schedules_run_at_time,
        WhenType.SUNRISE to R.string.schedules_sunrise,
        WhenType.SUNSET to R.string.schedules_sunset,
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().height(44.dp)) {
        WhenType.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                selected = entry == type,
                onClick = { onType(entry) },
                shape = SegmentedButtonDefaults.itemShape(index, WhenType.entries.size, HaacShapes.Small),
                colors = haacSegmentedColors(),
            ) {
                Text(stringResource(labels.getValue(entry)))
            }
        }
    }
}

/** Two big boxes for hour and minute with chevrons above and below (hour ±1, minute ±5). */
@Composable
private fun TimeSteppers(time: LocalTime, onTime: (LocalTime) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Stepper(
            value = "%02d".format(time.hour),
            up = R.string.schedules_hour_up to { onTime(time.plusHoursWrapped(1)) },
            down = R.string.schedules_hour_down to { onTime(time.plusHoursWrapped(-1)) },
        )
        Text(
            ":",
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 54.sp),
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        Stepper(
            value = "%02d".format(time.minute),
            up = R.string.schedules_minute_up to { onTime(time.plusMinutesWrapped(ScheduleDraft.STEP)) },
            down = R.string.schedules_minute_down to { onTime(time.plusMinutesWrapped(-ScheduleDraft.STEP)) },
        )
    }
}

/** One box with its value and the chevron buttons above and below; each button has its description and action. */
@Composable
private fun Stepper(value: String, up: Pair<Int, () -> Unit>, down: Pair<Int, () -> Unit>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = up.second, modifier = Modifier.size(width = 56.dp, height = 44.dp)) {
            Icon(painterResource(R.drawable.ic_schedules_up), stringResource(up.first))
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .background(HaacColors.Background, RoundedCornerShape(26.dp))
                .border(1.dp, HaacColors.Outline, RoundedCornerShape(26.dp)),
        ) {
            Text(value, style = MaterialTheme.typography.displaySmall.copy(fontSize = 54.sp))
        }
        IconButton(onClick = down.second, modifier = Modifier.size(width = 56.dp, height = 44.dp)) {
            Icon(painterResource(R.drawable.ic_schedules_down), stringResource(down.first))
        }
    }
}

/** The quick times 06:00, 06:30, 07:00 and 07:30; the one that matches is marked. */
@Composable
private fun QuickChips(time: LocalTime, onTime: (LocalTime) -> Unit) {
    val spacing = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    Row(horizontalArrangement = spacing, modifier = Modifier.fillMaxWidth()) {
        QuickTimes.forEach { quick ->
            val selected = quick == time
            val outline = if (selected) HaacColors.AccentBorder else HaacColors.OutlineStrong
            Text(
                quick.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) HaacColors.Accent else HaacColors.OnSurface,
                modifier = Modifier
                    .background(if (selected) HaacColors.AccentTint else HaacColors.Surface, HaacShapes.Small)
                    .border(BorderStroke(if (selected) 1.5.dp else 1.dp, outline), HaacShapes.Small)
                    .clickable { onTime(quick) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

/** The offset of a sun event: − and + buttons around the value, and the label "30 min after sunrise". */
@Composable
private fun OffsetStepper(type: WhenType, offset: Int, onOffset: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OffsetButton(R.drawable.ic_schedules_minus, R.string.schedules_offset_earlier) {
                onOffset(stepOffset(offset, -1))
            }
            Text(
                (if (offset > 0) "+" else "") + offset,
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 54.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.width(150.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            OffsetButton(R.drawable.ic_schedules_add, R.string.schedules_offset_later) {
                onOffset(stepOffset(offset, 1))
            }
        }
        Text(offsetText(type, offset), style = MaterialTheme.typography.bodyLarge, color = HaacColors.OnSurfaceVariant)
    }
}

/** A 64 dp round-cornered − or + button. */
@Composable
private fun OffsetButton(icon: Int, description: Int, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(64.dp)
            .background(HaacColors.Background, RoundedCornerShape(20.dp))
            .border(1.dp, HaacColors.Outline, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        Icon(painterResource(icon), stringResource(description), modifier = Modifier.size(28.dp))
    }
}
