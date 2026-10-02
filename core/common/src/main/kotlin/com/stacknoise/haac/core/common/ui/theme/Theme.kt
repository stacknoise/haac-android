package com.stacknoise.haac.core.common.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * The app theme: the colors of [palette] (light "Salbei" by default), Figtree and the corner radii of the design
 * handoff.
 *
 * Text without an explicit colour uses `onBackground`, so screens not wrapped in a Surface still get the
 * theme's text colour instead of Compose's default black.
 */
@Composable
fun HaacTheme(palette: HaacPalette = LightHaacPalette, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = haacColorScheme(palette),
        typography = HaacTypography,
        shapes = HaacMaterialShapes,
    ) {
        CompositionLocalProvider(
            LocalHaacPalette provides palette,
            LocalContentColor provides palette.onSurface,
            content = content,
        )
    }
}
