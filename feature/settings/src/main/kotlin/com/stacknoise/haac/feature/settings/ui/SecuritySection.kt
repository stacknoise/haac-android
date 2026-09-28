package com.stacknoise.haac.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.database.settings.SecuritySettings
import com.stacknoise.haac.feature.settings.R

/** Settings → Security (concept 5.4, 5.5, 15.4): fingerprint unlock, unlock window and lock timeout. */
@Composable
fun SecuritySection(state: SecurityUiState, busy: Boolean, actions: SecurityActions) {
    Text(stringResource(R.string.settings_security).uppercase(), style = SectionLabelStyle)
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_fingerprint), style = MaterialTheme.typography.titleMedium)
            val hint = when {
                state.fingerprintAvailable -> R.string.settings_fingerprint_hint
                else -> R.string.settings_fingerprint_unavailable
            }
            Hint(stringResource(hint))
        }
        Switch(
            checked = state.fingerprintEnabled,
            onCheckedChange = actions.onFingerprintChanged,
            enabled = !busy && state.fingerprintAvailable,
        )
    }
    if (state.fingerprintAvailable && state.unlockWindowSelectable) {
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.settings_unlock_window), style = MaterialTheme.typography.titleMedium)
        Hint(stringResource(R.string.settings_unlock_window_hint))
        Choices(
            choices = SecuritySettings.UNLOCK_WINDOW_CHOICES,
            selected = state.unlockWindowMinutes,
            zeroLabel = stringResource(R.string.settings_off),
            enabled = !busy,
            onSelected = actions.onUnlockWindowChanged,
        )
    }
    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.settings_lock_timeout), style = MaterialTheme.typography.titleMedium)
    Hint(stringResource(R.string.settings_lock_timeout_hint))
    Choices(
        choices = SecuritySettings.LOCK_TIMEOUT_CHOICES,
        selected = state.lockTimeoutMinutes,
        zeroLabel = stringResource(R.string.settings_immediately),
        enabled = !busy,
        onSelected = actions.onLockTimeoutChanged,
    )
}

/** Secondary text below a setting. */
@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Single-select chips for minute values; 0 is shown as [zeroLabel]. */
@Composable
private fun Choices(
    choices: List<Int>,
    selected: Int,
    zeroLabel: String,
    enabled: Boolean,
    onSelected: (Int) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { minutes ->
            FilterChip(
                selected = minutes == selected,
                onClick = { if (minutes != selected) onSelected(minutes) },
                enabled = enabled,
                shape = HaacShapes.Small,
                label = { Text(if (minutes == 0) zeroLabel else stringResource(R.string.settings_minutes, minutes)) },
            )
        }
    }
}
