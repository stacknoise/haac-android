package com.stacknoise.haac.app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.stacknoise.haac.feature.entities.ui.AddEntitiesScreen
import com.stacknoise.haac.feature.entities.ui.AddEntitiesViewModel
import com.stacknoise.haac.feature.entities.ui.AssignScreen
import com.stacknoise.haac.feature.entities.ui.AssignViewModel
import com.stacknoise.haac.feature.entities.ui.EntityDetailScreen
import com.stacknoise.haac.feature.entities.ui.EntityDetailViewModel
import com.stacknoise.haac.feature.entities.ui.RoomsNavigation
import com.stacknoise.haac.feature.entities.ui.RoomsScreen
import com.stacknoise.haac.feature.layout.ui.PlaceEditorScreen
import com.stacknoise.haac.feature.layout.ui.PlaceEditorViewModel
import com.stacknoise.haac.feature.layout.ui.PlacesScreen
import com.stacknoise.haac.feature.notifications.ui.NotificationBell

/** The Rooms tab (M-05) with the notification bell in its header (M-08). */
internal fun NavGraphBuilder.roomsDestination(navController: NavController) {
    composable(TopLevelDestination.ROOMS.route) {
        RoomsScreen(
            RoomsNavigation(
                onAddEntities = { roomId -> navController.navigate(EntityRoutes.add(roomId)) },
                onOpenEntity = { entityId -> navController.navigate(EntityRoutes.detail(entityId)) },
                onOpenPlaces = { navController.openTopLevel(TopLevelDestination.PLACES) },
                headerActions = { NotificationBell(onClick = { navController.navigate(NotificationsRoute) }) },
            ),
        )
    }
}

/** The Places tab (M-02) and the place form (M-03). */
internal fun NavGraphBuilder.placeDestinations(navController: NavController) {
    composable(TopLevelDestination.PLACES.route) {
        PlacesScreen(onOpen = { kind, id -> navController.navigate(PlaceRoutes.editor(kind, id)) })
    }
    composable(
        PlaceRoutes.EDITOR,
        arguments = listOf(
            navArgument(PlaceEditorViewModel.KIND_ARG) { type = NavType.StringType },
            navArgument(PlaceEditorViewModel.ID_ARG) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) {
        PlaceEditorScreen(onClose = { navController.popBackStack() })
    }
}

/** *Add entities* (M-04), the detail screen (15.4) and *Add to room* from the notification list (M-09). */
internal fun NavGraphBuilder.entityDestinations(navController: NavController) {
    composable(
        EntityRoutes.DETAIL,
        arguments = listOf(navArgument(EntityDetailViewModel.ENTITY_ARG) { type = NavType.StringType }),
    ) {
        EntityDetailScreen(onClose = { navController.popBackStack() })
    }
    composable(
        EntityRoutes.ADD,
        arguments = listOf(navArgument(AddEntitiesViewModel.ROOM_ARG) { type = NavType.StringType }),
    ) {
        AddEntitiesScreen(onClose = { navController.popBackStack() })
    }
    composable(
        EntityRoutes.ASSIGN,
        arguments = listOf(
            navArgument(AssignViewModel.IDS_ARG) {
                type = NavType.StringType
                defaultValue = ""
            },
        ),
    ) {
        AssignScreen(onClose = { navController.popBackStack() })
    }
}
