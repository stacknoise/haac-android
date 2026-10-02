package com.stacknoise.haac.feature.schedules.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacEmptyState
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.common.ui.theme.haacButtonColors
import com.stacknoise.haac.core.common.ui.theme.haacFilterChipBorder
import com.stacknoise.haac.core.common.ui.theme.haacFilterChipColors
import com.stacknoise.haac.feature.schedules.R
import com.stacknoise.haac.feature.schedules.data.Candidate
import com.stacknoise.haac.feature.schedules.data.Candidates
import com.stacknoise.haac.feature.schedules.domain.ScheduleDraft

/**
 * Pick entities (M-14): the switches shared with the user as tiles to select, with the rooms as filter chips (rooms
 * only filter, the schedule does not know them). [selected] starts with the entities of the schedule; *Done* hands
 * the chosen ones to [onDone] in the order they were picked, Back leaves without a change.
 */
@Composable
internal fun EntityPicker(
    candidates: Candidates,
    selected: List<String>,
    editing: Boolean,
    onDone: (List<String>) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var chosen by rememberSaveable { mutableStateOf(selected) }
    var room by rememberSaveable { mutableStateOf<String?>(null) }
    val shown = candidates.inRoom(room).filter { it.active || it.entityId in chosen }
    val full = chosen.size >= ScheduleDraft.MAX_ENTITIES
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PickerHeader(editing, onBack)
        RoomChips(candidates, room) { room = it }
        Box(Modifier.weight(1f)) {
            if (shown.isEmpty()) {
                val empty = stringResource(R.string.schedules_pick_empty)
                HaacEmptyState(painterResource(R.drawable.ic_schedules_clock), empty)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(shown, key = { it.entityId }) { candidate ->
                        val on = candidate.entityId in chosen
                        PickerTile(candidate, on, enabled = on || !full) {
                            chosen = if (on) chosen - candidate.entityId else chosen + candidate.entityId
                        }
                    }
                }
            }
        }
        DoneBar(chosen.size, full) { onDone(chosen) }
    }
}

/** Back arrow, the eyebrow NEW SCHEDULE / EDIT SCHEDULE and the title. */
@Composable
private fun PickerHeader(editing: Boolean, onBack: () -> Unit) {
    Column(Modifier.padding(start = 8.dp, end = 20.dp, bottom = 8.dp)) {
        IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_schedules_back), stringResource(R.string.schedules_back))
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(
                stringResource(if (editing) R.string.schedules_edit_eyebrow else R.string.schedules_new_eyebrow),
                style = SectionLabelStyle,
            )
            Text(stringResource(R.string.schedules_pick_title), style = MaterialTheme.typography.headlineLarge)
        }
    }
}

/** *All* and one chip per room; they only filter the tiles. */
@Composable
private fun RoomChips(candidates: Candidates, room: String?, onRoom: (String?) -> Unit) {
    if (candidates.rooms.isEmpty()) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = room == null,
                onClick = { onRoom(null) },
                label = { Text(stringResource(R.string.schedules_pick_all)) },
                colors = haacFilterChipColors(),
                border = haacFilterChipBorder(enabled = true, selected = room == null),
            )
        }
        items(candidates.rooms, key = { it.id }) { chip ->
            FilterChip(
                selected = room == chip.id,
                onClick = { onRoom(chip.id) },
                label = { Text(chip.name) },
                colors = haacFilterChipColors(),
                border = haacFilterChipBorder(enabled = true, selected = room == chip.id),
            )
        }
    }
}

/** One entity tile (160 dp): the check badge, the name and its room; [on] marks the selection. */
@Composable
private fun PickerTile(candidate: Candidate, on: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = HaacShapes.Tile
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .height(160.dp)
            .background(if (on) HaacColors.AccentTint else HaacColors.Surface, shape)
            .border(if (on) 1.5.dp else 1.dp, if (on) HaacColors.AccentBorder else HaacColors.Outline, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Checkbox; selected = on }
            .padding(16.dp),
    ) {
        CheckBadge(on)
        Column {
            val title = MaterialTheme.typography.titleLarge
            Text(candidate.name, style = title, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val rooms = candidate.rooms.joinToString(", ") { it.name }
            val line = if (candidate.active) rooms else stringResource(R.string.schedules_pick_withdrawn)
            if (line.isNotEmpty()) {
                Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HaacColors.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The 28 dp badge: an accent circle with a white check when [on], a ring otherwise. */
@Composable
private fun CheckBadge(on: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .background(if (on) HaacColors.Accent else Color.Transparent, CircleShape)
            .then(if (on) Modifier else Modifier.border(2.dp, HaacColors.SwitchTrackOff, CircleShape)),
    ) {
        if (on) GlyphIcon(R.drawable.ic_schedules_check, HaacColors.OnAccent, 18.dp)
    }
}

/** The button at the bottom: *Done · N selected*, or *Select at least one entity*; a note once 20 are chosen. */
@Composable
private fun DoneBar(count: Int, full: Boolean, onDone: () -> Unit) {
    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
        if (full) {
            Text(
                stringResource(R.string.schedules_pick_limit),
                style = MaterialTheme.typography.bodyMedium,
                color = HaacColors.OnSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Button(
            onClick = onDone,
            enabled = count > 0,
            colors = haacButtonColors(),
            shape = HaacShapes.Button,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(
                if (count > 0) {
                    pluralStringResource(R.plurals.schedules_pick_done, count, count)
                } else {
                    stringResource(R.string.schedules_pick_select_one)
                },
            )
        }
    }
}
