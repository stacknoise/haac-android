package com.stacknoise.haac.core.common.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * The app theme: light "Salbei" colors, Figtree and the corner radii of the design handoff.
 *
 * Text without an explicit colour uses `onBackground`, so screens not wrapped in a Surface still get the
 * dark-green text colour instead of Compose's default black.
 */
@Composable
fun HaacTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HaacColorScheme,
        typography = HaacTypography,
        shapes = HaacMaterialShapes,
    ) {
        CompositionLocalProvider(LocalContentColor provides HaacColors.OnSurface, content = content)
    }
}
