package com.stacknoise.haac.core.common.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Design tokens of the light "Salbei" theme (design handoff "HAAC UI Redesign: Salbei"). */
object HaacColors {
    val Background = Color(0xFFF3F5F0)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFEDF1E9)
    val SegmentTrack = Color(0xFFE8EDE4)
    val Outline = Color(0xFFE2E7DD)
    val OutlineStrong = Color(0xFFCBD3C6)
    val OnSurface = Color(0xFF1D2620)
    val OnSurfaceVariant = Color(0xFF5A6A5F)
    val OnSurfaceVariantStrong = Color(0xFF4A5A4F)
    val Accent = Color(0xFF2F6B4F)
    val AccentTint = Color(0x1A2F6B4F)
    val AccentTintStrong = Color(0xFFE3EFE6)
    val AccentBorder = Color(0x772F6B4F)
    val SwitchTrackOff = Color(0xFFB9C3B3)
    val NavBackground = Color(0xFFFAFBF8)
    val NavBorder = Color(0xFFDDE3D8)
    val DisabledBackground = Color(0xFFE4E9DF)
    val DisabledText = Color(0xFF6B7A70)
    val Danger = Color(0xFF9B2C2C)
    val DangerContainer = Color(0xFFF8E7E4)
    val Scrim = Color(0x801D2620)
    val OnAccent = Color(0xFFFFFFFF)
    val OutlinedText = Color(0xFF2B3A31)
}

/** Material 3 light color scheme built from [HaacColors]; the dark variant is not designed yet. */
internal val HaacColorScheme = lightColorScheme(
    primary = HaacColors.Accent,
    onPrimary = HaacColors.OnAccent,
    primaryContainer = HaacColors.AccentTintStrong,
    onPrimaryContainer = HaacColors.Accent,
    secondaryContainer = HaacColors.AccentTintStrong,
    onSecondaryContainer = HaacColors.Accent,
    background = HaacColors.Background,
    onBackground = HaacColors.OnSurface,
    surface = HaacColors.Background,
    onSurface = HaacColors.OnSurface,
    surfaceVariant = HaacColors.SurfaceMuted,
    onSurfaceVariant = HaacColors.OnSurfaceVariant,
    surfaceTint = HaacColors.Accent,
    surfaceContainerLowest = HaacColors.Surface,
    surfaceContainerLow = HaacColors.Surface,
    surfaceContainer = HaacColors.Surface,
    surfaceContainerHigh = HaacColors.Surface,
    surfaceContainerHighest = HaacColors.SwitchTrackOff,
    surfaceDim = HaacColors.Background,
    surfaceBright = HaacColors.Surface,
    error = HaacColors.Danger,
    onError = HaacColors.OnAccent,
    errorContainer = HaacColors.DangerContainer,
    onErrorContainer = HaacColors.Danger,
    outline = HaacColors.OutlineStrong,
    outlineVariant = HaacColors.Outline,
    scrim = HaacColors.Scrim,
    inverseSurface = HaacColors.OnSurface,
    inverseOnSurface = HaacColors.Background,
)
