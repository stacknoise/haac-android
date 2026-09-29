package com.stacknoise.haac.feature.entities.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.ErrorMessage
import com.stacknoise.haac.core.common.ui.theme.MonoFontFamily
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.ControlRequest
import com.stacknoise.haac.feature.entities.domain.EntityDetail

/**
 * The detail screen of one entity (concept 8.1, 15.4): name and `entity_id`, current state, readings, the
 * controls of its domain, names and times, and every attribute. The pencil opens *Rename* (M-07).
 */
@Composable
fun EntityDetailScreen(onClose: () -> Unit, viewModel: EntityDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var renaming by rememberSaveable { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            TopRow(canRename = state.detail != null, onClose = onClose, onRename = { renaming = true })
            state.error?.let { ErrorMessage(it) }
            if (state.missing) Note(stringResource(R.string.detail_missing))
            state.detail?.let { DetailBody(it, state.connected, viewModel::onControl) }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
    FailureSnackbar(viewModel.failed, snackbar)
    val detail = state.detail
    if (renaming && detail != null) {
        RenameDialog(
            tile = detail.tile,
            onSave = {
                viewModel.onRename(it)
                renaming = false
            },
            onDismiss = { renaming = false },
        )
    }
}

/** *Back* and the pencil for *Rename*. */
@Composable
private fun TopRow(canRename: Boolean, onClose: () -> Unit, onRename: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        IconButton(onClick = onClose) {
            Icon(painterResource(R.drawable.ic_entities_back), stringResource(R.string.detail_back))
        }
        Spacer(Modifier.weight(1f))
        if (canRename) {
            IconButton(onClick = onRename) {
                Icon(painterResource(R.drawable.ic_entities_edit), stringResource(R.string.detail_rename))
            }
        }
    }
}

/** Title, state, readings, controls, information and attributes of [detail]. */
@Composable
private fun DetailBody(
    detail: EntityDetail,
    connected: Boolean,
    onControl: (ControlRequest?) -> Unit,
) {
    val tile = detail.tile
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(iconOf(tile.icon)),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp).padding(end = 4.dp),
        )
        Text(tile.name, style = MaterialTheme.typography.headlineLarge)
    }
    Text(tile.entityId, style = monoSmall(), color = secondaryColor())
    Text(
        primaryValue(detail),
        style = MaterialTheme.typography.displaySmall,
        modifier = Modifier.padding(top = 16.dp),
    )
    if (tile.withdrawn) Note(stringResource(R.string.tile_removed_text))
    detail.readings.forEach { InfoRow(readingLabel(it.kind), modeOrValue(it.kind, it.value)) }
    if (detail.controls.isNotEmpty()) {
        Section(R.string.detail_controls)
        if (!connected) Note(stringResource(R.string.detail_offline))
        DetailControls(detail.controls, enabled = connected, onControl = onControl)
    }
    if (!tile.withdrawn) {
        Section(R.string.history_title)
        HistorySection()
    }
    InfoSection(detail)
    Section(R.string.detail_attributes)
    if (detail.attributes.isEmpty()) Note(stringResource(R.string.detail_no_attributes))
    detail.attributes.forEach { InfoRow(it.name, it.value, mono = true) }
    Spacer(Modifier.padding(bottom = 32.dp))
}

/** HA's name, the bridge's name, `entity_id` and the times of the last change and update (concept 8.1). */
@Composable
private fun InfoSection(detail: EntityDetail) {
    Section(R.string.detail_info)
    InfoRow(stringResource(R.string.detail_ha_name), detail.haName)
    detail.configuredName?.let { InfoRow(stringResource(R.string.detail_configured_name), it) }
    InfoRow(stringResource(R.string.detail_entity_id), detail.tile.entityId, mono = true)
    detail.lastChanged?.let { InfoRow(stringResource(R.string.detail_last_changed), timeText(it)) }
    detail.lastUpdated?.let { InfoRow(stringResource(R.string.detail_last_updated), timeText(it)) }
}


/** A section heading with a divider above it. */
@Composable
private fun Section(@StringRes title: Int) {
    HorizontalDivider(modifier = Modifier.padding(top = 24.dp, bottom = 16.dp))
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/** A name and its value below it. */
@Composable
private fun InfoRow(label: String, value: String, mono: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = secondaryColor())
        Text(value, style = if (mono) monoSmall() else MaterialTheme.typography.bodyLarge)
    }
}

/** A short note in the secondary colour. */
@Composable
private fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = secondaryColor(),
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

/** Small text in the mono font (`entity_id`, attribute values). */
@Composable
private fun monoSmall(): TextStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily)
