package com.stacknoise.haac.core.common.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/** The app theme: dark "Nocturne" colors, Inter and the corner radii of concept 15.2. */
@Composable
fun HaacTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HaacColorScheme,
        typography = HaacTypography,
        shapes = HaacMaterialShapes,
        content = content,
    )
}
