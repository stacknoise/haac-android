package com.stacknoise.haac.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stacknoise.haac.feature.onboarding.ui.OnboardingScreen

/**
 * Root navigation (concept 4.1). Until instances are stored (login, next step) the app always
 * starts with onboarding; start routing by stored instance follows then.
 */
@Composable
fun HaacNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.ONBOARDING) {
        composable(Routes.ONBOARDING) { OnboardingScreen() }
        composable(Routes.MAIN) { MainScaffold() }
    }
}
