package com.stacknoise.haac.feature.entities.ui

import androidx.compose.runtime.Composable

/** Navigation out of the Rooms tab and the slot for the notification bell. */
class RoomsNavigation(
    val onAddEntities: (roomId: String) -> Unit,
    val onOpenEntity: (entityId: String) -> Unit,
    val onEditLayout: (roomId: String) -> Unit,
    val onOpenPlaces: () -> Unit,
    val headerActions: @Composable () -> Unit,
)
