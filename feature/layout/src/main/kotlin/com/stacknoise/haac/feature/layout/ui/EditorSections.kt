package com.stacknoise.haac.feature.layout.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.PlaceIcon
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.feature.layout.R
import com.stacknoise.haac.feature.layout.domain.PlaceForm
import com.stacknoise.haac.feature.layout.domain.PlaceKind
import com.stacknoise.haac.core.database.layout.Places

/** Small uppercase label above a section of the form. */
@Composable
internal fun SectionLabel(@StringRes text: Int) {
    Text(
        stringResource(text).uppercase(),
        style = SectionLabelStyle,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

/**
 * *Belongs to home*: one required choice (concept 6.1, 15.5). A level cannot move to another home (6.2), so
 * editing a level shows only its home.
 */
@Composable
internal fun HomeChips(form: PlaceForm, places: Places, onSelect: (String) -> Unit) {
    val homes = if (form.kind == PlaceKind.FLOOR && !form.isNew) {
        listOfNotNull(places.home(form.homeId))
    } else {
        places.homes
    }
    SectionLabel(R.string.editor_home_section)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        homes.forEach { home -> Choice(home.name, home.id == form.homeId) { onSelect(home.id) } }
    }
}

/** *Level* of a room: *No level* or one of the levels of its home (concept 6.1). */
@Composable
internal fun FloorChips(form: PlaceForm, places: Places, onSelect: (String?) -> Unit) {
    val floors = places.floorsOf(form.homeId)
    if (floors.isEmpty()) return
    SectionLabel(R.string.editor_level_section)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Choice(stringResource(R.string.editor_no_level), form.floorId == null) { onSelect(null) }
        floors.forEach { floor -> Choice(floor.name, floor.id == form.floorId) { onSelect(floor.id) } }
    }
}

/** Level number of a level with − and + (sorts the levels of a home, concept 6.1). */
@Composable
internal fun LevelStepper(level: Int, onChange: (Int) -> Unit) {
    SectionLabel(R.string.editor_level_number)
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(level - 1) }) {
            Icon(painterResource(R.drawable.ic_layout_remove), stringResource(R.string.editor_level_down))
        }
        Text(
            level.toString(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 40.dp),
        )
        IconButton(onClick = { onChange(level + 1) }) {
            Icon(painterResource(R.drawable.ic_layout_add), stringResource(R.string.editor_level_up))
        }
    }
    Text(
        stringResource(R.string.editor_level_number_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** *Icon*: the icons of [PlaceIcon] as a single choice; tapping the chosen icon again clears it (concept 6.1). */
@Composable
internal fun IconPicker(selected: String?, onSelect: (String?) -> Unit) {
    SectionLabel(R.string.editor_icon_section)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PlaceIcon.entries.forEach { icon ->
            val chosen = icon.key == selected
            FilledIconToggleButton(
                checked = chosen,
                onCheckedChange = { onSelect(if (chosen) null else icon.key) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(painterResource(icon.drawable), stringResource(icon.label))
            }
        }
    }
}

/** Single-select chip with a check mark when selected (M-03). */
@Composable
internal fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        shape = HaacShapes.Small,
        label = { Text(label, modifier = Modifier.padding(vertical = 8.dp)) },
        leadingIcon = if (selected) {
            { Icon(painterResource(R.drawable.ic_layout_check), null, Modifier.size(FilterChipDefaults.IconSize)) }
        } else {
            null
        },
    )
}
