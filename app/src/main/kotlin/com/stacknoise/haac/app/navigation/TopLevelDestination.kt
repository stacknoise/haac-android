package com.stacknoise.haac.app.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stacknoise.haac.R

/** Top-level destinations of the bottom bar (concept 15.2). */
enum class TopLevelDestination(
    val route: String,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
) {
    ROOMS("rooms", R.string.nav_rooms, R.drawable.ic_nav_rooms),
    PLACES("places", R.string.nav_places, R.drawable.ic_nav_places),
    SETTINGS("settings", R.string.nav_settings, R.drawable.ic_nav_settings),
}
