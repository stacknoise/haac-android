package com.stacknoise.haac.feature.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.CertificateDetails
import com.stacknoise.haac.core.common.ui.CertificateDialog
import com.stacknoise.haac.core.common.ui.CertificateDialogKind
import com.stacknoise.haac.core.common.ui.ErrorMessage
import com.stacknoise.haac.core.common.ui.SecureWindow
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.HaacTheme
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.network.endpoint.AddressSlot
import com.stacknoise.haac.feature.onboarding.R
import com.stacknoise.haac.feature.onboarding.domain.SignInResult
import com.stacknoise.haac.core.network.discovery.DiscoveredServer
/** M-01: pick a HA server, sign in, optional MFA code (concept 4.2, 5.1, 15.3); [onSignedIn] after success. */
@Composable
fun OnboardingScreen(onSignedIn: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    SecureWindow()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val password = rememberTextFieldState()
    val signedIn by rememberUpdatedState(onSignedIn)
    LaunchedEffect(state.signedInServerId) {
        if (state.signedInServerId != null) {
            password.clearText()
            signedIn()
        }
    }
    LaunchedEffect(state.error) {
        if (state.error == ErrorCode.AUTH_INVALID_CREDENTIALS) password.clearText()
    }
    val signIn = { viewModel.onSignIn(password.text.copyChars()) }
    OnboardingContent(
        state = state,
        password = password,
        actions = OnboardingActions(
            onServerSelected = viewModel::onServerSelected,
            onOtherAddress = viewModel::onOtherAddress,
            onRescan = viewModel::onRescan,
            onManualUrlChanged = viewModel::onManualUrlChanged,
            onUsernameChanged = viewModel::onUsernameChanged,
            onCodeChanged = viewModel::onCodeChanged,
            onSignIn = signIn,
            onSubmitCode = viewModel::onSubmitCode,
            onRetryBridgeCheck = viewModel::onRetryBridgeCheck,
            onStartOver = viewModel::onStartOver,
            onCleartextConfirmed = {
                viewModel.onCleartextConfirmed()
                signIn()
            },
            onCleartextDismissed = viewModel::onCleartextDismissed,
            onCertificateTrusted = viewModel::onCertificateTrusted,
            onCertificateDismissed = viewModel::onCertificateDismissed,
            onAddAddress = viewModel::onAddAddress,
            onAddressOfferDismissed = viewModel::onAddressOfferDismissed,
        ),
    )
}

/** Copies the characters without creating a String (concept 5.1). */
private fun CharSequence.copyChars(): CharArray = CharArray(length) { this[it] }

/** Stateless layout of M-01. */
@Composable
fun OnboardingContent(state: OnboardingUiState, password: TextFieldState, actions: OnboardingActions) {
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
            when (state.stage) {
                SignInStage.CREDENTIALS -> {
                    ServerSection(state, actions)
                    CredentialFields(state, password, actions)
                }
                SignInStage.CODE -> CodeSection(state, actions)
                SignInStage.BRIDGE -> BridgeHint()
            }
            state.error?.let { ErrorMessage(it) }
        }
        if (state.stage != SignInStage.CREDENTIALS) {
            TextButton(onClick = actions.onStartOver, enabled = !state.busy) {
                Text(stringResource(R.string.onboarding_start_over))
            }
        }
        PrimaryAction(state, actions)
    }
    state.cleartextWarningFor?.let { url -> CleartextDialog(url, actions) }
    state.certificateOffer?.let { offer -> TrustDialog(offer, actions) }
    state.addressOffer?.let { offer -> AddressOfferDialog(offer, actions) }
}

/** Sign in, Verify or Try again, depending on the stage. */
@Composable
private fun PrimaryAction(state: OnboardingUiState, actions: OnboardingActions) {
    val (label, onClick) = when (state.stage) {
        SignInStage.CREDENTIALS -> R.string.onboarding_sign_in to actions.onSignIn
        SignInStage.CODE -> R.string.onboarding_verify to actions.onSubmitCode
        SignInStage.BRIDGE -> R.string.onboarding_try_again to actions.onRetryBridgeCheck
    }
    val complete = state.stage != SignInStage.CREDENTIALS || state.username.isNotBlank()
    PrimaryButton(stringResource(label), enabled = !state.busy && complete, busy = state.busy, onClick = onClick)
}

/** Primary action: outlined in the accent colour with an arrow (concept 15.2). */
@Composable
private fun PrimaryButton(label: String, enabled: Boolean, busy: Boolean, onClick: () -> Unit) {
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
            Text(label, style = MaterialTheme.typography.titleMedium)
            if (busy) {
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

/** The server already belongs to a stored instance: add the address to it instead (concept 4.5). */
@Composable
private fun AddressOfferDialog(offer: SignInResult.SameInstance, actions: OnboardingActions) {
    val internal = offer.slot == AddressSlot.INTERNAL
    val slot = stringResource(if (internal) R.string.onboarding_slot_internal else R.string.onboarding_slot_external)
    val text = stringResource(R.string.onboarding_address_offer_text, offer.displayName, slot)
    val replaces = offer.replaces?.let { stringResource(R.string.onboarding_address_offer_replaces, it) }
    AlertDialog(
        onDismissRequest = actions.onAddressOfferDismissed,
        title = { Text(stringResource(R.string.onboarding_address_offer_title)) },
        text = { Text(listOfNotNull(text, replaces).joinToString("\n\n")) },
        confirmButton = {
            TextButton(onClick = actions.onAddAddress) {
                Text(stringResource(R.string.onboarding_address_offer_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onAddressOfferDismissed) {
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
                username = "anna",
            ),
            password = rememberTextFieldState(),
            actions = OnboardingActions(),
        )
    }
}

/** Asks whether to trust the self-signed certificate of the server on first use (concept 4.3). */
@Composable
private fun TrustDialog(offer: CertificateOffer, actions: OnboardingActions) {
    val certificate = offer.certificate
    CertificateDialog(
        details = CertificateDetails(
            offer.url.host,
            certificate.fingerprint,
            certificate.subject,
            certificate.expiresAt,
        ),
        kind = CertificateDialogKind.TRUST_NEW,
        onConfirm = actions.onCertificateTrusted,
        onDismiss = actions.onCertificateDismissed,
    )
}
