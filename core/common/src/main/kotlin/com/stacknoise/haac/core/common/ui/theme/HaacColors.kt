package com.stacknoise.haac.core.common.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** One set of design tokens (design handoff "HAAC UI Redesign: Salbei"); [LightHaacPalette] is the shipped one. */
@Immutable
data class HaacPalette(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val segmentTrack: Color,
    val outline: Color,
    val outlineStrong: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val onSurfaceVariantStrong: Color,
    val accent: Color,
    val accentTint: Color,
    val accentTintStrong: Color,
    val accentBorder: Color,
    val switchTrackOff: Color,
    val navBackground: Color,
    val navBorder: Color,
    val disabledBackground: Color,
    val disabledText: Color,
    val danger: Color,
    val dangerContainer: Color,
    val scrim: Color,
    val onAccent: Color,
    val outlinedText: Color,
)

/** The tokens of the light "Salbei" theme. */
val LightHaacPalette = HaacPalette(
    background = Color(0xFFF3F5F0),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFEDF1E9),
    segmentTrack = Color(0xFFE8EDE4),
    outline = Color(0xFFE2E7DD),
    outlineStrong = Color(0xFFCBD3C6),
    onSurface = Color(0xFF1D2620),
    onSurfaceVariant = Color(0xFF5A6A5F),
    onSurfaceVariantStrong = Color(0xFF4A5A4F),
    accent = Color(0xFF2F6B4F),
    accentTint = Color(0x1A2F6B4F),
    accentTintStrong = Color(0xFFE3EFE6),
    accentBorder = Color(0x772F6B4F),
    switchTrackOff = Color(0xFFB9C3B3),
    navBackground = Color(0xFFFAFBF8),
    navBorder = Color(0xFFDDE3D8),
    disabledBackground = Color(0xFFE4E9DF),
    disabledText = Color(0xFF6B7A70),
    danger = Color(0xFF9B2C2C),
    dangerContainer = Color(0xFFF8E7E4),
    scrim = Color(0x801D2620),
    onAccent = Color(0xFFFFFFFF),
    outlinedText = Color(0xFF2B3A31),
)

/** The palette of the current theme; [HaacTheme] provides it. */
internal val LocalHaacPalette = staticCompositionLocalOf { LightHaacPalette }

/** The tokens of the current theme, read where a screen draws (composable context only). */
object HaacColors {
    val Background: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.background
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.surface
    val SurfaceMuted: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.surfaceMuted
    val SegmentTrack: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.segmentTrack
    val Outline: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.outline
    val OutlineStrong: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.outlineStrong
    val OnSurface: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.onSurface
    val OnSurfaceVariant: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.onSurfaceVariant
    val OnSurfaceVariantStrong: Color
        @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.onSurfaceVariantStrong
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.accent
    val AccentTint: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.accentTint
    val AccentTintStrong: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.accentTintStrong
    val AccentBorder: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.accentBorder
    val SwitchTrackOff: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.switchTrackOff
    val NavBackground: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.navBackground
    val NavBorder: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.navBorder
    val DisabledBackground: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.disabledBackground
    val DisabledText: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.disabledText
    val Danger: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.danger
    val DangerContainer: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.dangerContainer
    val Scrim: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.scrim
    val OnAccent: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.onAccent
    val OutlinedText: Color @Composable @ReadOnlyComposable get() = LocalHaacPalette.current.outlinedText
}

/** The Material 3 color scheme built from [palette]. */
internal fun haacColorScheme(palette: HaacPalette): ColorScheme = lightColorScheme(
    primary = palette.accent,
    onPrimary = palette.onAccent,
    primaryContainer = palette.accentTintStrong,
    onPrimaryContainer = palette.accent,
    secondaryContainer = palette.accentTintStrong,
    onSecondaryContainer = palette.accent,
    background = palette.background,
    onBackground = palette.onSurface,
    surface = palette.background,
    onSurface = palette.onSurface,
    surfaceVariant = palette.surfaceMuted,
    onSurfaceVariant = palette.onSurfaceVariant,
    surfaceTint = palette.accent,
    surfaceContainerLowest = palette.surface,
    surfaceContainerLow = palette.surface,
    surfaceContainer = palette.surface,
    surfaceContainerHigh = palette.surface,
    surfaceContainerHighest = palette.switchTrackOff,
    surfaceDim = palette.background,
    surfaceBright = palette.surface,
    error = palette.danger,
    onError = palette.onAccent,
    errorContainer = palette.dangerContainer,
    onErrorContainer = palette.danger,
    outline = palette.outlineStrong,
    outlineVariant = palette.outline,
    scrim = palette.scrim,
    inverseSurface = palette.onSurface,
    inverseOnSurface = palette.background,
)
