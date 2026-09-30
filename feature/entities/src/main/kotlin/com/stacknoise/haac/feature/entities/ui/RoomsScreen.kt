package com.stacknoise.haac.feature.entities.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.ErrorMessage
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.EntityControl
import com.stacknoise.haac.feature.entities.domain.RoomGroup
import kotlinx.coroutines.flow.Flow

/**
 * The Rooms tab (M-05, M-08, concept 7, 8): header with *Home · Level* menu, room chips, banner for entities no
 * longer in HA and the tile grid with its controls. A long press opens *Rename* and *Remove from this room*;
 * failed service calls show a snackbar with the code.
 */
@Composable
fun RoomsScreen(navigation: RoomsNavigation, viewModel: RoomsViewModel = hiltViewModel()) {
    val snackbar = remember { SnackbarHostState() }
    Box(modifier = Modifier.fillMaxSize()) {
        RoomsContent(navigation, viewModel)
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
    FailureSnackbar(viewModel.failed, snackbar)
}

/** Shows each of the [failed] service calls as a snackbar: message and code (concept 14.1, 17.4). */
@Composable
internal fun FailureSnackbar(failed: Flow<HaacException>, snackbar: SnackbarHostState) {
    var failure by remember { mutableStateOf<ErrorCode?>(null) }
    LaunchedEffect(failed) { failed.collect { failure = it.code } }
    val code = failure ?: return
    val text = stringResource(R.string.tile_failed, stringResource(code.message), code.code)
    LaunchedEffect(code, text) {
        snackbar.showSnackbar(text)
        failure = null
    }
}

/** Header, chips, banner and grid of the shown room. */
@Composable
private fun RoomsContent(navigation: RoomsNavigation, viewModel: RoomsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf<RoomSheet?>(null) }
    val groups = state.groups ?: return
    val room = state.room
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Header(state, groups, navigation, viewModel::onSelectRoom)
        state.error?.let { ErrorMessage(it) }
        if (room == null) {
            NoRooms(navigation.onOpenPlaces)
            return@Column
        }
        RoomChips(state.group, room.id, viewModel::onSelectRoom)
        if (state.stale) StaleBanner()
        val withdrawn = state.tiles.filter { it.withdrawn }
        if (withdrawn.isNotEmpty()) WithdrawnBanner(withdrawn.size) { sheet = RoomSheet.Review(withdrawn) }
        if (state.tiles.isEmpty()) {
            EmptyRoom { navigation.onAddEntities(room.id) }
        } else {
            val actions = TileActions(
                onClick = { tile ->
                    when {
                        tile.withdrawn -> sheet = RoomSheet.Withdrawn(tile)
                        tile.control is EntityControl.Toggle -> viewModel.onToggle(tile)
                        else -> navigation.onOpenEntity(tile.entityId)
                    }
                },
                onLongClick = { tile ->
                    sheet = if (tile.withdrawn) RoomSheet.Withdrawn(tile) else RoomSheet.Menu(tile)
                },
                onToggle = viewModel::onToggle,
                onStep = viewModel::onStep,
            )
            val grid = Modifier.padding(vertical = 16.dp)
            RoomGrid(state.tiles, state.connected, actions, if (state.stale) grid.alpha(StaleAlpha) else grid)
        }
    }
    sheet?.let { open -> Sheets(open, viewModel, navigation.onOpenEntity, onChange = { sheet = it }) }
}

/** The open sheet or rename dialog; [onChange] switches to another one or closes it (null). */
@Composable
private fun Sheets(
    open: RoomSheet,
    viewModel: RoomsViewModel,
    onOpenEntity: (String) -> Unit,
    onChange: (RoomSheet?) -> Unit,
) {
    if (open is RoomSheet.Rename) {
        RenameDialog(
            tile = open.tile,
            onSave = { alias ->
                viewModel.onRename(open.tile.entityId, alias)
                onChange(null)
            },
            onDismiss = { onChange(null) },
        )
        return
    }
    RoomSheetContent(
        open,
        SheetActions(
            onDetails = {
                onChange(null)
                onOpenEntity(it.entityId)
            },
            onRename = { onChange(RoomSheet.Rename(it)) },
            onRemoveHere = {
                viewModel.onRemoveHere(it)
                onChange(null)
            },
            onRemoveEverywhere = {
                viewModel.onRemoveEverywhere(it)
                onChange(null)
            },
            onDismiss = { onChange(null) },
        ),
    )
}

