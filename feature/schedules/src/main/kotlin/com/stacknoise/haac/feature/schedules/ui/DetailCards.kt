package com.stacknoise.haac.feature.schedules.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacCard
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.common.ui.theme.haacSwitchColors
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.feature.schedules.R
import com.stacknoise.haac.feature.schedules.domain.LastRun
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The owner of a foreign schedule (M-17). */
@Composable
internal fun OwnerCard(name: String?) {
    HaacCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBox(R.drawable.ic_schedules_person)
            Column(Modifier.padding(start = 14.dp)) {
                Text(stringResource(R.string.schedules_owner), style = SectionLabelStyle)
                Text(name.orEmpty(), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** The 40 dp accent icon box of a card. */
@Composable
internal fun IconBox(icon: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(40.dp).background(HaacColors.AccentTint, HaacShapes.Medium),
    ) {
        Icon(painterResource(icon), null, tint = HaacColors.Accent, modifier = Modifier.size(22.dp))
    }
}

/** What the schedule does to one entity: its name and "turns on". */
@Composable
internal fun ActionCard(name: String, verb: String) {
    HaacCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBox(R.drawable.ic_schedules_clock)
            Column(Modifier.padding(start = 14.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(verb, style = MaterialTheme.typography.bodyMedium, color = secondary())
            }
        }
    }
}

/** The hint that only the owner changes the entities of a foreign schedule (M-17). */
@Composable
internal fun LockedHint() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_schedules_lock), null, tint = secondary(), modifier = Modifier.size(16.dp))
        Text(
            stringResource(R.string.schedules_owner_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = secondary(),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** The *Enabled* switch card. */
@Composable
internal fun EnabledCard(enabled: Boolean, editable: Boolean, onChange: (Boolean) -> Unit) {
    HaacCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.schedules_enabled), style = MaterialTheme.typography.titleMedium)
                if (!enabled) {
                    Text(
                        stringResource(R.string.schedules_enabled_off),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(checked = enabled, onCheckedChange = onChange, enabled = editable, colors = haacSwitchColors())
        }
    }
}

/** *NEXT RUNS*: up to three rows in one card. */
@Composable
internal fun NextRuns(runs: List<Long>) {
    Label(R.string.schedules_next_runs)
    HaacCard(contentPadding = PaddingValues(horizontal = 16.dp)) {
        if (runs.isEmpty()) {
            Text(
                stringResource(R.string.schedules_no_next_runs),
                modifier = Modifier.padding(vertical = 14.dp),
                color = secondary(),
            )
        }
        runs.forEachIndexed { index, millis ->
            if (index > 0) HorizontalDivider(color = HaacColors.Outline)
            Text(
                dayAndClock(millis),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            )
        }
    }
}

/** *LAST RUN*: when, and *Done* or the failure with its code in the danger colour. */
@Composable
internal fun LastRunRow(last: LastRun?) {
    Label(R.string.schedules_last_run)
    HaacCard {
        if (last == null) {
            Text(stringResource(R.string.schedules_never_run), color = secondary())
            return@HaacCard
        }
        val whenText = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .format(Instant.ofEpochMilli(last.at).atZone(ZoneId.systemDefault()))
        val result = if (last.ok) {
            stringResource(R.string.schedules_last_run_done)
        } else {
            stringResource(ErrorCode.SCH_RUN_INCOMPLETE.message) + " (" + ErrorCode.SCH_RUN_INCOMPLETE.code + ")"
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(if (last.ok) R.drawable.ic_schedules_check else R.drawable.ic_schedules_warning),
                contentDescription = null,
                tint = if (last.ok) HaacColors.Accent else HaacColors.Danger,
                modifier = Modifier.size(22.dp),
            )
            Text(
                stringResource(R.string.schedules_last_run_text, whenText, result),
                style = MaterialTheme.typography.bodyLarge,
                color = if (last.ok) Color.Unspecified else HaacColors.Danger,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

/** A section label such as NEXT RUNS. */
@Composable
internal fun Label(text: Int) {
    Text(stringResource(text), style = SectionLabelStyle, modifier = Modifier.padding(top = 8.dp))
}

/** Secondary text colour. */
@Composable
internal fun secondary(): Color = MaterialTheme.colorScheme.onSurfaceVariant
