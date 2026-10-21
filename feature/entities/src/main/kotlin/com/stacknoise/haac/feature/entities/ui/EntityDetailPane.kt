package com.stacknoise.haac.feature.entities.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stacknoise.haac.core.common.ui.theme.HaacColors

/** Width of the detail pane beside the room grid in landscape (concept 15.7). */
internal val DetailPaneWidth = 380.dp

/**
 * The detail of [entityId] as a fixed pane on the right of the room grid (concept 15.7, T-02); [onClose] hides it.
 * The view model gets the entity as a navigation argument would pass it, so it reads it from `SavedStateHandle`.
 */
@Composable
internal fun EntityDetailPane(entityId: String, onClose: () -> Unit) {
    val owner = checkNotNull(LocalViewModelStoreOwner.current)
    val extras = remember(owner, entityId) { owner.extrasWith(entityId) }
    Box(
        modifier = Modifier
            .width(DetailPaneWidth)
            .fillMaxHeight()
            .border(1.dp, HaacColors.Outline),
    ) {
        EntityDetailScreen(onClose, viewModel(viewModelStoreOwner = owner, key = entityId, extras = extras))
    }
}

/** The creation extras of this owner with [entityId] as the default argument of the detail view model. */
private fun ViewModelStoreOwner.extrasWith(entityId: String): CreationExtras {
    val base = (this as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras ?: CreationExtras.Empty
    return MutableCreationExtras(base).apply {
        set(DEFAULT_ARGS_KEY, bundleOf(EntityDetailViewModel.ENTITY_ARG to entityId))
    }
}
