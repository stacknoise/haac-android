package com.stacknoise.haac.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stacknoise.haac.app.start.StartViewModel
import com.stacknoise.haac.feature.onboarding.ui.OnboardingScreen
import com.stacknoise.haac.feature.onboarding.ui.OnboardingViewModel

/** Root navigation; the start screen follows the start routing of concept 4.1. */
@Composable
fun HaacNavHost(start: StartViewModel = hiltViewModel()) {
    val route by start.route.collectAsStateWithLifecycle()
    // While the route is decided the window background (theme colour) is shown.
    val startRoute = route ?: return
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.of(startRoute)) {
        composable(
            Routes.ONBOARDING,
            arguments = listOf(
                navArgument(OnboardingViewModel.SERVER_ID_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            OnboardingScreen(onSignedIn = { navController.replaceAll(Routes.MAIN) })
        }
        composable(Routes.MAIN) {
            MainScaffold(onSignedOut = { serverId -> navController.replaceAll(Routes.onboarding(serverId)) })
        }
    }
}

/** Navigates to [route] and clears the back stack, so Back does not return to sign-in or signed-out screens. */
private fun NavController.replaceAll(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
