package com.stacknoise.haac.feature.schedules.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.common.ui.theme.haacSwitchColors
import com.stacknoise.haac.feature.schedules.R
import com.stacknoise.haac.feature.schedules.data.ScheduleView
import com.stacknoise.haac.feature.schedules.domain.WeekDays
import java.time.format.TextStyle

/** Colour of muted cards (off or paused schedules) and their border (design 3b). */
private val MutedBackground = Color(0xFFEAEEE6)
private val MutedBorder = Color(0xFFDDE3D8)

/**
 * The card of one schedule (design 3b): time and weekdays on the left, which open the detail screen, name, target
 * and, for admins, the owner in the middle, and the *enabled* switch. Off or paused schedules are muted; the switch
 * works only while [editable].
 */
@Composable
internal fun ScheduleCard(
    view: ScheduleView,
    editable: Boolean,
    showOwner: Boolean,
    onOpen: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = view.item
    val muted = item.muted
    val text = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(if (muted) MutedBackground else HaacColors.Surface, HaacShapes.Card)
            .border(1.dp, if (muted) MutedBorder else HaacColors.Outline, HaacShapes.Card)
            .padding(end = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clip(HaacShapes.Card)
                .clickable(onClick = onOpen)
                .padding(start = 16.dp, top = 14.dp, bottom = 14.dp),
        ) {
            Column(Modifier.width(TimeBlockWidth)) {
                Text(
                    timeText(item),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 27.sp, letterSpacing = (-0.02).em),
                    fontWeight = FontWeight.Bold,
                    color = text,
                    maxLines = 1,
                )
                Text(
                    shortDays(item.days),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    targetText(view),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showOwner && !item.own) OwnerLine(item.ownerName)
                if (item.paused) PausedBadge(Modifier.padding(top = 6.dp))
            }
        }
        Switch(
            checked = item.enabled,
            onCheckedChange = onToggle,
            enabled = editable,
            colors = haacSwitchColors(),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Width of the left block of a card that holds the time and the weekdays. */
private val TimeBlockWidth = 78.dp

/** The owner of a foreign schedule below its target, for admins (design 3b, A1). */
@Composable
private fun OwnerLine(name: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Icon(
            painterResource(R.drawable.ic_schedules_person),
            contentDescription = null,
            tint = HaacColors.OnSurfaceVariantStrong,
            modifier = Modifier.size(14.dp),
        )
        Text(
            name.orEmpty(),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
            color = HaacColors.OnSurfaceVariantStrong,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/** *Paused*: the system paused this schedule (concept 19.6). */
@Composable
internal fun PausedBadge(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.schedules_paused),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = HaacColors.OnSurfaceVariantStrong,
        modifier = modifier
            .background(HaacColors.DisabledBackground, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** The banner of the schedule that runs next: accent icon, the label NEXT UP and "Name · Fri 06:45" (design 3b). */
@Composable
internal fun NextUpBanner(text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(HaacColors.AccentTint, HaacShapes.Card)
            .border(1.5.dp, HaacColors.AccentBorder, HaacShapes.Card)
            .padding(14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp).background(HaacColors.Accent, HaacShapes.Medium),
        ) {
            Icon(
                painterResource(R.drawable.ic_schedules_clock),
                contentDescription = null,
                tint = HaacColors.OnAccent,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(stringResource(R.string.schedules_next_up), style = SectionLabelStyle, color = HaacColors.Accent)
            Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
        }
    }
}

/** Seven round weekday marks M T W T F S S: [days] are filled with the accent (design 3b, detail screen). */
@Composable
internal fun WeekdayDots(days: List<Int>, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = modifier.fillMaxWidth()) {
        for (day in 0 until WeekDays.COUNT) {
            val on = day in days
            val name = dayName(day, TextStyle.FULL)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(if (on) HaacColors.Accent else HaacColors.SurfaceMuted, CircleShape)
                    .semantics { contentDescription = name },
            ) {
                Text(
                    dayName(day, TextStyle.NARROW),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (on) HaacColors.OnAccent else HaacColors.OnSurfaceVariant,
                )
            }
        }
    }
}


/** A decorative icon of [size] in [tint]; it has no description because its text is next to it. */
@Composable
internal fun GlyphIcon(@DrawableRes icon: Int, tint: Color, size: Dp) {
    Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(size))
}
