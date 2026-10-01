package com.stacknoise.haac.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stacknoise.haac.app.access.RequestLocalNetworkAccess
import com.stacknoise.haac.app.connection.ConnectionBanner
import com.stacknoise.haac.app.connection.ConnectionViewModel
import com.stacknoise.haac.app.licenses.LicensesScreen
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.error.ErrorAction
import com.stacknoise.haac.feature.instance.ui.InstanceBar
import com.stacknoise.haac.feature.instance.ui.InstancesSection
import com.stacknoise.haac.feature.notifications.ui.NotificationsScreen
import com.stacknoise.haac.feature.settings.ui.SettingsScreen

/**
 * Main area after sign-in: instance switcher (concept 4.4), bottom bar with Rooms, Places, Settings (15.2) and the
 * live connection of the active instance (11.4). Another active instance discards the open screens: its own
 * home view opens (4.4).
 */
@Composable
fun MainScaffold(actions: MainActions, connection: ConnectionViewModel = hiltViewModel()) {
    val activeId by connection.activeId.collectAsStateWithLifecycle()
    val signInRequired by connection.signInRequired.collectAsStateWithLifecycle()
    LifecycleStartEffect(connection) {
        connection.onForeground()
        onStopOrDispose { connection.onBackground() }
    }
    LaunchedEffect(signInRequired) { signInRequired?.let(actions.onSignedOut) }
    val hasInternalAddress by connection.hasInternalAddress.collectAsStateWithLifecycle()
    RequestLocalNetworkAccess(enabled = hasInternalAddress, onResult = { if (it) connection.retry() })
    key(activeId) { MainContent(actions, connection) }
}

/** The screens of one instance: top bar with switcher and connection banner, tabs and their destinations. */
@Composable
private fun MainContent(actions: MainActions, connection: ConnectionViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val connectionState by connection.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            // The bar draws under the status bar; the banner inside it then adds no second inset.
            Column(Modifier.statusBarsPadding()) {
                InstanceBar(onSwitched = actions.instances.onSwitched, onAdd = actions.instances.onAdd)
                ConnectionBanner(
                    state = connectionState,
                    onRetry = connection::retry,
                    onOpenSettings = { navController.openTopLevel(TopLevelDestination.SETTINGS) },
                )
            }
        },
        bottomBar = {
            // Forms and the notification list use the whole screen (M-03, M-09).
            if (TopLevelDestination.entries.any { it.route == currentRoute }) {
                BottomBar(currentRoute, onOpen = navController::openTopLevel)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.ROOMS.route,
            modifier = Modifier.padding(padding),
        ) {
            roomsDestination(navController)
            placeDestinations(navController)
            entityDestinations(navController)
            composable(TopLevelDestination.SETTINGS.route) {
                SettingsScreen(
                    onSignedOut = actions.onSignedOut,
                    onOpenLicenses = { navController.navigate(LicensesRoute) },
                    instances = { InstancesSection(actions.instances) },
                )
            }
            composable(LicensesRoute) { LicensesScreen(onBack = { navController.popBackStack() }) }
            composable(NotificationsRoute) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onErrorAction = { action, serverId ->
                        when (action) {
                            ErrorAction.RETRY -> connection.retry()
                            ErrorAction.SIGN_IN -> serverId?.let(actions.onSignedOut)
                            ErrorAction.OPEN_SETTINGS -> navController.openTopLevel(TopLevelDestination.SETTINGS)
                            ErrorAction.NONE -> Unit
                        }
                    },
                    onAddToRoom = { ids -> navController.navigate(EntityRoutes.assign(ids)) },
                )
            }
        }
    }
}

/** Bottom bar with Rooms, Places and Settings; [currentRoute] is selected. Draws an accent bar, not M3's pill. */
@Composable
private fun BottomBar(currentRoute: String?, onOpen: (TopLevelDestination) -> Unit) {
    Column(Modifier.fillMaxWidth().background(HaacColors.NavBackground)) {
        HorizontalDivider(color = HaacColors.NavBorder)
        Row(Modifier.navigationBarsPadding().selectableGroup()) {
            TopLevelDestination.entries.forEach { destination ->
                BottomBarItem(destination, selected = currentRoute == destination.route) { onOpen(destination) }
            }
        }
    }
}

/** One item of the bottom bar: icon over label, with a 24×3 dp accent bar at the top edge when [selected]. */
@Composable
private fun RowScope.BottomBarItem(destination: TopLevelDestination, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) HaacColors.Accent else HaacColors.OnSurfaceVariant
    Box(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = BarItemHeight)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab),
    ) {
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 24.dp, height = 3.dp)
                    .background(color, RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)),
            )
        }
        Column(
            modifier = Modifier.align(Alignment.Center).padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painterResource(destination.icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp),
            )
            Text(
                stringResource(destination.label),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = color,
            )
        }
    }
}

/** Minimum height of a bottom bar item, above the navigation bar inset. */
private val BarItemHeight = 64.dp

/** Opens a tab of the bottom bar and keeps the state of the others. */
internal fun NavController.openTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Route of the notification list (M-09); not a tab of the bottom bar. */
internal const val NotificationsRoute = "notifications"

/** Route of the library licenses, opened from *Settings → About* (16.2). */
internal const val LicensesRoute = "licenses"
