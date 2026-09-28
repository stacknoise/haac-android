package com.stacknoise.haac.feature.settings.ui

/** Callbacks of the security section. */
class SecurityActions(
    val onFingerprintChanged: (Boolean) -> Unit,
    val onUnlockWindowChanged: (Int) -> Unit,
    val onLockTimeoutChanged: (Int) -> Unit,
)
