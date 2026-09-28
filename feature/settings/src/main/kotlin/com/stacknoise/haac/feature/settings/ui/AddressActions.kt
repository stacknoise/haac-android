package com.stacknoise.haac.feature.settings.ui

import com.stacknoise.haac.core.network.endpoint.AddressSlot

/** Callbacks of the addresses section. */
class AddressActions(
    val onEdit: (AddressSlot) -> Unit,
    val onRemove: (AddressSlot) -> Unit,
    val onAlwaysUseInternalChanged: (Boolean) -> Unit,
    val onUseHaAddresses: () -> Unit,
    val onInputChanged: (String) -> Unit,
    val onSave: () -> Unit,
    val onDismissEdit: () -> Unit,
)
