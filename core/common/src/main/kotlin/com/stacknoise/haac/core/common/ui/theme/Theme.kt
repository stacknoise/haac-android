package com.stacknoise.haac.core.common.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * The app theme: dark "Nocturne" colors, Inter and the corner radii of concept 15.2.
 *
 * Text without an explicit colour uses `onBackground`; without this, screens not wrapped in a
 * Surface would draw it in Compose's default black, which is unreadable on the dark background.
 */
@Composable
fun HaacTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HaacColorScheme,
        typography = HaacTypography,
        shapes = HaacMaterialShapes,
    ) {
        CompositionLocalProvider(LocalContentColor provides HaacColors.OnBackground, content = content)
    }
}
