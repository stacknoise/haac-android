package com.stacknoise.haac.feature.onboarding.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.stacknoise.haac.core.common.ui.CertificateDetails
import com.stacknoise.haac.core.common.ui.CertificateDialog
import com.stacknoise.haac.core.common.ui.CertificateDialogKind
import com.stacknoise.haac.core.network.endpoint.AddressSlot
import com.stacknoise.haac.feature.onboarding.R
import com.stacknoise.haac.feature.onboarding.domain.SignInResult

/** The dialogs of M-01 that [state] asks for: cleartext warning, certificate, address offer and fingerprint. */
@Composable
internal fun OnboardingDialogs(state: OnboardingUiState, actions: OnboardingActions) {
    state.cleartextWarningFor?.let { url -> CleartextDialog(url, actions) }
    state.certificateOffer?.let { offer -> TrustDialog(offer, actions) }
    state.addressOffer?.let { offer -> AddressOfferDialog(offer, actions) }
    if (state.fingerprintOffer != null) FingerprintDialog(state.busy, actions)
}

/** Warning before an unencrypted connection to a private address (concept 4.3). */
@Composable
internal fun CleartextDialog(url: String, actions: OnboardingActions) {
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
internal fun AddressOfferDialog(offer: SignInResult.SameInstance, actions: OnboardingActions) {
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

/** Offers fingerprint unlock for the new instance; either answer opens it (concept 4.4, 5.4). */
@Composable
internal fun FingerprintDialog(busy: Boolean, actions: OnboardingActions) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.onboarding_fingerprint_title)) },
        text = { Text(stringResource(R.string.onboarding_fingerprint_text)) },
        confirmButton = {
            TextButton(onClick = actions.onEnableFingerprint, enabled = !busy) {
                Text(stringResource(R.string.onboarding_fingerprint_enable))
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onSkipFingerprint, enabled = !busy) {
                Text(stringResource(R.string.onboarding_fingerprint_skip))
            }
        },
    )
}


/** Asks whether to trust the self-signed certificate of the server on first use (concept 4.3). */
@Composable
internal fun TrustDialog(offer: CertificateOffer, actions: OnboardingActions) {
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
