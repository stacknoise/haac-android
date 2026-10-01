package com.stacknoise.haac.core.common.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Switch colours of the Salbei theme: accent track when on, grey-green track when off, white knob, no outline. */
@Composable
fun haacSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = HaacColors.OnAccent,
    checkedTrackColor = HaacColors.Accent,
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = HaacColors.OnAccent,
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

/** Colours of the Grid | List control: dark selected segment on a pale track, no outline. */
@Composable
fun haacSegmentedColors(): SegmentedButtonColors = SegmentedButtonDefaults.colors(
    activeContainerColor = HaacColors.OnSurface,
    activeContentColor = HaacColors.Background,
    activeBorderColor = Color.Transparent,
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

/**
 * The 26 dp checkbox of the Salbei theme: accent square with a white tick when [checked], grey when checked but not
 * [enabled]. Toggles through [onCheckedChange]; null leaves toggling to a clickable parent.
 */
@Composable
fun HaacCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val fill = when {
        !checked -> HaacColors.Surface
        enabled -> HaacColors.Accent
        else -> HaacColors.OutlineStrong
    }
    val border = if (checked) fill else HaacColors.OutlineStrong
    val toggle = if (onCheckedChange == null) {
        Modifier
    } else {
        Modifier.toggleable(checked, enabled, Role.Checkbox, onValueChange = onCheckedChange)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(toggle)
            .size(CheckboxSize)
            .background(fill, HaacShapes.Checkbox)
            .border(1.5.dp, border, HaacShapes.Checkbox),
    ) {
        if (checked) {
            Canvas(Modifier.size(14.dp)) {
                val tick = Path().apply {
                    moveTo(size.width * 0.08f, size.height * 0.52f)
                    lineTo(size.width * 0.38f, size.height * 0.82f)
                    lineTo(size.width * 0.92f, size.height * 0.2f)
                }
                val stroke = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawPath(tick, HaacColors.OnAccent, style = stroke)
            }
        }
    }
}

/** Side of [HaacCheckbox]. */
private val CheckboxSize = 26.dp

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

/**
 * A list card: white surface with a 1 dp outline and 22 dp corners; [selected] uses the accent tint with a 1.5 dp
 * accent border instead.
 */
@Composable
fun HaacCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val frame = if (selected) {
        Modifier
            .background(HaacColors.AccentTint, HaacShapes.Card)
            .border(1.5.dp, HaacColors.AccentBorder, HaacShapes.Card)
    } else {
        Modifier
            .background(HaacColors.Surface, HaacShapes.Card)
            .border(1.dp, HaacColors.Outline, HaacShapes.Card)
    }
    Column(modifier = modifier.fillMaxWidth().then(frame).padding(contentPadding), content = content)
}
