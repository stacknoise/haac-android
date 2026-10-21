package com.stacknoise.haac.feature.entities.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.PlaceIcon
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.database.layout.Room
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.RoomGroup

/** Width of the place tree in landscape (concept 15.7). */
internal val PlaceTreeWidth = 264.dp

/**
 * The place tree of the Expanded layout (concept 15.7, T-01): per home its levels and rooms, each room with its tile
 * count. It replaces the *Home · Level* menu and the room chips; [selected] is the shown room.
 */
@Composable
internal fun PlaceTree(
    groups: List<RoomGroup>,
    selected: String?,
    tileCounts: Map<String, Int>,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(PlaceTreeWidth)
            .fillMaxHeight()
            .background(HaacColors.NavBackground)
            .border(1.dp, HaacColors.NavBorder)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.rooms_place_tree).uppercase(),
            style = SectionLabelStyle,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        )
        var home: String? = null
        groups.forEach { group ->
            if (group.home.id != home) {
                home = group.home.id
                Text(group.home.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(8.dp))
            }
            group.floor?.let {
                Text(
                    it.name.uppercase(),
                    style = SectionLabelStyle,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
                )
            }
            group.rooms.forEach { room ->
                TreeRoom(room, room.id == selected, tileCounts[room.id] ?: 0) { onSelect(room.id) }
            }
        }
    }
}

/** One room row of the place tree: icon, name and tile count; the shown room is highlighted. */
@Composable
private fun TreeRoom(room: Room, selected: Boolean, count: Int, onClick: () -> Unit) {
    val background = if (selected) HaacColors.AccentTint else HaacColors.NavBackground
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(background, HaacShapes.Small)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
    ) {
        PlaceIcon.fromKey(room.icon)?.let { icon ->
            Icon(
                painterResource(icon.drawable),
                contentDescription = null,
                tint = if (selected) HaacColors.Accent else HaacColors.OnSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp).size(20.dp),
            )
        }
        Text(
            room.name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) HaacColors.Accent else HaacColors.OnSurface,
            modifier = Modifier.weight(1f),
        )
        Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = HaacColors.OnSurfaceVariant)
    }
}
