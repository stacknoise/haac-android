package com.stacknoise.haac.feature.instance.ui

import com.stacknoise.haac.feature.instance.domain.RemoveOutcome
import com.stacknoise.haac.feature.instance.domain.SwitchStep

/** What the instance list asks the navigation to do: a switch (concept 4.4), *Add instance* or a removal. */
class InstanceListActions(
    val onSwitched: (String, SwitchStep) -> Unit,
    val onAdd: () -> Unit,
    val onRemoved: (RemoveOutcome) -> Unit,
)
