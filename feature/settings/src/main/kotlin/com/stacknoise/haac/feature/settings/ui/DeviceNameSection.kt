package com.stacknoise.haac.feature.settings.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.theme.HaacCard
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.common.ui.theme.haacTextFieldColors
import com.stacknoise.haac.feature.settings.R

/**
 * Settings → Device name (concept 21.5): the name the app sends with a switching action, so Home Assistant can
 * show which device it came from. It is visible in Home Assistant, in its log and on its Activity page.
 */
@Composable
fun DeviceNameSection(viewModel: DeviceNameViewModel = hiltViewModel()) {
    val stored = viewModel.name.collectAsStateWithLifecycle().value
    // What the user typed is kept here, so a blank at the end survives while the stored name is trimmed.
    var typed by remember { mutableStateOf<String?>(null) }
    Text(stringResource(R.string.settings_device).uppercase(), style = SectionLabelStyle)
    Spacer(Modifier.height(8.dp))
    HaacCard {
        Text(stringResource(R.string.settings_device_name), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.settings_device_name_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = typed ?: stored,
            onValueChange = {
                typed = it.take(DeviceNameViewModel.MAX_LENGTH)
                viewModel.onNameChanged(it)
            },
            singleLine = true,
            shape = HaacShapes.Medium,
            colors = haacTextFieldColors(),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }
}
