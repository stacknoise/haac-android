package com.stacknoise.haac.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stacknoise.haac.app.lock.UnlockScreen
import com.stacknoise.haac.app.lock.UnlockViewModel
import com.stacknoise.haac.app.start.StartViewModel
import com.stacknoise.haac.feature.onboarding.ui.OnboardingScreen
import com.stacknoise.haac.feature.onboarding.ui.OnboardingViewModel

/**
 * Root navigation; the start screen follows the start routing of concept 4.1. After an app lock (5.5) the
 * routing runs again and replaces whatever screen was open.
 */
@Composable
fun HaacNavHost(start: StartViewModel = hiltViewModel()) {
    val decision by start.decision.collectAsStateWithLifecycle()
    // While the route is decided the window background (theme colour) is shown.
    val current = decision ?: return
    val navController = rememberNavController()
    val startDestination = remember { Routes.of(current.route) }
    LaunchedEffect(current) {
        if (current.lock > 0) navController.replaceAll(Routes.of(current.route))
    }
    NavHost(navController = navController, startDestination = startDestination) {
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
        composable(
            Routes.UNLOCK,
            arguments = listOf(navArgument(UnlockViewModel.SERVER_ID_ARG) { type = NavType.StringType }),
        ) { entry ->
            val serverId = entry.arguments?.getString(UnlockViewModel.SERVER_ID_ARG)
            UnlockScreen(
                onUnlocked = { navController.replaceAll(Routes.MAIN) },
                onSignIn = { navController.replaceAll(Routes.onboarding(serverId)) },
            )
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
