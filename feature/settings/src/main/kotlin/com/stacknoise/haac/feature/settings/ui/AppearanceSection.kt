package com.stacknoise.haac.feature.settings.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.theme.HaacCard
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.common.ui.theme.haacSegmentedColors
import com.stacknoise.haac.core.database.settings.ThemeMode
import com.stacknoise.haac.feature.settings.R

/** Settings → Appearance (concept 15.2): follow the system, always light or always dark. */
@Composable
fun AppearanceSection(viewModel: AppearanceViewModel = hiltViewModel()) {
    val mode = viewModel.mode.collectAsStateWithLifecycle().value
    Text(stringResource(R.string.settings_appearance).uppercase(), style = SectionLabelStyle)
    Spacer(Modifier.height(8.dp))
    HaacCard {
        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.settings_theme_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            val modes = ThemeMode.entries
            modes.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == mode,
                    onClick = { viewModel.onModeChanged(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                    colors = haacSegmentedColors(),
                    label = { Text(stringResource(modeLabel(option))) },
                )
            }
        }
    }
}

/** The label of a design mode. */
private fun modeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}
