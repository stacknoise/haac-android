package com.stacknoise.haac.core.common.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Switch colours of the Salbei theme: accent track when on, grey-green track when off, white knob, no outline. */
@Composable
fun haacSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = HaacColors.OnAccent,
    checkedTrackColor = HaacColors.Accent,
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = HaacColors.SwitchThumbOff,
    uncheckedTrackColor = HaacColors.SwitchTrackOff,
    uncheckedBorderColor = Color.Transparent,
)

/** Colours of primary buttons: accent with white text, soft green-grey when disabled. */
@Composable
fun haacButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = HaacColors.Accent,
    contentColor = HaacColors.OnAccent,
    disabledContainerColor = HaacColors.DisabledBackground,
    disabledContentColor = HaacColors.DisabledText,
)

/** Colours of segmented controls: [selected] segment (accent green by default) on a pale track, no outline. */
@Composable
fun haacSegmentedColors(
    selected: Color = HaacColors.Accent,
    onSelected: Color = HaacColors.OnAccent,
): SegmentedButtonColors = SegmentedButtonDefaults.colors(
    activeContainerColor = selected,
    activeBorderColor = Color.Transparent,
    activeContentColor = onSelected,
    inactiveContainerColor = HaacColors.SegmentTrack,
    inactiveContentColor = HaacColors.OnSurfaceVariantStrong,
    inactiveBorderColor = Color.Transparent,
)

/** Colours of text fields: white field, pale outline, accent outline on focus, muted leading icon. */
@Composable
fun haacTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = HaacColors.Surface,
    unfocusedContainerColor = HaacColors.Surface,
    disabledContainerColor = HaacColors.Surface,
    focusedBorderColor = HaacColors.Accent,
    unfocusedBorderColor = HaacColors.Outline,
    cursorColor = HaacColors.Accent,
    focusedLeadingIconColor = HaacColors.OnSurfaceVariant,
    unfocusedLeadingIconColor = HaacColors.OnSurfaceVariant,
)

/** Colours of outlined buttons: dark-green text on the transparent background. */
@Composable
fun haacOutlinedButtonColors(): ButtonColors =
    ButtonDefaults.outlinedButtonColors(contentColor = HaacColors.OutlinedText)

/** Colours of filter chips: outlined when unselected, filled dark-green with pale text when selected. */
@Composable
fun haacFilterChipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    containerColor = Color.Transparent,
    labelColor = HaacColors.OnSurfaceVariantStrong,
    iconColor = HaacColors.OnSurfaceVariantStrong,
    selectedContainerColor = HaacColors.OnSurface,
    selectedLabelColor = HaacColors.Background,
    selectedLeadingIconColor = HaacColors.Background,
)

/** Border of a filter chip: 1 dp strong outline, none when [selected] (the dark fill carries it). */
@Composable
fun haacFilterChipBorder(enabled: Boolean, selected: Boolean): BorderStroke =
    FilterChipDefaults.filterChipBorder(
        enabled = enabled,
        selected = selected,
        borderColor = HaacColors.OutlineStrong,
        selectedBorderColor = HaacColors.OnSurface,
    )

/** Colours of chips that stay outlined when selected: the accent green marks the choice, the fill stays clear. */
@Composable
fun haacOutlinedChipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    containerColor = Color.Transparent,
    labelColor = HaacColors.OnSurfaceVariantStrong,
    selectedContainerColor = Color.Transparent,
    selectedLabelColor = HaacColors.Accent,
)

/** Border for [haacOutlinedChipColors]: 1.5 dp accent green when [selected], the strong outline otherwise. */
@Composable
fun haacOutlinedChipBorder(enabled: Boolean, selected: Boolean): BorderStroke =
    FilterChipDefaults.filterChipBorder(
        enabled = enabled,
        selected = selected,
        borderColor = HaacColors.OutlineStrong,
        selectedBorderColor = HaacColors.Accent,
        selectedBorderWidth = 1.5.dp,
    )