/** *Home · Level* with its menu, the header actions, *Add entities* and the room name as title. */
@Composable
private fun Header(
    state: RoomsUiState,
    groups: List<RoomGroup>,
    navigation: RoomsNavigation,
    onSelect: (String) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            state.group?.let { group ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { menu = true }.padding(vertical = 8.dp),
                ) {
                    Text(
                        groupLabel(group),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        painterResource(R.drawable.ic_entities_expand),
                        stringResource(R.string.rooms_choose_group),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                groups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(groupLabel(group)) },
                        onClick = {
                            menu = false
                            onSelect(group.rooms.first().id)
                        },
                    )
                }
            }
        }
        navigation.headerActions()
        state.room?.let { room ->
            IconButton(onClick = { navigation.onEditLayout(room.id) }) {
                Icon(painterResource(R.drawable.ic_entities_edit), stringResource(R.string.rooms_edit_layout))
            }
            IconButton(onClick = { navigation.onAddEntities(room.id) }) {
                Icon(painterResource(R.drawable.ic_entities_add), stringResource(R.string.rooms_add_entities))
            }
        }
    }
    state.room?.let {
        Text(it.name, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(bottom = 8.dp))
    }
}

/** "Main house · Ground floor", or only the home for its rooms without a level. */
private fun groupLabel(group: RoomGroup): String = listOfNotNull(group.home.name, group.floor?.name).joinToString(" · ")

/** The rooms of the shown level as chips (M-05); a level with a single room shows none, its name is the title. */
@Composable
private fun RoomChips(group: RoomGroup?, selected: String, onSelect: (String) -> Unit) {
    if ((group?.rooms?.size ?: 0) < 2) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        group?.rooms?.forEach { room ->
            FilterChip(
                selected = room.id == selected,
                onClick = { onSelect(room.id) },
                label = { Text(room.name, modifier = Modifier.padding(vertical = 8.dp)) },
                shape = HaacShapes.Medium,
            )
        }
    }
}

/** Opacity of the tiles while their states are stale. */
private const val StaleAlpha = 0.6f

/** "Offline: showing the last known states" while the connection has been down for a while (concept 14.1). */
@Composable
private fun StaleBanner() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(MaterialTheme.colorScheme.surface, HaacShapes.Medium)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(painterResource(R.drawable.ic_entities_warning), contentDescription = null, Modifier.size(20.dp))
        Text(
            stringResource(R.string.rooms_stale),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** "1 entity no longer exists in Home Assistant" with *Review* (M-08). */
@Composable
private fun WithdrawnBanner(count: Int, onReview: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(MaterialTheme.colorScheme.surface, HaacShapes.Medium)
            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_entities_warning),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Text(
            pluralStringResource(R.plurals.rooms_withdrawn_banner, count, count),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
        TextButton(onClick = onReview) { Text(stringResource(R.string.rooms_review)) }
    }
}

/** Shown while the instance has no room: points to Places. */
@Composable
private fun NoRooms(onOpenPlaces: () -> Unit) {
    Text(
        stringResource(R.string.rooms_no_rooms),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 24.dp),
    )
    OutlinedButton(onClick = onOpenPlaces, shape = HaacShapes.Medium) {
        Text(stringResource(R.string.rooms_open_places))
    }
}

/** Shown for a room without tiles: *Add entities*. */
@Composable
private fun EmptyRoom(onAdd: () -> Unit) {
    Text(
        stringResource(R.string.rooms_empty_room),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
    )
    OutlinedButton(onClick = onAdd, shape = HaacShapes.Medium) { Text(stringResource(R.string.rooms_add_entities)) }
}
