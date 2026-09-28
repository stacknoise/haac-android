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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stacknoise.haac.R
import com.stacknoise.haac.feature.settings.ui.SettingsScreen

/** Main area after sign-in: bottom bar with Rooms, Places, Settings (concept 15.2); [onSignedOut] after logout. */
@Composable
fun MainScaffold(onSignedOut: (String) -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
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
