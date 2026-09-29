package com.stacknoise.haac.feature.settings.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.ErrorMessage
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.network.connection.ConnectionState
import com.stacknoise.haac.feature.settings.R
import java.text.DateFormat
import java.util.Date

/** *Settings → Diagnostics* of the active instance (concept 9.3). */
@Composable
fun DiagnosticsSection(viewModel: DiagnosticsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DiagnosticsContent(state)
}

/** Stateless layout: connection, last sync, versions and revision; a connection error shows its code. */
@Composable
fun DiagnosticsContent(state: DiagnosticsUiState) {
    Text(stringResource(R.string.settings_diagnostics).uppercase(), style = SectionLabelStyle)
    Spacer(Modifier.height(8.dp))
    DiagnosticsRow(R.string.diagnostics_connection, stringResource(state.connection.label()))
    DiagnosticsRow(R.string.diagnostics_last_sync, state.lastSyncAt.asTime())
    DiagnosticsRow(R.string.diagnostics_ha_version, state.haVersion ?: None)
    DiagnosticsRow(R.string.diagnostics_bridge_api, state.bridgeApiVersion?.toString() ?: None)
    DiagnosticsRow(R.string.diagnostics_revision, state.revision?.take(RevisionChars) ?: None)
    when (val connection = state.connection) {
        is ConnectionState.Reconnecting -> ErrorMessage(connection.error.code)
        is ConnectionState.Failed -> ErrorMessage(connection.error.code)
        else -> Unit
    }
}

/** One label with its value, both on one line. */
@Composable
private fun DiagnosticsRow(@StringRes label: Int, value: String) {
    Row(modifier = Modifier.fillMaxWidth().height(28.dp)) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** The string resource for the state of the connection. */
@StringRes
private fun ConnectionState.label(): Int = when (this) {
    is ConnectionState.Connected -> R.string.diagnostics_connected
    ConnectionState.Connecting -> R.string.diagnostics_connecting
    is ConnectionState.Reconnecting -> R.string.diagnostics_reconnecting
    is ConnectionState.Failed -> R.string.diagnostics_failed
    ConnectionState.Idle -> R.string.diagnostics_idle
}

/** Date and time in the device's format, or a dash while nothing was synced yet. */
private fun Long?.asTime(): String =
    this?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)) } ?: None

/** Shown where a value is missing. */
private const val None = "–"

/** Length of the revision hash that is shown. */
private const val RevisionChars = 8
