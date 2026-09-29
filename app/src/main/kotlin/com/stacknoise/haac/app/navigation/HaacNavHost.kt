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
import com.stacknoise.haac.feature.instance.domain.SwitchStep
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
            OnboardingScreen(onSignedIn = navController::openMain)
        }
        composable(
            Routes.UNLOCK,
            arguments = listOf(navArgument(UnlockViewModel.SERVER_ID_ARG) { type = NavType.StringType }),
        ) { entry ->
            val serverId = entry.arguments?.getString(UnlockViewModel.SERVER_ID_ARG)
            UnlockScreen(
                onUnlocked = navController::openMain,
                onSignIn = {
                    navController.navigate(Routes.onboarding(serverId)) {
                        popUpTo(Routes.UNLOCK) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.MAIN) {
            MainScaffold(
                MainActions(
                    onSignedOut = { serverId -> navController.replaceAll(Routes.onboarding(serverId)) },
                    onSwitched = { serverId, step -> navController.onSwitched(serverId, step) },
                    onAddInstance = { navController.navigate(Routes.onboarding()) },
                ),
            )
        }
    }
}

/**
 * Continues after a switch (concept 4.4): the instance is active already ([SwitchStep.READY]), or its unlock or
 * login opens on top of the main area, so Back returns to the instance that was active before.
 */
private fun NavController.onSwitched(serverId: String, step: SwitchStep) {
    when (step) {
        SwitchStep.READY -> Unit
        SwitchStep.UNLOCK -> navigate(Routes.unlock(serverId))
        SwitchStep.SIGN_IN -> navigate(Routes.onboarding(serverId))
    }
}

/**
 * Shows the main area after an unlock or sign-in: returns to it when it lies below, as after a switch or *Add
 * instance* (its connection is not touched twice), otherwise replaces the whole stack.
 */
private fun NavController.openMain() {
    if (!popBackStack(Routes.MAIN, inclusive = false)) replaceAll(Routes.MAIN)
}

/** Navigates to [route] and clears the back stack, so Back does not return to sign-in or signed-out screens. */
private fun NavController.replaceAll(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
