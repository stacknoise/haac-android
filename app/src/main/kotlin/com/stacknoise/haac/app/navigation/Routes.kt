package com.stacknoise.haac.app.navigation

import com.stacknoise.haac.app.lock.UnlockViewModel
import com.stacknoise.haac.app.start.StartRoute
import com.stacknoise.haac.feature.onboarding.ui.OnboardingViewModel

/** Root routes of the app. */
object Routes {
    /** Server selection and sign-in (concept 4); the optional argument signs in to a stored instance (4.1). */
    const val ONBOARDING = "onboarding?${OnboardingViewModel.SERVER_ID_ARG}={${OnboardingViewModel.SERVER_ID_ARG}}"

    /** Fingerprint unlock of a stored instance (concept 5.4). */
    const val UNLOCK = "unlock/{${UnlockViewModel.SERVER_ID_ARG}}"

    /** Main area with the bottom bar. */
    const val MAIN = "main"

    /** Onboarding for a new instance, or the login of the stored instance [serverId]. */
    fun onboarding(serverId: String? = null): String =
        if (serverId == null) "onboarding" else "onboarding?${OnboardingViewModel.SERVER_ID_ARG}=$serverId"

    /** Unlock screen of [serverId]. */
    fun unlock(serverId: String): String = "unlock/$serverId"

    /** The route that opens [start]. */
    fun of(start: StartRoute): String = when (start) {
        StartRoute.Onboarding -> onboarding()
        is StartRoute.SignIn -> onboarding(start.serverId)
        is StartRoute.Unlock -> unlock(start.serverId)
        is StartRoute.Main -> MAIN
    }
}
