package com.stacknoise.haac.feature.entities.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.haacFilterChipBorder
import com.stacknoise.haac.core.common.ui.theme.haacFilterChipColors
import com.stacknoise.haac.core.common.ui.theme.haacOutlinedButtonColors
import com.stacknoise.haac.core.common.ui.theme.haacSegmentedColors
import com.stacknoise.haac.core.common.ui.theme.haacSwitchColors
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.ClimateMode
import com.stacknoise.haac.feature.entities.domain.ControlRequest
import com.stacknoise.haac.feature.entities.domain.EntityControl
import com.stacknoise.haac.feature.entities.domain.degrees

/**
 * The controls of the detail screen (concept 8.2, 8.4) in the order the factory gives them; each sends its
 * request through [onControl]. All are disabled unless [enabled].
 */
@Composable
internal fun DetailControls(controls: List<EntityControl>, enabled: Boolean, onControl: (ControlRequest?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        controls.forEach { control ->
            when (control) {
                is EntityControl.Toggle -> ToggleRow(control, enabled, onControl)
                is EntityControl.Power -> PowerRow(control, enabled, onControl)
                is EntityControl.HvacModes -> HvacModeRow(control, enabled, onControl)
                is EntityControl.TargetTemperature -> TemperatureRow(control, enabled, onControl)
                is EntityControl.TargetRange -> RangeRow(control, enabled, onControl)
                is EntityControl.Humidity -> HumidityRow(control, enabled, onControl)
                is EntityControl.Modes -> ModeChips(control, enabled, onControl)
            }
        }
    }
}

/** On/Off with a switch (concept 8.2). */
@Composable
private fun ToggleRow(control: EntityControl.Toggle, enabled: Boolean, onControl: (ControlRequest?) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(if (control.on) R.string.tile_on else R.string.tile_off),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = control.on,
            onCheckedChange = { onControl(control.flipped()) },
            enabled = enabled,
            colors = haacSwitchColors(),
        )
    }
}

/** *Turn on* / *Turn off* of a climate entity (flags 256, 128). */
@Composable
private fun PowerRow(control: EntityControl.Power, enabled: Boolean, onControl: (ControlRequest?) -> Unit) {
    val request = control.flipped()
    Labeled(R.string.detail_power) {
        OutlinedButton(
            onClick = { onControl(request) },
            enabled = enabled && request != null,
            shape = HaacShapes.Button,
            colors = haacOutlinedButtonColors(),
        ) {
            Text(stringResource(if (control.on) R.string.detail_turn_off else R.string.detail_turn_on))
        }
    }
}

/** The HVAC modes as a segmented control; more than four as chips, so the labels stay readable. */
@Composable
private fun HvacModeRow(control: EntityControl.HvacModes, enabled: Boolean, onControl: (ControlRequest?) -> Unit) {
    Labeled(R.string.detail_mode) {
        if (control.modes.size > Segments) {
            Chips(control.modes, control.current, enabled) { onControl(ControlRequest.SetHvacMode(it)) }
            return@Labeled
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            control.modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = mode == control.current,
                    onClick = { onControl(ControlRequest.SetHvacMode(mode)) },
                    shape = SegmentedButtonDefaults.itemShape(index, control.modes.size, HaacShapes.Small),
                    colors = haacSegmentedColors(),
                    enabled = enabled,
                ) { Text(modeLabel(mode), maxLines = 1) }
            }
        }
    }
}

/** − target + in steps of `target_temp_step` (concept 8.4, flag 1). */
@Composable
private fun TemperatureRow(
    control: EntityControl.TargetTemperature,
    enabled: Boolean,
    onControl: (ControlRequest?) -> Unit,
) {
    Labeled(R.string.detail_target) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            val lower = control.stepped(up = false)
            val raise = control.stepped(up = true)
            OutlinedIconButton(onClick = { onControl(lower) }, enabled = enabled && lower != null) {
                Icon(painterResource(R.drawable.ic_entities_minus), stringResource(R.string.tile_colder))
            }
            Text(degrees(control.target), style = MaterialTheme.typography.displaySmall)
            OutlinedIconButton(onClick = { onControl(raise) }, enabled = enabled && raise != null) {
                Icon(painterResource(R.drawable.ic_entities_add), stringResource(R.string.tile_warmer))
            }
        }
    }
}

/** Two-handle slider for `target_temp_low` … `target_temp_high` (concept 8.4, flag 2). */
@Composable
private fun RangeRow(control: EntityControl.TargetRange, enabled: Boolean, onControl: (ControlRequest?) -> Unit) {
    Labeled(R.string.detail_range) {
        Text(
            stringResource(R.string.detail_range_value, degrees(control.low), degrees(control.high)),
            style = MaterialTheme.typography.titleLarge,
        )
        RangeSlider(
            value = control.low.toFloat()..control.high.toFloat(),
            onValueChange = { onControl(control.request(it.start.toDouble(), it.endInclusive.toDouble())) },
            valueRange = control.min.toFloat()..control.max.toFloat(),
            enabled = enabled,
        )
    }
}

/** Slider for the target humidity (concept 8.4, flag 4). */
@Composable
private fun HumidityRow(control: EntityControl.Humidity, enabled: Boolean, onControl: (ControlRequest?) -> Unit) {
    Labeled(R.string.detail_humidity) {
        Text(
            stringResource(R.string.detail_humidity_value, control.target.toInt().toString()),
            style = MaterialTheme.typography.titleLarge,
        )
        Slider(
            value = control.target.toFloat(),
            onValueChange = { onControl(control.request(it.toDouble())) },
            valueRange = control.min.toFloat()..control.max.toFloat(),
            enabled = enabled,
        )
    }
}

/** Fan, preset or swing modes as chips (concept 8.4). */
@Composable
private fun ModeChips(control: EntityControl.Modes, enabled: Boolean, onControl: (ControlRequest?) -> Unit) {
    val title = when (control.kind) {
        ClimateMode.FAN -> R.string.detail_fan_mode
        ClimateMode.PRESET -> R.string.detail_preset
        ClimateMode.SWING -> R.string.detail_swing
        ClimateMode.SWING_HORIZONTAL -> R.string.detail_swing_horizontal
    }
    Labeled(title) { Chips(control.choices, control.current, enabled) { onControl(control.request(it)) } }
}

/** One chip per choice; [current] is selected. */
@Composable
private fun Chips(choices: List<String>, current: String?, enabled: Boolean, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { choice ->
            FilterChip(
                selected = choice == current,
                onClick = { onSelect(choice) },
                label = { Text(modeLabel(choice)) },
                enabled = enabled,
                shape = HaacShapes.Small,
                colors = haacFilterChipColors(),
                border = haacFilterChipBorder(enabled, choice == current),
            )
        }
    }
}

/** A control with its [title] above it. */
@Composable
private fun Labeled(@StringRes title: Int, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        content()
    }
}

/** Most HVAC modes that still fit a segmented control. */
private const val Segments = 4
