package com.stacknoise.haac.feature.entities.ui

import android.os.Bundle
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.argsViewModel
import com.stacknoise.haac.core.common.ui.theme.HaacColors

/** Width of the detail pane beside the room grid in landscape (concept 15.7). */
internal val DetailPaneWidth = 380.dp

/**
 * The detail of [entityId] as a fixed pane on the right of the room grid (concept 15.7, T-02); [onClose] hides it.
 * The view model gets the entity as a navigation argument would pass it, so it reads it from `SavedStateHandle`.
 */
@Composable
internal fun EntityDetailPane(entityId: String, onClose: () -> Unit) {
    val args = Bundle().apply { putString(EntityDetailViewModel.ENTITY_ARG, entityId) }
    Box(
        modifier = Modifier
            .width(DetailPaneWidth)
            .fillMaxHeight()
            .border(1.dp, HaacColors.Outline),
    ) {
        EntityDetailScreen(onClose, argsViewModel(EntityDetailViewModel::class.java, entityId, args))
    }
}
