package com.stacknoise.haac.feature.onboarding.ui

import com.stacknoise.haac.core.network.discovery.DiscoveredServer

/** Callbacks of the onboarding screen, grouped to keep composable signatures short. */
data class OnboardingActions(
    val onServerSelected: (DiscoveredServer) -> Unit = {},
    val onOtherAddress: () -> Unit = {},
    val onRescan: () -> Unit = {},
    val onManualUrlChanged: (String) -> Unit = {},
    val onUsernameChanged: (String) -> Unit = {},
    val onCodeChanged: (String) -> Unit = {},
    val onSignIn: () -> Unit = {},
    val onSubmitCode: () -> Unit = {},
    val onRetryBridgeCheck: () -> Unit = {},
    val onStartOver: () -> Unit = {},
    val onCleartextConfirmed: () -> Unit = {},
    val onCleartextDismissed: () -> Unit = {},
    val onCertificateTrusted: () -> Unit = {},
    val onCertificateDismissed: () -> Unit = {},
    val onAddAddress: () -> Unit = {},
    val onAddressOfferDismissed: () -> Unit = {},
)
