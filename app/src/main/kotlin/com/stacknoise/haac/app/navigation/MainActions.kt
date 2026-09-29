package com.stacknoise.haac.app.navigation

import com.stacknoise.haac.feature.instance.ui.InstanceListActions

/**
 * What the main area asks the root navigation to do: [onSignedOut] after logout or when HA no longer accepts the
 * instance's token (14.1), [instances] for switching, adding and removing instances (4.4).
 */
class MainActions(
    val onSignedOut: (String) -> Unit,
    val instances: InstanceListActions,
)
