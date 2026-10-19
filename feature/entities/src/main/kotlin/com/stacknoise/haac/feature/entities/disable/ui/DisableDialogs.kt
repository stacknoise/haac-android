package com.stacknoise.haac.feature.entities.disable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.haacButtonColors
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.disable.domain.DisablePreview
import com.stacknoise.haac.feature.entities.domain.Tile
import com.stacknoise.haac.feature.entities.ui.iconOf
import com.stacknoise.haac.feature.entities.ui.secondaryColor
import com.stacknoise.haac.feature.entities.ui.stateText

/**
 * The dialog *Deactivate?* (M-20) for an active tile, or the sheet with *Activate* (M-21) for a deactivated one
 * (concept 7.5). The schedules it names come from the cache; both buttons need a connection. [onDetails] adds
 * *Details* to the sheet (null where the detail screen is already open); [onClose] closes the dialog or sheet.
 */
@Composable
internal fun DisableFlow(tile: Tile, viewModel: DisableViewModel, onDetails: (() -> Unit)?, onClose: () -> Unit) {
    val connected by viewModel.connected.collectAsStateWithLifecycle()
    val preview by remember(tile.entityId) { viewModel.preview(tile.entityId) }
        .collectAsStateWithLifecycle(DisablePreview())
    if (tile.disabled) {
        DisabledSheet(
            tile = tile,
            preview = preview,
            connected = connected,
            onActivate = {
                viewModel.onEnable(tile.entityId)
                onClose()
            },
            onDetails = onDetails,
            onDismiss = onClose,
        )
    } else {
        DisableDialog(
            name = tile.name,
            preview = preview,
            connected = connected,
            onConfirm = {
                viewModel.onDisable(tile.entityId)
                onClose()
            },
            onDismiss = onClose,
        )
    }
}

/** *Deactivate <name>?* with the schedules that will be switched off and the scope of the change (M-20). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DisableDialog(
    name: String,
    preview: DisablePreview,
    connected: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = HaacShapes.Dialog, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(modifier = Modifier.padding(start = 26.dp, end = 26.dp, top = 24.dp, bottom = 16.dp)) {
                Text(
                    stringResource(R.string.disable_dialog_title, name),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    stringResource(R.string.disable_dialog_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
                if (preview.off.isNotEmpty()) {
                    val count = preview.off.size
                    ScheduleNote(
                        title = pluralStringResource(R.plurals.disable_dialog_schedules, count, count),
                        names = preview.off,
                        background = HaacColors.ChartYellow.copy(alpha = NoteAlpha),
                        content = HaacColors.ChartYellow,
                    )
                }
                Text(
                    stringResource(R.string.disable_dialog_scope),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
                if (!connected) OfflineNote()
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.disable_cancel)) }
                    Button(
                        onClick = onConfirm,
                        enabled = connected,
                        shape = HaacShapes.Button,
                        colors = haacButtonColors(),
                        modifier = Modifier.padding(start = 8.dp).heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.disable_confirm)) }
                }
            }
        }
    }
}

/** The sheet of a deactivated tile: what it means, the schedules that come on again and *Activate* (M-21). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DisabledSheet(
    tile: Tile,
    preview: DisablePreview,
    connected: Boolean,
    onActivate: () -> Unit,
    onDetails: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp).background(HaacColors.SurfaceMuted, HaacShapes.Medium),
                ) {
                    Icon(
                        painterResource(iconOf(tile.icon)),
                        contentDescription = null,
                        tint = HaacColors.OnSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(tile.name, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                    Text(stateText(tile), style = MaterialTheme.typography.bodyMedium, color = secondaryColor())
                }
                DeactivatedChip()
            }
            Text(
                stringResource(R.string.disable_sheet_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
            if (preview.on.isNotEmpty()) {
                val count = preview.on.size
                ScheduleNote(
                    title = pluralStringResource(R.plurals.disable_sheet_schedules, count, count),
                    names = preview.on,
                    background = HaacColors.AccentTint,
                    content = HaacColors.OnSurface,
                )
            }
            if (!connected) OfflineNote()
            Button(
                onClick = onActivate,
                enabled = connected,
                shape = HaacShapes.Button,
                colors = haacButtonColors(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(top = 16.dp),
            ) {
                Icon(painterResource(R.drawable.ic_entities_unlock), contentDescription = null, Modifier.size(20.dp))
                Text(stringResource(R.string.disable_activate), modifier = Modifier.padding(start = 8.dp))
            }
            onDetails?.let { open ->
                OutlinedButton(
                    onClick = open,
                    shape = HaacShapes.Button,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(top = 8.dp),
                ) { Text(stringResource(R.string.tile_details), color = HaacColors.OnSurfaceVariantStrong) }
            }
        }
    }
}

/** The box with the clock icon, a bold [title] and the schedule [names] in the colours [background], [content]. */
@Composable
private fun ScheduleNote(title: String, names: List<String>, background: Color, content: Color) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(background, HaacShapes.Medium)
            .padding(16.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_entities_clock),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = content,
            )
            Text(names.joinToString(", "), style = MaterialTheme.typography.bodyMedium, color = content)
        }
    }
}

/** *Needs a connection*, shown instead of a working button while the app is offline (concept 7.5, 14.1). */
@Composable
private fun OfflineNote() {
    Text(
        stringResource(R.string.disable_offline),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
}

/** *Deactivate* or, for a deactivated entity, *Activate* on the detail screen; it opens the dialog or the sheet. */
@Composable
internal fun DisableButton(disabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = HaacShapes.Button,
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(top = 16.dp),
    ) {
        val icon = if (disabled) R.drawable.ic_entities_unlock else R.drawable.ic_entities_lock
        Icon(painterResource(icon), contentDescription = null, Modifier.size(20.dp))
        val label = if (disabled) R.string.disable_menu_activate else R.string.disable_menu_deactivate
        Text(
            stringResource(label),
            color = HaacColors.OnSurfaceVariantStrong,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Shows "N schedules were switched on again" after an activation (M-21). */
@Composable
internal fun ReactivatedSnackbar(viewModel: DisableViewModel, snackbar: SnackbarHostState) {
    val resources = LocalContext.current.resources
    LaunchedEffect(viewModel) {
        viewModel.reactivated.collect { count ->
            snackbar.showSnackbar(resources.getQuantityString(R.plurals.disable_reactivated, count, count))
        }
    }
}

/** Opacity of the warning box behind the schedules a deactivation switches off. */
private const val NoteAlpha = 0.16f
