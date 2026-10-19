package com.stacknoise.haac.feature.entities.disable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.haacSwitchColors
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.Tile
import com.stacknoise.haac.feature.entities.domain.TileContent

/** Opacity of the content of a deactivated tile (concept 7.5, M-19). */
internal const val DisabledAlpha = 0.55f

/** The round lock badge in the corner of a deactivated tile (M-19). */
@Composable
internal fun LockBadge(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(BadgeSize)
            .background(HaacColors.Surface, CircleShape)
            .border(1.dp, HaacColors.OutlineStrong, CircleShape),
    ) {
        Icon(
            painterResource(R.drawable.ic_entities_lock),
            contentDescription = stringResource(R.string.tile_deactivated),
            tint = HaacColors.OnSurface,
            modifier = Modifier.size(LockSize),
        )
    }
}

/** The pill *Deactivated* with a small lock, on the tile (M-19) and in the sheet (M-21). */
@Composable
internal fun DeactivatedChip(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(HaacColors.AccentTint, RoundedCornerShape(ChipRadius))
            .border(1.dp, HaacColors.AccentBorder, RoundedCornerShape(ChipRadius))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_entities_lock),
            contentDescription = null,
            tint = HaacColors.OnSurfaceVariantStrong,
            modifier = Modifier.size(ChipIconSize),
        )
        Text(
            stringResource(R.string.tile_deactivated),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = HaacColors.OnSurfaceVariantStrong,
            maxLines = 1,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** *Deactivated* and, for a switch, its greyed-out switch in the state HA reports (concept 7.5, M-19). */
@Composable
internal fun DisabledRow(tile: Tile, on: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        DeactivatedChip()
        Spacer(Modifier.weight(1f))
        if (tile.content is TileContent.Switch) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Switch(checked = on, onCheckedChange = null, enabled = false, colors = haacSwitchColors())
            }
        }
    }
}

private val BadgeSize = 32.dp
private val LockSize = 16.dp
private val ChipIconSize = 14.dp
private val ChipRadius = 16.dp
