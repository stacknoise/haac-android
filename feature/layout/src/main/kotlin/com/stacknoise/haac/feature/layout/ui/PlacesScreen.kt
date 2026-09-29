package com.stacknoise.haac.feature.layout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.ErrorMessage
import com.stacknoise.haac.core.common.ui.theme.HaacTheme
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.feature.layout.R
import com.stacknoise.haac.feature.layout.data.PlaceTrash
import com.stacknoise.haac.feature.layout.domain.Floor
import com.stacknoise.haac.feature.layout.domain.Home
import com.stacknoise.haac.feature.layout.domain.PlaceFilter
import com.stacknoise.haac.feature.layout.domain.PlaceKind
import com.stacknoise.haac.feature.layout.domain.PlaceRow
import com.stacknoise.haac.feature.layout.domain.Places
import com.stacknoise.haac.feature.layout.domain.Relation
import com.stacknoise.haac.feature.layout.domain.Room
import com.stacknoise.haac.feature.layout.domain.count
import com.stacknoise.haac.feature.layout.domain.rows
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Places overview (M-02, concept 6): every home, level and room with filter chips and the create menu.
 * [onOpen] opens the form for a place of a kind; the id is null for a new one.
 */
@Composable
fun PlacesScreen(onOpen: (PlaceKind, String?) -> Unit, viewModel: PlacesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val deletedText = pending?.let { stringResource(R.string.places_deleted, it.name) }
    val undo = stringResource(R.string.places_undo)
    LaunchedEffect(pending) {
        val deletion = pending ?: return@LaunchedEffect
        // The window counts from the deletion, so a snackbar shown again later only gets the rest of it.
        val remaining = PlaceTrash.UNDO_MS - (System.currentTimeMillis() - deletion.at)
        val result = withTimeoutOrNull(remaining.coerceAtLeast(0)) {
            snackbar.showSnackbar(deletedText.orEmpty(), undo, duration = SnackbarDuration.Indefinite)
        }
        when (result) {
            SnackbarResult.ActionPerformed -> viewModel.onUndo(deletion)
            else -> viewModel.onUndoExpired(deletion)
        }
    }
    val hasHome = !state.places?.homes.isNullOrEmpty()
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = { CreateMenu(hasHome = hasHome, onCreate = { onOpen(it, null) }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        PlacesContent(state, viewModel::onFilter, onOpen = { onOpen(it.kind, it.id) }, Modifier.padding(padding))
    }
}

/** Stateless layout: title, filter chips and the rows. */
@Composable
fun PlacesContent(
    state: PlacesUiState,
    onFilter: (PlaceFilter) -> Unit,
    onOpen: (PlaceRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    val places = state.places ?: return
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            Text(
                stringResource(R.string.places_title),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            )
        }
        item { FilterTabs(places, state.filter, onFilter) }
        state.error?.let { item { ErrorMessage(it, Modifier.padding(horizontal = 16.dp)) } }
        if (places.homes.isEmpty()) item { EmptyHint() }
        items(places.rows(state.filter), key = { "${it.kind}-${it.id}" }) { row ->
            PlaceRowItem(row, onClick = { onOpen(row) })
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

/** *All / Homes / Levels / Rooms* with their counts (M-02). */
@Composable
private fun FilterTabs(places: Places, selected: PlaceFilter, onFilter: (PlaceFilter) -> Unit) {
    val labels = mapOf(
        PlaceFilter.ALL to R.string.places_filter_all,
        PlaceFilter.HOMES to R.string.places_filter_homes,
        PlaceFilter.LEVELS to R.string.places_filter_levels,
        PlaceFilter.ROOMS to R.string.places_filter_rooms,
    )
    PrimaryScrollableTabRow(
        selectedTabIndex = selected.ordinal,
        edgePadding = 16.dp,
        containerColor = MaterialTheme.colorScheme.background,
        divider = {},
    ) {
        PlaceFilter.entries.forEach { filter ->
            Tab(
                selected = filter == selected,
                onClick = { onFilter(filter) },
                text = { Text("${stringResource(labels.getValue(filter))} ${places.count(filter)}") },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Type label, name and relation of one place. */
@Composable
private fun PlaceRowItem(row: PlaceRow, onClick: () -> Unit) {
    val (type, typeColor) = when (row.kind) {
        PlaceKind.HOME -> R.string.places_type_home to MaterialTheme.colorScheme.primary
        PlaceKind.FLOOR -> R.string.places_type_level to MaterialTheme.colorScheme.onSurfaceVariant
        PlaceKind.ROOM -> R.string.places_type_room to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(
            stringResource(type).uppercase(),
            style = SectionLabelStyle.copy(color = typeColor),
            modifier = Modifier.width(72.dp),
        )
        Text(
            row.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            relationText(row.relation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** "2 levels", "3 rooms" or the name of the linked place. */
@Composable
private fun relationText(relation: Relation): String = when (relation) {
    is Relation.Levels -> pluralStringResource(R.plurals.places_levels, relation.count, relation.count)
    is Relation.Rooms -> pluralStringResource(R.plurals.places_rooms, relation.count, relation.count)
    is Relation.In -> relation.name
}

/** Shown while the instance has no home. */
@Composable
private fun EmptyHint() {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            stringResource(R.string.places_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Preview with two homes, levels and rooms. */
@Preview
@Composable
private fun PlacesPreview() {
    val places = Places(
        homes = listOf(Home("h1", "Main house"), Home("h2", "Garden house")),
        floors = listOf(Floor("f1", "h1", "Ground floor", 0), Floor("f2", "h1", "First floor", 1)),
        rooms = listOf(Room("r1", "h1", "f1", "Living room"), Room("r2", "h2", null, "Shed")),
    )
    HaacTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            PlacesContent(PlacesUiState(places), {}, {})
        }
    }
}
