package com.stacknoise.haac.app.navigation

import com.stacknoise.haac.feature.instance.domain.SwitchStep

/**
 * What the main area asks the root navigation to do: [onSignedOut] after logout or when HA no longer accepts the
 * instance's token (14.1), [onSwitched] after the user picked another instance (4.4), [onAddInstance] for the
 * sign-in flow of a new one.
 */
class MainActions(
    val onSignedOut: (String) -> Unit,
    val onSwitched: (String, SwitchStep) -> Unit,
    val onAddInstance: () -> Unit,
)
