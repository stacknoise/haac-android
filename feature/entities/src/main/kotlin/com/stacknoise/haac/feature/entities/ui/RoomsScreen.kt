package com.stacknoise.haac.feature.entities.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.stacknoise.haac.core.common.ui.LocalWindowWidthClass
import com.stacknoise.haac.core.common.ui.PlaceIcon
import com.stacknoise.haac.core.common.ui.WindowWidthClass
import com.stacknoise.haac.core.common.ui.theme.HaacEmptyState
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.common.ui.theme.haacFilterChipBorder
import com.stacknoise.haac.core.common.ui.theme.haacFilterChipColors
import com.stacknoise.haac.core.common.ui.theme.haacOutlinedButtonColors
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.disable.ui.DisableFlow
import com.stacknoise.haac.feature.entities.disable.ui.DisableViewModel
import com.stacknoise.haac.feature.entities.disable.ui.ReactivatedSnackbar
import com.stacknoise.haac.feature.entities.domain.RoomGroup
import com.stacknoise.haac.feature.entities.guard.GuardConfirmDialog
import com.stacknoise.haac.feature.entities.guard.GuardSettingsSheet
import com.stacknoise.haac.feature.entities.guard.SwitchGuard
import kotlinx.coroutines.flow.Flow

/**
 * The Rooms tab (M-05, M-08, concept 7, 8): header with *Home · Level* menu, room chips, banner for entities no
 * longer in HA and the tile grid with its controls. A long press opens *Rename* and *Remove from this room*;
 * failed service calls show a snackbar with the code.
 */
@Composable
fun RoomsScreen(
    navigation: RoomsNavigation,
    viewModel: RoomsViewModel = hiltViewModel(),
    disable: DisableViewModel = hiltViewModel(),
) {
    val snackbar = remember { SnackbarHostState() }
    Box(modifier = Modifier.fillMaxSize()) {
        RoomsContent(navigation, viewModel, disable)
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
    FailureSnackbar(viewModel.failed, snackbar)
    ReactivatedSnackbar(disable, snackbar)
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

/** The place tree (Expanded only) beside header, chips, banner and grid of the shown room, and the open sheet. */
@Composable
private fun RoomsContent(
    navigation: RoomsNavigation,
    viewModel: RoomsViewModel,
    disable: DisableViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf<RoomSheet?>(null) }
    val groups = state.groups ?: return
    val room = state.room
    val tree = LocalWindowWidthClass.current == WindowWidthClass.Expanded && room != null
    val counts by viewModel.tileCounts.collectAsStateWithLifecycle()
    Row(modifier = Modifier.fillMaxSize()) {
        if (tree) PlaceTree(groups, room?.id, counts, viewModel::onSelectRoom)
        RoomsColumn(state, groups, tree, navigation, viewModel, Modifier.weight(1f)) { sheet = it }
    }
    sheet?.let { open ->
        Sheets(open, state.guards, viewModel, disable, navigation.onOpenEntity, onChange = { sheet = it })
    }
    val prompt by viewModel.gate.prompt.collectAsStateWithLifecycle()
    prompt?.let { GuardConfirmDialog(it, onConfirm = viewModel.gate::confirm, onDismiss = viewModel.gate::dismiss) }
}

/** Header, chips (without the [tree]), banner and grid of the shown room; [onSheet] opens a sheet. */
@Composable
private fun RoomsColumn(
    state: RoomsUiState,
    groups: List<RoomGroup>,
    tree: Boolean,
    navigation: RoomsNavigation,
    viewModel: RoomsViewModel,
    modifier: Modifier,
    onSheet: (RoomSheet) -> Unit,
) {
    val room = state.room
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Header(state, groups, tree, navigation, viewModel::onSelectRoom)
        state.error?.let { ErrorMessage(it) }
        if (room == null) {
            NoRooms(navigation.onOpenPlaces)
            return@Column
        }
        if (!tree) RoomChips(state.group, room.id, viewModel::onSelectRoom)
        if (state.stale) StaleBanner()
        val withdrawn = state.tiles.filter { it.withdrawn }
        if (withdrawn.isNotEmpty()) WithdrawnBanner(withdrawn.size) { onSheet(RoomSheet.Review(withdrawn)) }
        if (state.tiles.isEmpty()) {
            EmptyRoom { navigation.onAddEntities(room.id) }
        } else {
            val actions = TileActions(
                onClick = { tile ->
                    when {
                        tile.withdrawn -> onSheet(RoomSheet.Withdrawn(tile))
                        tile.disabled -> onSheet(RoomSheet.Disable(tile))
                        else -> viewModel.onToggle(tile)
                    }
                },
                onLongClick = { tile ->
                    onSheet(if (tile.withdrawn) RoomSheet.Withdrawn(tile) else RoomSheet.Menu(tile))
                },
                onToggle = viewModel::onToggle,
                onStep = viewModel::onStep,
            )
            val grid = Modifier.padding(vertical = 16.dp)
            RoomGrid(state.tiles, state.connected, actions, if (state.stale) grid.alpha(StaleAlpha) else grid)
        }
    }
}

