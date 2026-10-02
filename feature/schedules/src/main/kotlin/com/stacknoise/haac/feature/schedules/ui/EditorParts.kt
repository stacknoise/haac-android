package com.stacknoise.haac.feature.schedules.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.common.ui.theme.haacFilterChipBorder
import com.stacknoise.haac.core.common.ui.theme.haacFilterChipColors
import com.stacknoise.haac.core.common.ui.theme.haacSegmentedColors
import com.stacknoise.haac.feature.schedules.R
import com.stacknoise.haac.feature.schedules.domain.ScheduleAction
import com.stacknoise.haac.feature.schedules.domain.ScheduleDraft
import com.stacknoise.haac.feature.schedules.domain.WeekDays
import java.time.format.TextStyle

/** A section label of the editor, with the space above it. */
@Composable
internal fun EditorLabel(text: Int) {
    Text(stringResource(text), style = SectionLabelStyle, modifier = Modifier.padding(top = 12.dp))
}

/** Seven round weekday buttons M T W T F S S (design 3b); selected ones are filled with the accent. */
@Composable
internal fun WeekdayButtons(days: Set<Int>, onDay: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        for (day in 0 until WeekDays.COUNT) {
            val on = day in days
            val name = dayName(day, TextStyle.FULL)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (on) HaacColors.Accent else HaacColors.Surface, CircleShape)
                    .border(1.dp, if (on) HaacColors.Accent else HaacColors.OutlineStrong, CircleShape)
                    .clickable(role = Role.Checkbox) { onDay(day) }
                    .semantics { contentDescription = name; selected = on },
            ) {
                Text(
                    dayName(day, TextStyle.NARROW),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    color = if (on) HaacColors.OnAccent else HaacColors.OnSurface,
                )
            }
        }
    }
}

/** *Weekdays*, *Weekend* and *Every day*; the one that matches the selection is the dark chip. */
@Composable
internal fun PresetChips(days: Set<Int>, onDays: (Set<Int>) -> Unit) {
    val presets = listOf(
        R.string.schedules_preset_weekdays to ScheduleDraft.WEEKDAYS,
        R.string.schedules_preset_weekend to ScheduleDraft.WEEKEND,
        R.string.schedules_preset_every_day to ScheduleDraft.EVERY_DAY,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { (label, set) ->
            FilterChip(
                selected = days == set,
                onClick = { onDays(set) },
                label = { Text(stringResource(label)) },
                colors = haacFilterChipColors(),
                border = haacFilterChipBorder(enabled = true, selected = days == set),
                modifier = Modifier.height(40.dp),
            )
        }
    }
}

/** The live summary "Every weekday at 06:45 · Lamp turns on" (design 3b). */
@Composable
internal fun SummaryCard(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(HaacColors.AccentTint, HaacShapes.Small)
            .padding(14.dp),
    ) {
        GlyphIcon(R.drawable.ic_schedules_clock, HaacColors.Accent, 20.dp)
        Text(
            text,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = HaacColors.Accent,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

/** *Turn on / Turn off / Toggle* as a segmented control. */
@Composable
internal fun ActionSegments(action: ScheduleAction, onAction: (ScheduleAction) -> Unit) {
    val labels = mapOf(
        ScheduleAction.TURN_ON to R.string.schedules_action_on,
        ScheduleAction.TURN_OFF to R.string.schedules_action_off,
        ScheduleAction.TOGGLE to R.string.schedules_action_toggle,
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().height(44.dp)) {
        ScheduleAction.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                selected = entry == action,
                onClick = { onAction(entry) },
                shape = SegmentedButtonDefaults.itemShape(index, ScheduleAction.entries.size, HaacShapes.Small),
                colors = haacSegmentedColors(),
            ) { Text(stringResource(labels.getValue(entry))) }
        }
    }
}

/** The 84 dp time card: icon, the time in 40 sp and *Change*, which opens the *Run at* sheet. */
@Composable
internal fun TimeCard(time: String, subtitle: String?, onChange: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(HaacColors.Surface, HaacShapes.Card)
            .border(1.dp, HaacColors.Outline, HaacShapes.Card)
            .clickable(onClick = onChange)
            .padding(horizontal = 16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(48.dp).background(HaacColors.AccentTint, HaacShapes.Medium),
        ) {
            GlyphIcon(R.drawable.ic_schedules_clock, HaacColors.Accent, 24.dp)
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                time,
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp, letterSpacing = (-0.025).em),
                maxLines = 1,
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = HaacColors.OnSurfaceVariant)
            }
        }
        TextButton(onClick = onChange) {
            Text(stringResource(R.string.schedules_change), color = HaacColors.Accent, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * One chosen entity (68 dp): the name and, for a schedule of another user, a lock instead of the remove button
 * ([locked], grey card, design 3b A3).
 */
@Composable
internal fun EntityRow(name: String, locked: Boolean, onRemove: () -> Unit) {
    val background = if (locked) MutedCard else HaacColors.Surface
    val iconBox = if (locked) MutedIconBox else HaacColors.Accent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .background(background, HaacShapes.Card)
            .border(1.dp, if (locked) MutedCardBorder else HaacColors.Outline, HaacShapes.Card)
            .padding(horizontal = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp).background(iconBox, HaacShapes.Medium),
        ) {
            GlyphIcon(R.drawable.ic_schedules_clock, HaacColors.OnAccent, 22.dp)
        }
        Text(
            name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
        )
        if (locked) {
            GlyphIcon(R.drawable.ic_schedules_lock, HaacColors.OnSurfaceVariant, 20.dp)
        } else {
            IconButton(onClick = onRemove) {
                Icon(
                    painterResource(R.drawable.ic_schedules_close),
                    stringResource(R.string.schedules_remove_entity, name),
                    tint = HaacColors.OnSurfaceVariant,
                )
            }
        }
    }
}

/** Grey card colours of a locked entity and of the banner (design 3b A3). */
private val MutedCard = Color(0xFFEAEEE6)
private val MutedCardBorder = Color(0xFFDDE3D8)
private val MutedIconBox = Color(0xFFCBD3C6)

/** The dashed *Add entities* tile that opens the picker. */
@Composable
internal fun AddEntitiesTile(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(HaacShapes.Card)
            .clickable(onClick = onClick)
            .dashedBorder(HaacColors.AccentBorder),
    ) {
        GlyphIcon(R.drawable.ic_schedules_add, HaacColors.Accent, 22.dp)
        Text(
            stringResource(R.string.schedules_add_entities),
            style = MaterialTheme.typography.titleSmall,
            color = HaacColors.Accent,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** A 1.5 dp dashed outline in [color] with card corners. */
private fun Modifier.dashedBorder(color: Color): Modifier = drawBehind {
    val stroke = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(Dash, Dash)))
    drawRoundRect(color, cornerRadius = CornerRadius(CardRadius.dp.toPx()), style = stroke)
}

/** Length of a dash and of a gap, in pixels, and the card corner radius in dp. */
private const val Dash = 18f
private const val CardRadius = 22

/** The banner of an edited foreign schedule: who owns it and what may be changed (M-18). */
@Composable
internal fun OwnerBanner(owner: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().background(MutedCard, HaacShapes.Small).padding(14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp).background(HaacColors.Surface, HaacShapes.Small),
        ) {
            GlyphIcon(R.drawable.ic_schedules_person, HaacColors.Accent, 20.dp)
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(
                stringResource(R.string.schedules_owned_by, owner),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                stringResource(R.string.schedules_owned_note),
                style = MaterialTheme.typography.bodyMedium,
                color = HaacColors.OnSurfaceVariant,
            )
        }
    }
}
