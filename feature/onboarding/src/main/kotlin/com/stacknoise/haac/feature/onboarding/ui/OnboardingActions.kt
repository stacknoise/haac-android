package com.stacknoise.haac.feature.onboarding.ui

import com.stacknoise.haac.feature.onboarding.domain.DiscoveredServer

/** Callbacks of the onboarding screen, grouped to keep composable signatures short. */
data class OnboardingActions(
    val onServerSelected: (DiscoveredServer) -> Unit = {},
    val onOtherAddress: () -> Unit = {},
    val onManualUrlChanged: (String) -> Unit = {},
    val onContinue: () -> Unit = {},
    val onCleartextConfirmed: () -> Unit = {},
    val onCleartextDismissed: () -> Unit = {},
)