/** The open sheet or rename dialog; [onChange] switches to another one or closes it (null). */
@Composable
private fun Sheets(
    open: RoomSheet,
    guards: Map<String, SwitchGuard>,
    viewModel: RoomsViewModel,
    disable: DisableViewModel,
    onOpenEntity: (String) -> Unit,
    onChange: (RoomSheet?) -> Unit,
) {
    when (open) {
        is RoomSheet.Rename -> RenameDialog(
            tile = open.tile,
            onSave = { alias ->
                viewModel.onRename(open.tile.entityId, alias)
                onChange(null)
            },
            onDismiss = { onChange(null) },
        )
        is RoomSheet.Disable -> DisableFlow(
            tile = open.tile,
            viewModel = disable,
            onDetails = {
                onChange(null)
                onOpenEntity(open.tile.entityId)
            },
            onClose = { onChange(null) },
        )
        is RoomSheet.Guard -> GuardSettingsSheet(
            name = open.tile.name,
            guard = guards[open.tile.entityId],
            onChange = { viewModel.onGuardChange(open.tile.entityId, it) },
            onDismiss = { onChange(null) },
        )
        else -> RoomSheetContent(open, sheetActions(viewModel, onOpenEntity, onChange))
    }
}

/** *Home · Level* with its menu (not with the place [tree]), the header actions, *Add entities* and the room title. */
@Composable
private fun Header(
    state: RoomsUiState,
    groups: List<RoomGroup>,
    tree: Boolean,
    navigation: RoomsNavigation,
    onSelect: (String) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            state.group?.takeIf { !tree }?.let { group ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { menu = true }.padding(vertical = 8.dp),
                ) {
                    Text(groupLabel(group).uppercase(), style = SectionLabelStyle)
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
    state.room?.let { room ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            PlaceIcon.fromKey(room.icon)?.let { icon ->
                Icon(
                    painterResource(icon.drawable),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 12.dp).size(TitleIconSize),
                )
            }
            Text(room.name, style = MaterialTheme.typography.headlineLarge)
        }
    }
}

/** Size of the room icon in front of the title. */
private val TitleIconSize = 28.dp

/** "Main house · Ground floor", or only the home for its rooms without a level. */
private fun groupLabel(group: RoomGroup): String = listOfNotNull(group.home.name, group.floor?.name).joinToString(" · ")

/** The other rooms of the shown level as chips (M-05); the shown room is the title, so it has no chip. */
@Composable
private fun RoomChips(group: RoomGroup?, selected: String, onSelect: (String) -> Unit) {
    val others = group?.rooms.orEmpty().filter { it.id != selected }
    if (others.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        others.forEach { room ->
            FilterChip(
                selected = false,
                onClick = { onSelect(room.id) },
                label = { Text(room.name, style = MaterialTheme.typography.labelLarge) },
                leadingIcon = PlaceIcon.fromKey(room.icon)?.let { icon ->
                    { Icon(painterResource(icon.drawable), null, Modifier.size(FilterChipDefaults.IconSize)) }
                },
                shape = HaacShapes.Small,
                colors = haacFilterChipColors(),
                border = haacFilterChipBorder(enabled = true, selected = false),
                modifier = Modifier.height(40.dp),
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
            .background(MaterialTheme.colorScheme.surface, HaacShapes.Card)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, HaacShapes.Card)
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
            .background(MaterialTheme.colorScheme.surface, HaacShapes.Card)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, HaacShapes.Card)
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
    HaacEmptyState(painterResource(PlaceIcon.HOME.drawable), stringResource(R.string.rooms_no_rooms)) {
        OutlinedButton(onClick = onOpenPlaces, shape = HaacShapes.Button, colors = haacOutlinedButtonColors()) {
            Text(stringResource(R.string.rooms_open_places))
        }
    }
}

/** Shown for a room without tiles: *Add entities*. */
@Composable
private fun EmptyRoom(onAdd: () -> Unit) {
    HaacEmptyState(painterResource(R.drawable.ic_entities_add), stringResource(R.string.rooms_empty_room)) {
        OutlinedButton(onClick = onAdd, shape = HaacShapes.Button, colors = haacOutlinedButtonColors()) {
            Text(stringResource(R.string.rooms_add_entities))
        }
    }
}
