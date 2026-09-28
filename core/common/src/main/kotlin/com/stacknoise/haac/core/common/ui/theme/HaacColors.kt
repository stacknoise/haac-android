package com.stacknoise.haac.core.common.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/** Design tokens of the "Nocturne" theme, read from mockup set 1c (concept 15.2). */
object HaacColors {
    val Background = Color(0xFF161826)
    val Surface = Color(0xFF232532)
    val SurfaceVariant = Color(0xFF3F424D)
    val Outline = Color(0xFF75798C)
    val Primary = Color(0xFF4DFF7A)
    val PrimaryContainer = Color(0xFF0D2F18)
    val OnPrimaryContainer = Color(0xFFA6FFBB)
    val OnBackground = Color(0xFFE9E9ED)
    val OnSurfaceVariant = Color(0xFF9397AB)
}

/** Material 3 dark color scheme built from [HaacColors]; v1 has no light theme. */
internal val HaacColorScheme = darkColorScheme(
    primary = HaacColors.Primary,
    onPrimary = HaacColors.Background,
    primaryContainer = HaacColors.PrimaryContainer,
    onPrimaryContainer = HaacColors.OnPrimaryContainer,
    background = HaacColors.Background,
    onBackground = HaacColors.OnBackground,
    surface = HaacColors.Background,
    onSurface = HaacColors.OnBackground,
    surfaceVariant = HaacColors.SurfaceVariant,
    onSurfaceVariant = HaacColors.OnSurfaceVariant,
    surfaceContainerLowest = HaacColors.Background,
    surfaceContainerLow = HaacColors.Surface,
    surfaceContainer = HaacColors.Surface,
    surfaceContainerHigh = HaacColors.Surface,
    surfaceContainerHighest = HaacColors.SurfaceVariant,
    outline = HaacColors.Outline,
    outlineVariant = HaacColors.SurfaceVariant,
)
