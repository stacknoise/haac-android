package com.stacknoise.haac.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.CertificateDetails
import com.stacknoise.haac.core.common.ui.CertificateDialog
import com.stacknoise.haac.core.common.ui.ErrorMessage
import com.stacknoise.haac.core.common.ui.theme.HaacCard
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.MonoFontFamily
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.common.ui.theme.haacSwitchColors
import com.stacknoise.haac.core.common.ui.theme.haacTextFieldColors
import com.stacknoise.haac.core.network.endpoint.AddressSlot
import com.stacknoise.haac.feature.settings.R

/** *Settings → Addresses* of the active instance (concept 4.5). */
@Composable
fun AddressesSection(viewModel: AddressesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AddressesContent(
        state,
        AddressActions(
            onEdit = viewModel::onEdit,
            onRemove = viewModel::onRemove,
            onAlwaysUseInternalChanged = viewModel::onAlwaysUseInternalChanged,
            onUseHaAddresses = viewModel::onUseHaAddresses,
            onInputChanged = viewModel::onInputChanged,
            onSave = viewModel::onSave,
            onDismissEdit = viewModel::onDismissEdit,
            onCertificate = viewModel::onCertificate,
            onCertificateConfirm = viewModel::onCertificateConfirm,
            onCertificateDismiss = viewModel::onCertificateDismiss,
        ),
    )
}

/** Stateless layout of the addresses section. */
@Composable
fun AddressesContent(state: AddressesUiState, actions: AddressActions) {
    Text(stringResource(R.string.settings_addresses).uppercase(), style = SectionLabelStyle)
    Spacer(Modifier.height(8.dp))
    val both = state.internal != null && state.external != null
    AddressRow(AddressSlot.INTERNAL, state.internal, removable = both, enabled = !state.busy, actions = actions)
    Spacer(Modifier.height(10.dp))
    AddressRow(AddressSlot.EXTERNAL, state.external, removable = both, enabled = !state.busy, actions = actions)
    Spacer(Modifier.height(10.dp))
    HaacCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(stringResource(R.string.settings_always_internal), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.settings_always_internal_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.alwaysUseInternal,
                onCheckedChange = actions.onAlwaysUseInternalChanged,
                enabled = !state.busy && state.internal != null,
                colors = haacSwitchColors(),
            )
        }
    }
    TextButton(onClick = actions.onUseHaAddresses, enabled = !state.busy) {
        Text(stringResource(R.string.settings_use_ha_addresses))
    }
    if (state.editing == null) state.error?.let { ErrorMessage(it) }
    state.editing?.let { slot -> EditAddressDialog(slot, state, actions) }
    state.certificate?.let { review -> CertificateReviewDialog(review, actions) }
}

/** One address with its label and the Edit and Remove actions. */
@Composable
private fun AddressRow(
    slot: AddressSlot,
    url: String?,
    removable: Boolean,
    enabled: Boolean,
    actions: AddressActions,
) {
    HaacCard {
        Text(stringResource(slot.label()), style = MaterialTheme.typography.titleMedium)
        Text(
            url ?: stringResource(R.string.settings_address_not_set),
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row {
            TextButton(onClick = { actions.onEdit(slot) }, enabled = enabled) {
                Text(stringResource(R.string.settings_address_edit))
            }
            if (url?.startsWith("https://") == true) {
                TextButton(onClick = { actions.onCertificate(slot) }, enabled = enabled) {
                    Text(stringResource(R.string.settings_address_certificate))
                }
            }
            if (removable) {
                TextButton(
                    onClick = { actions.onRemove(slot) },
                    enabled = enabled,
                    colors = ButtonDefaults.textButtonColors(contentColor = HaacColors.Danger),
                ) { Text(stringResource(R.string.settings_address_remove)) }
            }
        }
    }
}

/** Dialog to enter the address of [slot]; it is checked against the instance before it is stored. */
@Composable
private fun EditAddressDialog(slot: AddressSlot, state: AddressesUiState, actions: AddressActions) {
    AlertDialog(
        onDismissRequest = actions.onDismissEdit,
        title = { Text(stringResource(slot.label())) },
        text = {
            Column {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = actions.onInputChanged,
                    placeholder = { Text(stringResource(R.string.settings_address_placeholder)) },
                    singleLine = true,
                    enabled = !state.busy,
                    shape = HaacShapes.Medium,
                    colors = haacTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                state.error?.let { ErrorMessage(it) }
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onSave, enabled = !state.busy && state.input.isNotBlank()) {
                Text(stringResource(R.string.settings_save))
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onDismissEdit, enabled = !state.busy) {
                Text(stringResource(R.string.settings_cancel))
            }
        },
    )
}

/** Label of the slot in the UI. */
private fun AddressSlot.label(): Int =
    if (this == AddressSlot.INTERNAL) R.string.settings_address_internal else R.string.settings_address_external

/** The certificate of an address for the manual comparison, with the pin action of its kind (concept 4.3). */
@Composable
private fun CertificateReviewDialog(review: CertificateReview, actions: AddressActions) {
    val certificate = review.certificate
    CertificateDialog(
        details = CertificateDetails(
            review.url.host,
            certificate.fingerprint,
            certificate.subject,
            certificate.expiresAt,
        ),
        kind = review.kind,
        onConfirm = actions.onCertificateConfirm,
        onDismiss = actions.onCertificateDismiss,
    )
}
