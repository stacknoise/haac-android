package com.stacknoise.haac.feature.entities.guard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.HaacSheet
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.haacButtonColors
import com.stacknoise.haac.core.common.ui.theme.haacSegmentedColors
import com.stacknoise.haac.core.common.ui.theme.haacSwitchColors
import com.stacknoise.haac.feature.entities.R

/**
 * The sheet *Switch guard* of the switch [name] (M-28, concept 8.5): the switch *Require confirmation* and, while it
 * is on, the direction. Every change goes to [onChange] at once (null turns the guard off); *Done* calls [onDismiss].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardSettingsSheet(
    name: String,
    guard: SwitchGuard?,
    onChange: (SwitchGuard?) -> Unit,
    onDismiss: () -> Unit,
) {
    var direction by remember { mutableStateOf(guard?.direction ?: GuardDirection.BOTH) }
    HaacSheet(onDismiss) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            SheetHeader(name)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 20.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.guard_require), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.guard_require_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = guard != null,
                    onCheckedChange = { onChange(if (it) SwitchGuard(direction) else null) },
                    colors = haacSwitchColors(),
                )
            }
            if (guard != null) {
                DirectionRow(guard.direction) {
                    direction = it
                    onChange(SwitchGuard(it))
                }
            }
            Text(
                stringResource(R.string.guard_info),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .background(HaacColors.AccentTint, HaacShapes.Medium)
                    .padding(16.dp),
            )
            Button(
                onClick = onDismiss,
                shape = HaacShapes.Button,
                colors = haacButtonColors(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(top = 16.dp),
            ) { Text(stringResource(R.string.guard_done)) }
        }
    }
}

/** Shield box, the title *Switch guard* and the name of the switch. */
@Composable
private fun SheetHeader(name: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(48.dp).background(HaacColors.AccentTint, HaacShapes.Medium),
        ) {
            Icon(
                painterResource(R.drawable.ic_entities_shield),
                contentDescription = null,
                tint = HaacColors.Accent,
                modifier = Modifier.size(24.dp),
            )
        }
        Column {
            Text(stringResource(R.string.guard_title), style = MaterialTheme.typography.titleLarge)
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The label *When* and the segmented control *Turn on / Turn off / Both*. */
@Composable
private fun DirectionRow(selected: GuardDirection, onSelect: (GuardDirection) -> Unit) {
    Text(
        stringResource(R.string.guard_when).uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        GuardDirection.entries.forEachIndexed { index, direction ->
            SegmentedButton(
                selected = direction == selected,
                onClick = { onSelect(direction) },
                shape = SegmentedButtonDefaults.itemShape(index, GuardDirection.entries.size, HaacShapes.Small),
                colors = haacSegmentedColors(),
            ) {
                Text(stringResource(directionLabel(direction)), maxLines = 1)
            }
        }
    }
}

/** The text of a segment of the direction control. */
private fun directionLabel(direction: GuardDirection): Int = when (direction) {
    GuardDirection.ON -> R.string.guard_when_on
    GuardDirection.OFF -> R.string.guard_when_off
    GuardDirection.BOTH -> R.string.guard_when_both
}
