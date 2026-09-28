package com.stacknoise.haac.feature.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.MonoFontFamily
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.feature.onboarding.R

/** The stored instance when signing in again, otherwise discovered servers and *Other address…* (M-01). */
@Composable
internal fun ServerSection(state: OnboardingUiState, actions: OnboardingActions) {
    val known = state.knownServer
    if (known != null) {
        Text(stringResource(R.string.onboarding_sign_in_again).uppercase(), style = SectionLabelStyle)
        ServerRow(name = known.displayName, address = known.url, selected = true, onClick = {})
        return
    }
    SectionHeader(scanning = state.scanning)
    ServerList(state, actions)
    ManualAddress(state, actions)
}

/** "ON THIS NETWORK" label with the live scan indicator. */
@Composable
private fun SectionHeader(scanning: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.onboarding_on_this_network).uppercase(),
            style = SectionLabelStyle,
            modifier = Modifier.weight(1f),
        )
        if (scanning) {
            Icon(
                painterResource(R.drawable.ic_onboarding_scan),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                stringResource(R.string.onboarding_scanning).uppercase(),
                style = SectionLabelStyle.copy(color = MaterialTheme.colorScheme.primary),
            )
        }
    }
}

/** Discovered servers, or a hint if none was found. */
@Composable
private fun ServerList(state: OnboardingUiState, actions: OnboardingActions) {
    if (state.servers.isEmpty()) {
        Text(
            stringResource(R.string.onboarding_nothing_found),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        return
    }
    state.servers.forEach { server ->
        ServerRow(
            name = server.name,
            address = server.address,
            selected = !state.manualEntry && server.url == state.selectedUrl,
            onClick = { actions.onServerSelected(server) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    }
}

/** One server with accent bar, name, address in mono font and a check mark when selected. */
@Composable
private fun ServerRow(name: String, address: String, selected: Boolean, onClick: () -> Unit) {
    val selectedLabel = stringResource(R.string.onboarding_selected)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
            .semantics { if (selected) contentDescription = "$name, $selectedLabel" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    HaacShapes.Full,
                ),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(
                address,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(
                painterResource(R.drawable.ic_onboarding_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** *Other address…* link, or the URL field once it was tapped. */
@Composable
private fun ManualAddress(state: OnboardingUiState, actions: OnboardingActions) {
    if (!state.manualEntry) {
        TextButton(onClick = actions.onOtherAddress) {
            Text(stringResource(R.string.onboarding_other_address), color = MaterialTheme.colorScheme.primary)
        }
        return
    }
    Spacer(Modifier.height(16.dp))
    TextField(
        value = state.manualUrl,
        onValueChange = actions.onManualUrlChanged,
        label = { Text(stringResource(R.string.onboarding_address_label)) },
        placeholder = { Text(stringResource(R.string.onboarding_address_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        shape = HaacShapes.Medium,
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}
