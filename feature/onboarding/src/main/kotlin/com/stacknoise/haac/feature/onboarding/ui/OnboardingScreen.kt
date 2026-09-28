package com.stacknoise.haac.feature.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.HaacTheme
import com.stacknoise.haac.core.common.ui.theme.MonoFontFamily
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.feature.onboarding.R
import com.stacknoise.haac.feature.onboarding.domain.DiscoveredServer

/** M-01: pick a discovered HA server or enter its address (concept 4.2, 15.3). */
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingContent(
        state = state,
        actions = OnboardingActions(
            onServerSelected = viewModel::onServerSelected,
            onOtherAddress = viewModel::onOtherAddress,
            onManualUrlChanged = viewModel::onManualUrlChanged,
            onContinue = viewModel::onContinue,
            onCleartextConfirmed = viewModel::onCleartextConfirmed,
            onCleartextDismissed = viewModel::onCleartextDismissed,
        ),
    )
}

/** Stateless layout of M-01. */
@Composable
fun OnboardingContent(state: OnboardingUiState, actions: OnboardingActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(48.dp))
            Text(stringResource(R.string.onboarding_hello), style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.onboarding_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            SectionHeader(scanning = state.scanning)
            ServerList(state, actions)
            ManualAddress(state, actions)
            StatusMessage(state)
        }
        ContinueButton(enabled = !state.checking, checking = state.checking, onClick = actions.onContinue)
    }
    state.cleartextWarningFor?.let { url -> CleartextDialog(url, actions) }
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
            server = server,
            selected = !state.manualEntry && server.url == state.selectedUrl,
            onClick = { actions.onServerSelected(server) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    }
}

/** One server with accent bar, name, `IP:port` and a check mark when selected. */
@Composable
private fun ServerRow(server: DiscoveredServer, selected: Boolean, onClick: () -> Unit) {
    val selectedLabel = stringResource(R.string.onboarding_selected)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
            .semantics { if (selected) contentDescription = "${server.name}, $selectedLabel" },
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
            Text(server.name, style = MaterialTheme.typography.titleMedium)
            Text(
                server.address,
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
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
        shape = HaacShapes.Medium,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Error with its code (concept 17.4), or the positive result of the server check. */
@Composable
private fun StatusMessage(state: OnboardingUiState) {
    state.error?.let { ErrorMessage(it) }
    state.validatedUrl?.let { url ->
        Text(
            stringResource(R.string.onboarding_server_ok, url),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/** User text of an error code plus the code in the mono font. */
@Composable
private fun ErrorMessage(code: ErrorCode) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            stringResource(code.message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            code.code,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Primary action: outlined in the accent colour with an arrow (concept 15.2). */
@Composable
private fun ContinueButton(enabled: Boolean, checking: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = HaacShapes.Medium,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.onboarding_continue), style = MaterialTheme.typography.titleMedium)
            if (checking) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(painterResource(R.drawable.ic_onboarding_arrow), contentDescription = null)
            }
        }
    }
}

/** Warning before an unencrypted connection to a private address (concept 4.3). */
@Composable
private fun CleartextDialog(url: String, actions: OnboardingActions) {
    AlertDialog(
        onDismissRequest = actions.onCleartextDismissed,
        title = { Text(stringResource(R.string.onboarding_cleartext_title)) },
        text = { Text(stringResource(R.string.onboarding_cleartext_text, url)) },
        confirmButton = {
            TextButton(onClick = actions.onCleartextConfirmed) {
                Text(stringResource(R.string.onboarding_cleartext_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onCleartextDismissed) {
                Text(stringResource(R.string.onboarding_cleartext_cancel))
            }
        },
    )
}

/** Preview with two discovered servers, the first selected. */
@Preview
@Composable
private fun OnboardingPreview() {
    HaacTheme {
        OnboardingContent(
            state = OnboardingUiState(
                servers = listOf(
                    DiscoveredServer("1", "homeassistant.local", "192.168.1.10:8123", "http://192.168.1.10:8123", null),
                    DiscoveredServer("2", "ha-test.local", "192.168.1.40:8123", "http://192.168.1.40:8123", "2026.9.0"),
                ),
                selectedUrl = "http://192.168.1.10:8123",
            ),
            actions = OnboardingActions(),
        )
    }
}
