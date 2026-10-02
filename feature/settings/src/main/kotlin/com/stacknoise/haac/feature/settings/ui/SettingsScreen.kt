package com.stacknoise.haac.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.ErrorMessage
import com.stacknoise.haac.core.common.ui.SecureWindow
import com.stacknoise.haac.core.common.ui.findActivity
import com.stacknoise.haac.core.common.ui.theme.HaacCard
import com.stacknoise.haac.core.common.ui.theme.HaacScreenTitle
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.HaacTheme
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.common.ui.theme.haacOutlinedButtonColors
import com.stacknoise.haac.feature.settings.R

/**
 * Settings (concept 15.4); [onSignedOut] receives the id of the instance that was signed out. [instances] is the
 * instance list (concept 4.4), which another feature module provides; [onOpenLicenses] opens the library licenses.
 */
@Composable
fun SettingsScreen(
    onSignedOut: (String) -> Unit,
    onOpenLicenses: () -> Unit,
    instances: @Composable () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    SecureWindow()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val signedOut by rememberUpdatedState(onSignedOut)
    val activity = LocalContext.current.findActivity() as? FragmentActivity
    LaunchedEffect(state.signedOutServerId) {
        state.signedOutServerId?.let(signedOut)
    }
    SettingsContent(
        state = state,
        onSignOut = viewModel::onSignOut,
        instances = instances,
        addresses = { AddressesSection() },
        appearance = { AppearanceSection() },
        diagnostics = { DiagnosticsSection() },
        about = { AboutSection(onOpenLicenses) },
        security = SecurityActions(
            onFingerprintChanged = { enabled -> activity?.let { viewModel.onFingerprintChanged(it, enabled) } },
            onUnlockWindowChanged = { minutes -> activity?.let { viewModel.onUnlockWindowChanged(it, minutes) } },
            onLockTimeoutChanged = viewModel::onLockTimeoutChanged,
        ),
    )
}

/**
 * Stateless layout of the settings; [instances] is the instance list (4.4), [addresses] the address section (4.5),
 * [appearance] the design mode (15.2), [diagnostics] the diagnostics (9.3) and [about] the About section (16.2).
 */
@Composable
fun SettingsContent(
    state: SettingsUiState,
    onSignOut: () -> Unit,
    security: SecurityActions,
    instances: @Composable () -> Unit = {},
    addresses: @Composable () -> Unit = {},
    appearance: @Composable () -> Unit = {},
    diagnostics: @Composable () -> Unit = {},
    about: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        HaacScreenTitle(stringResource(R.string.settings_title))
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.settings_instance).uppercase(), style = SectionLabelStyle)
        Spacer(Modifier.height(8.dp))
        val instance = state.instance ?: return@Column
        HaacCard {
            Text(instance.name, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.settings_signed_in_as, instance.userName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(32.dp))
        instances()
        Spacer(Modifier.height(32.dp))
        addresses()
        Spacer(Modifier.height(32.dp))
        appearance()
        Spacer(Modifier.height(32.dp))
        SecuritySection(state.security, state.busy, security)
        state.error?.let { ErrorMessage(it) }
        Spacer(Modifier.height(32.dp))
        diagnostics()
        Spacer(Modifier.height(32.dp))
        about()
        Spacer(Modifier.height(32.dp))
        OutlinedButton(
            onClick = onSignOut,
            enabled = !state.busy,
            shape = HaacShapes.Button,
            colors = haacOutlinedButtonColors(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            if (state.busy) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.settings_sign_out), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** Preview with a signed-in instance and fingerprint unlock on. */
@Preview
@Composable
private fun SettingsPreview() {
    HaacTheme {
        SettingsContent(
            state = SettingsUiState(
                instance = ActiveInstance("1", "Home", "anna"),
                security = SecurityUiState(
                    fingerprintAvailable = true,
                    fingerprintEnabled = true,
                    unlockWindowSelectable = true,
                ),
            ),
            onSignOut = {},
            security = SecurityActions({}, {}, {}),
            addresses = {
                AddressesContent(
                    AddressesUiState(internal = "http://192.168.1.10:8123/", external = "https://abc.ui.nabu.casa/"),
                    AddressActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}),
                )
            },
        )
    }
}
