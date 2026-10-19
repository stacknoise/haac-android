package com.stacknoise.haac.feature.entities.guard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.haacSwitchColors
import com.stacknoise.haac.feature.entities.R

/** The dialog's switch reacts only this long after the dialog opened, so a double tap cannot do both steps. */
internal const val ArmDelayMs = 500L

/** The dialog closes without a command after this long without input (concept 8.5). */
internal const val IdleTimeoutMs = 15_000L

/**
 * The switch guard dialog (M-27, concept 8.5): a second switch has to be flipped on purpose before the command for
 * [prompt] is sent through [onConfirm]. *Cancel*, the back gesture, a tap outside and [IdleTimeoutMs] without input
 * call [onDismiss]; the switch is locked for [ArmDelayMs] after the dialog opened.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardConfirmDialog(prompt: GuardPrompt, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var armed by remember(prompt) { mutableStateOf(false) }
    var touches by remember(prompt) { mutableIntStateOf(0) }
    LaunchedEffect(prompt) {
        delay(ArmDelayMs)
        armed = true
    }
    LaunchedEffect(prompt, touches) {
        delay(IdleTimeoutMs)
        onDismiss()
    }
    val turnOn = prompt.request.on
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = HaacShapes.Dialog,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.pointerInput(prompt) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        touches++
                    }
                }
            },
        ) {
            Column(modifier = Modifier.padding(start = 26.dp, end = 26.dp, top = 24.dp, bottom = 16.dp)) {
                GuardLabel()
                Text(
                    stringResource(if (turnOn) R.string.guard_title_on else R.string.guard_title_off, prompt.name),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    stateText(prompt.on),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
                )
                ConfirmSwitchRow(turnOn, armed, onConfirm)
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).padding(top = 8.dp)) {
                    Text(stringResource(R.string.guard_cancel))
                }
            }
        }
    }
}

/** Shield icon and the label *Switch guard* above the title. */
@Composable
private fun GuardLabel() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            painterResource(R.drawable.ic_entities_shield),
            contentDescription = null,
            tint = HaacColors.Accent,
            modifier = Modifier.size(20.dp),
        )
        Text(
            stringResource(R.string.guard_label).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = HaacColors.Accent,
        )
    }
}

/** The card with the action name, the note that it is sent at once and the switch; [armed] unlocks the switch. */
@Composable
private fun ConfirmSwitchRow(turnOn: Boolean, armed: Boolean, onConfirm: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(HaacColors.AccentTint, HaacShapes.Medium)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(if (turnOn) R.string.guard_action_on else R.string.guard_action_off),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.guard_sent_at_once),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = false,
            onCheckedChange = { if (armed && it) onConfirm() },
            enabled = armed,
            colors = haacSwitchColors(),
        )
    }
}

/** The sentence under the title: the current state of the switch and what to do. */
@Composable
private fun stateText(on: Boolean?): String = when (on) {
    true -> stringResource(R.string.guard_text_on)
    false -> stringResource(R.string.guard_text_off)
    null -> stringResource(R.string.guard_text_unknown)
}
