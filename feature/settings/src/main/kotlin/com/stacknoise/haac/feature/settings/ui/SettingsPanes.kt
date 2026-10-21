package com.stacknoise.haac.feature.settings.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.ErrorMessage
import com.stacknoise.haac.core.common.ui.centeredContent
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacScreenTitle
import com.stacknoise.haac.feature.settings.R

/** Width of the category list on the left of the expanded settings (concept 15.7). */
private val CategoryListWidth: Dp = 264.dp

/** The groups of the expanded settings, in the order of the list (concept 15.7); the demo has no addresses. */
internal enum class SettingsCategory(@StringRes val label: Int) {
    INSTANCE(R.string.settings_instance),
    ADDRESSES(R.string.settings_addresses),
    APPEARANCE(R.string.settings_appearance),
    SECURITY(R.string.settings_security),
    DIAGNOSTICS(R.string.settings_diagnostics),
    ABOUT(R.string.settings_about),
}

/** The sections of the settings as slots; [addresses] is left out for the demo instance. */
internal class SettingsSections(
    val instances: @Composable () -> Unit,
    val addresses: @Composable () -> Unit,
    val appearance: @Composable () -> Unit,
    val diagnostics: @Composable () -> Unit,
    val about: @Composable () -> Unit,
)

/** Expanded settings: the categories on the left, the selected one on the right (concept 15.7). */
@Composable
internal fun SettingsPanes(
    state: SettingsUiState,
    demo: Boolean,
    security: SecurityActions,
    sections: SettingsSections,
    onSignOut: () -> Unit,
) {
    val categories = SettingsCategory.entries.filter { !demo || it != SettingsCategory.ADDRESSES }
    var selected by rememberSaveable { mutableStateOf(SettingsCategory.INSTANCE) }
    val current = selected.takeIf { it in categories } ?: SettingsCategory.INSTANCE
    Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.width(CategoryListWidth).fillMaxHeight().padding(horizontal = 20.dp, vertical = 16.dp)) {
            HaacScreenTitle(stringResource(R.string.settings_title))
            Spacer(Modifier.height(16.dp))
            Column(Modifier.selectableGroup()) {
                categories.forEach { CategoryRow(it, selected = it == current) { selected = it } }
            }
            Spacer(Modifier.weight(1f))
            // The demo is removed in the instance list; signing out would ask for a login it does not have.
            if (!demo) SignOutButton(state.busy, onSignOut)
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(HaacColors.NavBorder))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .centeredContent()
                .padding(horizontal = 28.dp, vertical = 16.dp),
        ) {
            CategoryBody(current, state, security, sections)
        }
    }
}

/** One entry of the category list, with an accent bar at its left edge when [selected]. */
@Composable
private fun CategoryRow(category: SettingsCategory, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) HaacColors.Accent else HaacColors.OnSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab),
    ) {
        Box(
            Modifier
                .size(width = 3.dp, height = 24.dp)
                .background(if (selected) color else HaacColors.NavBackground, RoundedCornerShape(3.dp)),
        )
        Text(
            stringResource(category.label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** The content of [category]. */
@Composable
private fun CategoryBody(
    category: SettingsCategory,
    state: SettingsUiState,
    security: SecurityActions,
    sections: SettingsSections,
) {
    when (category) {
        SettingsCategory.INSTANCE -> {
            InstanceCard(state)
            Spacer(Modifier.height(32.dp))
            sections.instances()
        }
        SettingsCategory.ADDRESSES -> sections.addresses()
        SettingsCategory.APPEARANCE -> sections.appearance()
        SettingsCategory.SECURITY -> {
            SecuritySection(state.security, state.busy, security)
            state.error?.let { ErrorMessage(it) }
        }
        SettingsCategory.DIAGNOSTICS -> sections.diagnostics()
        SettingsCategory.ABOUT -> sections.about()
    }
}
