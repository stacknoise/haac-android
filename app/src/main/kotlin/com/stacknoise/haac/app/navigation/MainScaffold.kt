package com.stacknoise.haac.app.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stacknoise.haac.R
import com.stacknoise.haac.app.connection.ConnectionBanner
import com.stacknoise.haac.app.connection.ConnectionViewModel
import com.stacknoise.haac.feature.settings.ui.SettingsScreen

/**
 * Main area after sign-in: bottom bar with Rooms, Places, Settings (concept 15.2) and the live connection of the
 * active instance (11.4). [onSignedOut] after logout or when HA no longer accepts the instance's token (14.1).
 */
@Composable
fun MainScaffold(onSignedOut: (String) -> Unit, connection: ConnectionViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val connectionState by connection.state.collectAsStateWithLifecycle()
    val signInRequired by connection.signInRequired.collectAsStateWithLifecycle()
    LifecycleStartEffect(connection) {
        connection.onForeground()
        onStopOrDispose { connection.onBackground() }
    }
    LaunchedEffect(signInRequired) { signInRequired?.let(onSignedOut) }
    Scaffold(
        topBar = {
            ConnectionBanner(
                state = connectionState,
                onRetry = connection::retry,
                onOpenSettings = { navController.openTopLevel(TopLevelDestination.SETTINGS) },
            )
        },
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = { navController.openTopLevel(destination) },
                        icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                        label = { Text(stringResource(destination.label)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.ROOMS.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(TopLevelDestination.ROOMS.route) { PlaceholderScreen(TopLevelDestination.ROOMS.label) }
            composable(TopLevelDestination.PLACES.route) { PlaceholderScreen(TopLevelDestination.PLACES.label) }
            composable(TopLevelDestination.SETTINGS.route) { SettingsScreen(onSignedOut = onSignedOut) }
        }
    }
}

/** Opens a tab of the bottom bar and keeps the state of the others. */
private fun NavController.openTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Temporary screen until the feature modules provide their own. */
@Composable
private fun PlaceholderScreen(@StringRes title: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(title) + " · " + stringResource(R.string.placeholder_coming_soon),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
